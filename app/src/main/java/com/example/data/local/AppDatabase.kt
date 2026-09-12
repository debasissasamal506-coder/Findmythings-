package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [ItemEntity::class, BoxEntity::class, TaskEntity::class, SubTaskEntity::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun itemDao(): ItemDao
    abstract fun boxDao(): BoxDao
    abstract fun taskDao(): TaskDao
    abstract fun subTaskDao(): SubTaskDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE items ADD COLUMN audioUri TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE items ADD COLUMN audioDurationSec INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS tasks (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        description TEXT NOT NULL DEFAULT '',
                        category TEXT NOT NULL DEFAULT 'Home',
                        priority TEXT NOT NULL DEFAULT 'Medium',
                        dueDate INTEGER DEFAULT NULL,
                        dueTimeFormatted TEXT NOT NULL DEFAULT '',
                        isCompleted INTEGER NOT NULL DEFAULT 0,
                        completedAt INTEGER DEFAULT NULL,
                        reminderSet INTEGER NOT NULL DEFAULT 0,
                        reminderTime INTEGER DEFAULT NULL,
                        repeatType TEXT NOT NULL DEFAULT 'None',
                        relatedThingId INTEGER DEFAULT NULL,
                        relatedThingName TEXT DEFAULT NULL,
                        relatedThingLocation TEXT DEFAULT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_tasks_isCompleted ON tasks(isCompleted)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_tasks_dueDate ON tasks(dueDate)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_tasks_relatedThingId ON tasks(relatedThingId)")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN relatedBoxId INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE tasks ADD COLUMN relatedBoxName TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE tasks ADD COLUMN relatedBoxLocation TEXT DEFAULT NULL")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_tasks_relatedBoxId ON tasks(relatedBoxId)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS subtasks (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        taskId INTEGER NOT NULL,
                        title TEXT NOT NULL,
                        isCompleted INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY(taskId) REFERENCES tasks(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_subtasks_taskId ON subtasks(taskId)")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "find_my_things_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
