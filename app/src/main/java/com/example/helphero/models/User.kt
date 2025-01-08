package com.example.helphero.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User (
    @PrimaryKey var userId: String,
    @ColumnInfo(name = "name") var name: String,
    @ColumnInfo(name = "phone") var phone: String,
    @ColumnInfo(name = "photoUrl") var photoUrl: String,
    @ColumnInfo(name = "email") var email: String,
    @ColumnInfo(name = "password") var password: String
)

data class FirestoreUser(
    val name: String = "",
    val phone: String = "",
    val photoUrl: String = "",
    val email: String = "",
    val password: String = ""
)

fun FirestoreUser.toRoomUser(userId: String): User {
    return User(
        userId = userId,
        name = this.name,
        phone = this.phone,
        photoUrl = this.photoUrl,
        email = this.email,
        password = this.password
    )
}

fun User.toFirestoreUser(): FirestoreUser {
    return FirestoreUser(
        name = this.name,
        phone = this.phone,
        photoUrl = this.photoUrl,
        email = this.email,
        password = this.password
    )
}