package com.example.helphero.databases.comments

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.helphero.models.Comment

@Database(entities = [Comment::class], version = 1, exportSchema = false)
abstract class CommentDatabase: RoomDatabase() {

    abstract fun commentDao(): CommentDao

    companion object {

        @Volatile
        private var INSTANCE: CommentDatabase? = null

        fun getDatabase(context: Context): CommentDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CommentDatabase::class.java,
                    "comments_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}