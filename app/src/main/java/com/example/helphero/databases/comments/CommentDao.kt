package com.example.helphero.databases.comments

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.helphero.models.Comment

@Dao
interface CommentDao {
    @Query("SELECT * from comments ORDER BY commentId DESC")
    fun getAll(): List<Comment>

    @Query("SELECT * FROM comments WHERE commentId = :id")
    fun get(id: String): Comment

    @Query("SELECT * FROM comments WHERE userId = :userId")
    fun getUserComments(userId: String): List<Comment>

    @Query("SELECT * FROM comments WHERE postId = :postId ORDER BY commentId DESC")
    fun getPostComments(postId: String): List<Comment>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(comment: Comment)

    @Update
    suspend fun update(comment: Comment)

    @Delete
    suspend fun delete(comment: Comment)
}