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
