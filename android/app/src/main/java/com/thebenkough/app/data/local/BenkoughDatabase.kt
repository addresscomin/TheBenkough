package com.thebenkough.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [ScriptDraftEntity::class, WorkspaceRecordEntity::class],
    version = 2,
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

        fun getInstance(context: Context): BenkoughDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    BenkoughDatabase::class.java,
                    "benkough-local.db"
                ).addMigrations(MIGRATION_1_2).build().also { INSTANCE = it }
            }
    }
}
