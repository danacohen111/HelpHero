package com.example.helphero.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "posts")
data class Post (
    @PrimaryKey var postId: String,
    @ColumnInfo(name = "userId") var userId: String,
    @ColumnInfo(name = "title") var title: String,
    @ColumnInfo(name = "desc") var desc: String,
    @ColumnInfo(name = "imageUrl") var imageUrl: String,
    @ColumnInfo(name = "date") var date: String,
    @ColumnInfo(name = "comments") var comments: List<Comment>
)

data class FirestorePost(
    val userId: String = "",
    val title: String = "",
    val desc: String = "",
    val imageUrl: String = "",
    val date: String = "",
    val comments: List<Comment> = emptyList()
)

fun FirestorePost.toRoomPost(postId: String): Post {
    return Post(
        postId = postId,
        userId = this.userId,
        title = this.title,
        desc = this.desc,
        imageUrl = this.imageUrl,
        date = this.date,
        comments = this.comments
    )
}

fun Post.toFirestorePost(): FirestorePost {
    return FirestorePost(
        userId = this.userId,
        title = this.title,
        desc = this.desc,
        imageUrl = this.imageUrl,
        date = this.date,
        comments = this.comments
    )
}