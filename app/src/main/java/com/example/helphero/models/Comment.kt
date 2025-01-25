package com.example.helphero.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "comments")
data class Comment (
    @PrimaryKey var commentId: String,
    @ColumnInfo(name = "postId") var postId: String,
    @ColumnInfo(name = "userId") var userId: String,
    @ColumnInfo(name = "text") var text: String,
    @ColumnInfo(name = "date") var date: String
)

data class FirestoreComment(
    val commentId: String = "",
    val postId: String = "",
    val userId: String = "",
    val text: String = "",
    val date: String = ""
    )

fun FirestoreComment.toRoomComment(): Comment {
    return Comment(
        commentId = this.commentId,
        postId = this.postId,
        userId = this.userId,
        text = this.text,
        date = this.date
        )
}

fun Comment.toFirestoreComment(): FirestoreComment {
    return FirestoreComment(
        commentId = this.commentId,
        postId = this.postId,
        userId = this.userId,
        date = this.date,
        text = this.text
    )
}