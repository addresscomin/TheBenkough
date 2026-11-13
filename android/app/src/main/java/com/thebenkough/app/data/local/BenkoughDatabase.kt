package com.thebenkough.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [ScriptDraftEntity::class, WorkspaceRecordEntity::class],
    version = 5,
    exportSchema = false
)
abstract class BenkoughDatabase : RoomDatabase() {
    abstract fun scriptDraftDao(): ScriptDraftDao
    abstract fun workspaceRecordDao(): WorkspaceRecordDao

    companion object {
        @Volatile private var INSTANCE: BenkoughDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS workspace_records (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        module TEXT NOT NULL,
                        title TEXT NOT NULL,
                        details TEXT NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )"""
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE workspace_records ADD COLUMN cloudKey TEXT NOT NULL DEFAULT ''")
                db.execSQL("UPDATE workspace_records SET cloudKey = lower(hex(randomblob(16))) WHERE cloudKey = ''")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE workspace_records ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE workspace_records ADD COLUMN ownerUserId TEXT NOT NULL DEFAULT ''")
            }
        }

        fun getInstance(context: Context): BenkoughDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    BenkoughDatabase::class.java,
                    "benkough-local.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5).build().also { INSTANCE = it }
            }
    }
}
