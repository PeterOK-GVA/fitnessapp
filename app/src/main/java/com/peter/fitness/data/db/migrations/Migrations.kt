package com.peter.fitness.data.db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2: Migration = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS active_rest_timer (
                id TEXT NOT NULL PRIMARY KEY,
                started_at INTEGER NOT NULL,
                duration_seconds INTEGER NOT NULL,
                session_id TEXT,
                set_entry_id TEXT,
                label TEXT
            )
            """.trimIndent(),
        )
    }
}

val MIGRATION_2_3: Migration = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS progression_state (
                exercise_id TEXT NOT NULL PRIMARY KEY,
                current_load_kg REAL NOT NULL,
                current_target_reps INTEGER NOT NULL,
                working_range_min_reps INTEGER NOT NULL,
                working_range_max_reps INTEGER NOT NULL,
                last_stimulus TEXT,
                volume_streak INTEGER NOT NULL,
                consecutive_successes INTEGER NOT NULL,
                updated_at INTEGER NOT NULL
            )
            """.trimIndent(),
        )
    }
}

val MIGRATION_3_4: Migration = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE progression_state ADD COLUMN deload_counter INTEGER NOT NULL DEFAULT 0",
        )
        db.execSQL(
            "ALTER TABLE progression_state ADD COLUMN consecutive_misses INTEGER NOT NULL DEFAULT 0",
        )
        db.execSQL(
            "ALTER TABLE progression_state " +
                "ADD COLUMN consecutive_much_too_heavy INTEGER NOT NULL DEFAULT 0",
        )
    }
}
