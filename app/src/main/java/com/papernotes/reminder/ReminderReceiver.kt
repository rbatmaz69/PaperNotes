package com.papernotes.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.papernotes.MainActivity
import com.papernotes.R
import com.papernotes.data.repository.NoteRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Empfängt fällige Alarme:
 * - [ACTION_REMIND]: postet eine Erinnerungs-Notification; ist die Erinnerung wiederkehrend,
 *   wird die nächste Fälligkeit gespeichert und neu eingeplant.
 * - [ACTION_CAPSULE]: öffnet eine Zeitkapsel (entsiegelt die Notiz) und meldet das.
 * Ein Tap öffnet die zugehörige Notiz im Editor ([MainActivity] liest [EXTRA_NOTE_ID]).
 */
@AndroidEntryPoint
class ReminderReceiver : BroadcastReceiver() {

    @Inject lateinit var repository: NoteRepository

    @Inject lateinit var scheduler: ReminderScheduler

    override fun onReceive(context: Context, intent: Intent) {
        val noteId = intent.getLongExtra(EXTRA_NOTE_ID, 0L)
        if (noteId == 0L) return

        when (intent.action) {
            ACTION_REMIND -> {
                val title = intent.getStringExtra(EXTRA_NOTE_TITLE)?.takeIf { it.isNotBlank() }
                    ?: "Notiz"
                notify(context, noteId, title, "Deine Notiz wartet auf dich.", withSnooze = true)
                rescheduleIfRecurring(noteId)
            }
            ACTION_CAPSULE -> openCapsule(context, noteId)
            ACTION_SNOOZE -> {
                val title = intent.getStringExtra(EXTRA_NOTE_TITLE)?.takeIf { it.isNotBlank() }
                    ?: "Notiz"
                snooze(context, noteId, title, intent.getIntExtra(EXTRA_SNOOZE_MINUTES, 10))
            }
            ACTION_SNOOZE_FIRE -> {
                val title = intent.getStringExtra(EXTRA_NOTE_TITLE)?.takeIf { it.isNotBlank() }
                    ?: "Notiz"
                notify(context, noteId, title, "Deine Notiz wartet auf dich.", withSnooze = true)
            }
        }
    }

    /**
     * „+10 Min / +1 Std" aus der Notification: räumt sie weg und meldet sich später erneut.
     * Einmalige Erinnerungen werden regulär verschoben ([NoteRepository.setReminder] hält
     * Flattern & [BootReceiver] konsistent); bei wiederkehrenden bleibt die Serie unberührt
     * (sie wurde beim Feuern bereits vorgerückt) – dort feuert ein einmaliger Zusatz-Alarm.
     */
    private fun snooze(context: Context, noteId: Long, title: String, minutes: Int) {
        NotificationManagerCompat.from(context).cancel(noteId.toInt())
        val at = System.currentTimeMillis() + minutes * 60_000L
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val note = repository.getNote(noteId) ?: return@launch
                if (note.isRecurring) {
                    scheduler.scheduleSnooze(noteId, title, at)
                } else {
                    repository.setReminder(noteId, at)
                    scheduler.schedule(noteId, note.title, at)
                }
            } finally {
                pending.finish()
            }
        }
    }

    /** Wiederkehrende Erinnerung: nächste Fälligkeit persistieren + neuen Alarm setzen. */
    private fun rescheduleIfRecurring(noteId: Long) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val note = repository.getNote(noteId) ?: return@launch
                if (!note.isRecurring) return@launch
                val base = note.reminderAt ?: return@launch
                var next = note.reminderRule.next(base)
                val now = System.currentTimeMillis()
                while (next <= now) next = note.reminderRule.next(next) // verpasste Termine überspringen
                repository.setReminder(noteId, next)
                scheduler.schedule(noteId, note.title, next, note.reminderRule)
            } finally {
                pending.finish()
            }
        }
    }

    /** Zeitkapsel-Termin erreicht: Siegel lösen + benachrichtigen. */
    private fun openCapsule(context: Context, noteId: Long) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val note = repository.getNote(noteId) ?: return@launch
                repository.save(note.copy(sealed = false, capsuleAt = null))
                val title = note.title.takeIf { it.isNotBlank() } ?: "Zeitkapsel"
                notify(context, noteId, title, "Ein Brief hat sich geöffnet.")
            } finally {
                pending.finish()
            }
        }
    }

    private fun notify(
        context: Context,
        noteId: Long,
        title: String,
        text: String,
        withSnooze: Boolean = false,
    ) {
        ensureChannel(context)
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_NOTE_ID, noteId)
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            noteId.toInt(),
            openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_reminder)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
        if (withSnooze) {
            builder.addAction(snoozeAction(context, noteId, title, minutes = 10, label = "+10 Min"))
            builder.addAction(snoozeAction(context, noteId, title, minutes = 60, label = "+1 Std"))
        }
        // notify() ist ohne POST_NOTIFICATIONS-Recht ein No-op – kein Crash.
        NotificationManagerCompat.from(context).notify(noteId.toInt(), builder.build())
    }

    private fun snoozeAction(
        context: Context,
        noteId: Long,
        title: String,
        minutes: Int,
        label: String,
    ): NotificationCompat.Action {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_SNOOZE
            // Eindeutige Data-URI: unterscheidet die beiden Snooze-PendingIntents derselben
            // Notiz (filterEquals berücksichtigt Action + Data, Extras nicht).
            data = android.net.Uri.parse("papernotes://snooze/$noteId/$minutes")
            putExtra(EXTRA_NOTE_ID, noteId)
            putExtra(EXTRA_NOTE_TITLE, title)
            putExtra(EXTRA_SNOOZE_MINUTES, minutes)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            noteId.toInt(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Action(0, label, pendingIntent)
    }

    private fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Erinnerungen",
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply { description = "Erinnert dich an deine Notizen." },
            )
        }
    }

    companion object {
        const val ACTION_REMIND = "com.papernotes.reminder.ACTION_REMIND"
        const val ACTION_CAPSULE = "com.papernotes.reminder.ACTION_CAPSULE"
        const val ACTION_SNOOZE = "com.papernotes.reminder.ACTION_SNOOZE"
        const val ACTION_SNOOZE_FIRE = "com.papernotes.reminder.ACTION_SNOOZE_FIRE"
        const val EXTRA_NOTE_ID = "note_id"
        const val EXTRA_NOTE_TITLE = "note_title"
        const val EXTRA_RULE = "note_rule"
        const val EXTRA_SNOOZE_MINUTES = "snooze_minutes"
        private const val CHANNEL_ID = "note_reminders"
    }
}
