package com.example.helphero.databases.posts

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.helphero.models.Post
import kotlinx.coroutines.flow.Flow

@Dao
interface PostDao {
    @Query("SELECT * from posts ORDER BY date DESC")
    fun getAll(): List<Post>

    @Query("SELECT * FROM posts WHERE postId = :id")
    fun get(id: Int): Post

    @Query("SELECT * FROM posts WHERE userId = :userId")
    fun getUserPosts(userId: String): Flow<List<Post>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(post: Post)

    @Update
    suspend fun update(post: Post)

    @Delete
    suspend fun delete(post: Post)
}