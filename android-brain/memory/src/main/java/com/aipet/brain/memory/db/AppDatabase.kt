package com.aipet.brain.memory.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        EventEntity::class,
        PersonEntity::class,
        FaceProfileEntity::class,
        FaceProfileObservationLinkEntity::class,
        FaceProfileEmbeddingEntity::class
    ],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun eventDao(): EventDao
    abstract fun personDao(): PersonDao
    abstract fun faceProfileDao(): FaceProfileDao

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
    }
}
