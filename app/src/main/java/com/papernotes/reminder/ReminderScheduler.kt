package com.papernotes.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.papernotes.domain.model.ReminderRule
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Kapselt das Planen/Abbrechen exakter Erinnerungs-Alarme über den [AlarmManager].
 * Jede Notiz hat genau einen Alarm (requestCode = Notiz-id), sodass erneutes Setzen
 * den vorherigen Alarm überschreibt. Feuert via [ReminderReceiver].
 */
@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val alarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedule(
        noteId: Long,
        title: String,
        triggerAtMillis: Long,
        rule: ReminderRule = ReminderRule.NONE,
    ) {
        setAlarm(triggerAtMillis, reminderIntent(noteId, title, rule, mutableUpdate = true))
    }

    fun cancel(noteId: Long) {
        alarmManager.cancel(reminderIntent(noteId, "", ReminderRule.NONE, mutableUpdate = false))
    }

    /** Plant das Selbst-Öffnen einer Zeitkapsel zum [triggerAtMillis]. */
    fun scheduleCapsule(noteId: Long, triggerAtMillis: Long) {
        setAlarm(triggerAtMillis, capsuleIntent(noteId, mutableUpdate = true))
    }

    fun cancelCapsule(noteId: Long) {
        alarmManager.cancel(capsuleIntent(noteId, mutableUpdate = false))
    }

    /**
     * Einmaliger Zusatz-Alarm fürs Snoozen einer wiederkehrenden Erinnerung: eigene Action,
     * damit der bereits auf den nächsten Termin vorgerückte Serien-Alarm unberührt bleibt.
     * Bewusst nicht persistiert – überlebt keinen Reboot (akzeptierter Trade-off).
     */
    fun scheduleSnooze(noteId: Long, title: String, triggerAtMillis: Long) {
        setAlarm(triggerAtMillis, snoozeIntent(noteId, title, mutableUpdate = true))
    }

    fun cancelSnooze(noteId: Long) {
        alarmManager.cancel(snoozeIntent(noteId, "", mutableUpdate = false))
    }

    /** Räumt eine bereits angezeigte Erinnerungs-Notification weg (Quittieren beim Öffnen). */
    fun dismissNotification(noteId: Long) {
        NotificationManagerCompat.from(context).cancel(noteId.toInt())
    }

    /**
     * Exakter Alarm, wenn der Nutzer SCHEDULE_EXACT_ALARM (noch) gewährt – sonst ungenauer
     * Alarm als Fallback (feuert im Doze-Fenster, typischerweise wenige Minuten später).
     * Wichtig: nicht stillschweigend verwerfen, die Erinnerung soll in jedem Fall kommen.
     */
    private fun setAlarm(triggerAtMillis: Long, operation: PendingIntent) {
        if (alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, operation)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, operation)
        }
    }

    private fun reminderIntent(
        noteId: Long,
        title: String,
        rule: ReminderRule,
        mutableUpdate: Boolean,
    ): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_REMIND
            putExtra(ReminderReceiver.EXTRA_NOTE_ID, noteId)
            putExtra(ReminderReceiver.EXTRA_NOTE_TITLE, title)
            putExtra(ReminderReceiver.EXTRA_RULE, rule.name)
        }
        return broadcast(noteId, intent, mutableUpdate)
    }

    // Eigene Action ⇒ unterscheidet sich für PendingIntent-Matching vom Erinnerungs-Alarm
    // derselben Notiz (filterEquals berücksichtigt die Action), daher gleicher requestCode ok.
    private fun capsuleIntent(noteId: Long, mutableUpdate: Boolean): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_CAPSULE
            putExtra(ReminderReceiver.EXTRA_NOTE_ID, noteId)
        }
        return broadcast(noteId, intent, mutableUpdate)
    }

    private fun snoozeIntent(noteId: Long, title: String, mutableUpdate: Boolean): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_SNOOZE_FIRE
            putExtra(ReminderReceiver.EXTRA_NOTE_ID, noteId)
            putExtra(ReminderReceiver.EXTRA_NOTE_TITLE, title)
        }
        return broadcast(noteId, intent, mutableUpdate)
    }

    private fun broadcast(noteId: Long, intent: Intent, mutableUpdate: Boolean): PendingIntent {
        // FLAG_UPDATE_CURRENT überschreibt die Extras eines bestehenden Alarms; zum Canceln
        // genügt ein zur requestCode + Action passender Intent (Extras irrelevant fürs Matching).
        val flags = PendingIntent.FLAG_IMMUTABLE or
            if (mutableUpdate) PendingIntent.FLAG_UPDATE_CURRENT else 0
        return PendingIntent.getBroadcast(context, noteId.toInt(), intent, flags)
    }
}
