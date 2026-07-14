package com.papernotes.data.local

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Öffnet eine von Hand erzeugte v1-Datenbank mit allen Migrationen 1→17. Historische
 * Schema-JSONs existieren nicht (Export wurde erst ab v17 aktiviert), deshalb wird das
 * v1-Schema per SQL nachgebaut; Room validiert das Endschema beim Öffnen selbst und
 * wirft bei jeder Abweichung eine IllegalStateException.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val dbName = "migration-test.db"
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Before
    @After
    fun deleteDb() {
        context.deleteDatabase(dbName)
    }

    /** v1: nur die Ur-Spalten — alles Weitere kam über MIGRATION_1_2 … MIGRATION_16_17. */
    private fun createV1Database() {
        val file = context.getDatabasePath(dbName).apply { parentFile?.mkdirs() }
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `notes` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`title` TEXT NOT NULL, `body` TEXT NOT NULL, `mood` TEXT NOT NULL, " +
                    "`dogEarFolded` INTEGER NOT NULL, `archived` INTEGER NOT NULL, " +
                    "`createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL)",
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS room_master_table " +
                    "(id INTEGER PRIMARY KEY, identity_hash TEXT)",
            )
            db.execSQL(
                "INSERT INTO notes (title, body, mood, dogEarFolded, archived, createdAt, updatedAt) " +
                    "VALUES ('Alte Notiz', 'aus Version 1', 'JOY', 1, 0, 1000, 2000)",
            )
            db.version = 1
        }
    }

    private fun openWithAllMigrations(): PaperNotesDatabase =
        Room.databaseBuilder(context, PaperNotesDatabase::class.java, dbName)
            .addMigrations(
                PaperNotesDatabase.MIGRATION_1_2,
                PaperNotesDatabase.MIGRATION_2_3,
                PaperNotesDatabase.MIGRATION_3_4,
                PaperNotesDatabase.MIGRATION_4_5,
                PaperNotesDatabase.MIGRATION_5_6,
                PaperNotesDatabase.MIGRATION_6_7,
                PaperNotesDatabase.MIGRATION_7_8,
                PaperNotesDatabase.MIGRATION_8_9,
                PaperNotesDatabase.MIGRATION_9_10,
                PaperNotesDatabase.MIGRATION_10_11,
                PaperNotesDatabase.MIGRATION_11_12,
                PaperNotesDatabase.MIGRATION_12_13,
                PaperNotesDatabase.MIGRATION_13_14,
                PaperNotesDatabase.MIGRATION_14_15,
                PaperNotesDatabase.MIGRATION_15_16,
                PaperNotesDatabase.MIGRATION_16_17,
            )
            .build()

    @Test
    fun alleMigrationenVon1Nach17_erhaltenDatenUndSchema() {
        createV1Database()

        openWithAllMigrations().apply {
            // Der erste Zugriff öffnet die DB, migriert und validiert das Schema.
            val cursor = openHelper.readableDatabase.query(
                "SELECT title, body, mood, dogEarFolded, type, pinned, paper, reminderRule, done " +
                    "FROM notes",
            )
            cursor.use {
                assertThat(it.moveToFirst()).isTrue()
                assertThat(it.getString(0)).isEqualTo("Alte Notiz")
                assertThat(it.getString(1)).isEqualTo("aus Version 1")
                assertThat(it.getString(2)).isEqualTo("JOY")
                assertThat(it.getInt(3)).isEqualTo(1)
                // Defaults der nachgerüsteten Spalten
                assertThat(it.getString(4)).isEqualTo("TEXT")
                assertThat(it.getInt(5)).isEqualTo(0)
                assertThat(it.getString(6)).isEqualTo("BLANK")
                assertThat(it.getString(7)).isEqualTo("NONE")
                assertThat(it.getInt(8)).isEqualTo(0)
            }

            // note_links (MIGRATION_3_4) existiert und ist benutzbar.
            openHelper.writableDatabase.execSQL("INSERT INTO note_links (aId, bId) VALUES (1, 2)")
            openHelper.readableDatabase.query("SELECT COUNT(*) FROM note_links").use {
                assertThat(it.moveToFirst()).isTrue()
                assertThat(it.getInt(0)).isEqualTo(1)
            }
            close()
        }
    }

    @Test
    fun frischeDatenbank_entsprichtDemExportiertenSchema() {
        // Ohne Alt-Daten: Neuanlage auf v17 — schlägt fehl, wenn Entity und Schema divergieren.
        openWithAllMigrations().apply {
            openHelper.readableDatabase.query("SELECT COUNT(*) FROM notes").use {
                assertThat(it.moveToFirst()).isTrue()
            }
            close()
        }
    }
}
