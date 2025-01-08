package com.example.helphero.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "comments")
data class Comment (
    @PrimaryKey var commentId: String,
    @ColumnInfo(name = "userId") var userId: String,
    @ColumnInfo(name = "text") var text: String
)

data class FirestoreComment(
    val userId: String = "",
    val text: String = ""
)

fun FirestoreComment.toRoomComment(commentId: String): Comment {
    return Comment(
        commentId = commentId,
        userId = this.userId,
        text = this.text
    )
}

fun Comment.toFirestoreComment(): FirestoreComment {
    return FirestoreComment(
        userId = this.userId,
        text = this.text
    )
}