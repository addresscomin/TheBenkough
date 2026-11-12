package com.thebenkough.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [ScriptDraftEntity::class],
    version = 1,
    exportSchema = false
)
abstract class BenkoughDatabase : RoomDatabase() {
    abstract fun scriptDraftDao(): ScriptDraftDao
}
