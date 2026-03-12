package com.aipet.brain.memory.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        EventEntity::class,
        PersonEntity::class,
        ObjectEntity::class,
        FaceProfileEntity::class,
        FaceProfileObservationLinkEntity::class,
        FaceProfileEmbeddingEntity::class,
        TeachSampleEntity::class,
        TeachSessionCompletionEntity::class,
        TraitsSnapshotEntity::class
    ],
    version = 16,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun eventDao(): EventDao
    abstract fun personDao(): PersonDao
    abstract fun objectDao(): ObjectDao
    abstract fun faceProfileDao(): FaceProfileDao
    abstract fun teachSampleDao(): TeachSampleDao
    abstract fun teachSessionCompletionDao(): TeachSessionCompletionDao
    abstract fun traitsSnapshotDao(): TraitsSnapshotDao

    companion object {
        const val DB_NAME: String = "pet_brain.db"

        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE events ADD COLUMN schema_version INTEGER NOT NULL DEFAULT 1"
                )
            }
        }

        val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `persons` (
                        `person_id` TEXT NOT NULL,
                        `display_name` TEXT NOT NULL,
                        `nickname` TEXT,
                        `is_owner` INTEGER NOT NULL,
                        `created_at_ms` INTEGER NOT NULL,
                        `updated_at_ms` INTEGER NOT NULL,
                        `last_seen_at_ms` INTEGER,
                        `familiarity_score` REAL NOT NULL DEFAULT 0.0,
                        PRIMARY KEY(`person_id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_persons_is_owner` ON `persons` (`is_owner`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_persons_updated_at_ms` ON `persons` (`updated_at_ms`)"
                )
            }
        }

        val MIGRATION_3_4: Migration = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `face_profiles` (
                        `profile_id` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `label` TEXT,
                        `note` TEXT,
                        `created_at_ms` INTEGER NOT NULL,
                        `updated_at_ms` INTEGER NOT NULL,
                        PRIMARY KEY(`profile_id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `face_profile_observation_links` (
                        `profile_id` TEXT NOT NULL,
                        `observation_id` TEXT NOT NULL,
                        `linked_at_ms` INTEGER NOT NULL,
                        PRIMARY KEY(`profile_id`, `observation_id`),
                        FOREIGN KEY(`profile_id`) REFERENCES `face_profiles`(`profile_id`) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_face_profiles_updated_at_ms` ON `face_profiles` (`updated_at_ms`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_face_profile_observation_links_profile_id` ON `face_profile_observation_links` (`profile_id`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_face_profile_observation_links_observation_id` ON `face_profile_observation_links` (`observation_id`)"
                )
            }
        }

        val MIGRATION_4_5: Migration = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `face_profile_embeddings` (
                        `embedding_id` TEXT NOT NULL,
                        `profile_id` TEXT NOT NULL,
                        `created_at_ms` INTEGER NOT NULL,
                        `vector_blob` BLOB NOT NULL,
                        `vector_dim` INTEGER NOT NULL,
                        `metadata` TEXT,
                        PRIMARY KEY(`embedding_id`),
                        FOREIGN KEY(`profile_id`) REFERENCES `face_profiles`(`profile_id`) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_face_profile_embeddings_profile_id` ON `face_profile_embeddings` (`profile_id`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_face_profile_embeddings_created_at_ms` ON `face_profile_embeddings` (`created_at_ms`)"
                )
            }
        }

        val MIGRATION_5_6: Migration = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE face_profiles ADD COLUMN linked_person_id TEXT"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_face_profiles_linked_person_id` ON `face_profiles` (`linked_person_id`)"
                )
            }
        }

        val MIGRATION_6_7: Migration = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE persons ADD COLUMN seen_count INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        val MIGRATION_7_8: Migration = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `teach_samples` (
                        `sample_id` TEXT NOT NULL,
                        `session_id` TEXT NOT NULL,
                        `observation_id` TEXT NOT NULL,
                        `observed_at_ms` INTEGER NOT NULL,
                        `source` TEXT NOT NULL,
                        `note` TEXT,
                        `image_uri` TEXT NOT NULL,
                        `created_at_ms` INTEGER NOT NULL,
                        PRIMARY KEY(`sample_id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_teach_samples_session_id` ON `teach_samples` (`session_id`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_teach_samples_created_at_ms` ON `teach_samples` (`created_at_ms`)"
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_teach_samples_observation_id` ON `teach_samples` (`observation_id`)"
                )
            }
        }

        val MIGRATION_8_9: Migration = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE teach_samples ADD COLUMN quality_status TEXT NOT NULL DEFAULT 'UNASSESSED'"
                )
                db.execSQL(
                    "ALTER TABLE teach_samples ADD COLUMN quality_flags TEXT NOT NULL DEFAULT ''"
                )
                db.execSQL(
                    "ALTER TABLE teach_samples ADD COLUMN quality_note TEXT"
                )
                db.execSQL(
                    "ALTER TABLE teach_samples ADD COLUMN quality_evaluated_at_ms INTEGER"
                )
            }
        }

        val MIGRATION_9_10: Migration = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE teach_samples ADD COLUMN face_crop_uri TEXT"
                )
            }
        }

        val MIGRATION_10_11: Migration = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `teach_session_completion` (
                        `teach_session_id` TEXT NOT NULL,
                        `is_completed_confirmed` INTEGER NOT NULL,
                        `confirmed_at_ms` INTEGER,
                        `updated_at_ms` INTEGER NOT NULL,
                        PRIMARY KEY(`teach_session_id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_teach_session_completion_updated_at_ms` ON `teach_session_completion` (`updated_at_ms`)"
                )
            }
        }

        val MIGRATION_11_12: Migration = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `traits_snapshot` (
                        `snapshot_id` TEXT NOT NULL,
                        `captured_at_ms` INTEGER NOT NULL,
                        `curiosity` REAL NOT NULL,
                        `sociability` REAL NOT NULL,
                        `energy` REAL NOT NULL,
                        `patience` REAL NOT NULL,
                        `boldness` REAL NOT NULL,
                        PRIMARY KEY(`snapshot_id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_traits_snapshot_captured_at_ms` ON `traits_snapshot` (`captured_at_ms`)"
                )
            }
        }

        val MIGRATION_12_13: Migration = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE persons ADD COLUMN familiarity_score REAL NOT NULL DEFAULT 0.0"
                )
            }
        }

        val MIGRATION_13_14: Migration = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `objects` (
                        `object_id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `created_at_ms` INTEGER NOT NULL,
                        PRIMARY KEY(`object_id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_objects_name` ON `objects` (`name`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_objects_created_at_ms` ON `objects` (`created_at_ms`)"
                )
            }
        }

        val MIGRATION_14_15: Migration = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE objects ADD COLUMN last_seen_at_ms INTEGER"
                )
                db.execSQL(
                    "ALTER TABLE objects ADD COLUMN seen_count INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_objects_last_seen_at_ms` ON `objects` (`last_seen_at_ms`)"
                )
            }
        }

        val MIGRATION_15_16: Migration = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `traits_snapshots` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `curiosity` REAL NOT NULL,
                        `sociability` REAL NOT NULL,
                        `energy` REAL NOT NULL,
                        `patience` REAL NOT NULL,
                        `boldness` REAL NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )

                val legacyTableExists = db.query(
                    "SELECT 1 FROM sqlite_master WHERE type='table' AND name='traits_snapshot' LIMIT 1"
                ).use { cursor ->
                    cursor.moveToFirst()
                }
                if (legacyTableExists) {
                    db.execSQL(
                        """
                        INSERT INTO `traits_snapshots` (
                            `curiosity`,
                            `sociability`,
                            `energy`,
                            `patience`,
                            `boldness`,
                            `createdAt`
                        )
                        SELECT
                            `curiosity`,
                            `sociability`,
                            `energy`,
                            `patience`,
                            `boldness`,
                            `captured_at_ms`
                        FROM `traits_snapshot`
                        """.trimIndent()
                    )
                }
            }
        }
    }
}
