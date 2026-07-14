package com.papernotes.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [NoteEntity::class, NoteLinkEntity::class],
    version = 17,
    exportSchema = true,
)
abstract class PaperNotesDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao

    companion object {
        /** v2: Notiz-Typ (Checkliste), Pinnen (Washi-Tape) und Papierkorb (deletedAt). */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN type TEXT NOT NULL DEFAULT 'TEXT'")
                db.execSQL("ALTER TABLE notes ADD COLUMN pinned INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE notes ADD COLUMN deletedAt INTEGER")
            }
        }

        /** v3: Erinnerungszeit (Papier-Flattern). */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN reminderAt INTEGER")
            }
        }

        /** v4: Verknüpfungstabelle ("roter Faden") zwischen Notizen. */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `note_links` " +
                        "(`aId` INTEGER NOT NULL, `bId` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`aId`, `bId`))",
                )
            }
        }

        /** v5: Wachssiegel (private Notiz – Inhalt im Grid verborgen). */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN sealed INTEGER NOT NULL DEFAULT 0")
            }
        }

        /** v6: Ablaufzeit (vergängliche Notiz, die sich selbst zerknüllt). */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN expiresAt INTEGER")
            }
        }

        /** v7: Rückseite des Blatts (frei beschreibbare zweite Fläche pro Notiz). */
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN backText TEXT NOT NULL DEFAULT ''")
            }
        }

        /** v8: Büroklammer-Stapel (Notizen mit gleichem clipId bilden ein Bündel). */
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN clipId INTEGER")
            }
        }

        /** v9: Abreißkalender (Zieldatum als Countdown auf der Karte). */
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN countdownAt INTEGER")
            }
        }

        /** v10: Foto-Polaroid (interner Dateiname des angehängten Bildes). */
        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN photoPath TEXT")
            }
        }

        /** v11: Papier-Liniierung (blanko/liniert/kariert/gepunktet). */
        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN paper TEXT NOT NULL DEFAULT 'BLANK'")
            }
        }

        /** v12: Geheimtinte (Text im Grid verborgen, beim Halten enthüllt). */
        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN invisibleInk INTEGER NOT NULL DEFAULT 0")
            }
        }

        /** v13: Karteireiter (Tags, \n-getrennt). */
        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN tags TEXT NOT NULL DEFAULT ''")
            }
        }

        /** v14: Manuelle Reihenfolge (Pinnwand). */
        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN position INTEGER NOT NULL DEFAULT 0")
            }
        }

        /** v15: Textmarker-Markierungen. */
        val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN highlights TEXT NOT NULL DEFAULT ''")
            }
        }

        /** v16: Erledigt-Stempel. */
        val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN done INTEGER NOT NULL DEFAULT 0")
            }
        }

        /** v17: Wiederkehrende Erinnerungen (reminderRule) + Zeitkapsel-Öffnungsdatum (capsuleAt). */
        val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN reminderRule TEXT NOT NULL DEFAULT 'NONE'")
                db.execSQL("ALTER TABLE notes ADD COLUMN capsuleAt INTEGER")
            }
        }
    }
}
