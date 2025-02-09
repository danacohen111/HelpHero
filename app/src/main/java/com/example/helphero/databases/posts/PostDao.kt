package com.example.helphero.databases.posts

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.helphero.models.Post

@Dao
interface PostDao {
    @Query("SELECT * from posts ORDER BY date DESC")
    fun getAll(): List<Post>

    @Query("SELECT * FROM posts WHERE postId = :id")
    fun get(id: String): Post?

    @Query("SELECT * FROM posts WHERE userId = :userId")
    fun getUserPosts(userId: String): List<Post>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(post: Post)

    @Update
    fun update(post: Post)

    @Delete
    fun delete(post: Post)

    @Query("DELETE FROM posts WHERE postId = :postId")
    fun deleteById(postId: String)
}