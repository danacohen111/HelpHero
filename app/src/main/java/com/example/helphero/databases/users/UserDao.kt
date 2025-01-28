package com.example.helphero.databases.users

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Update
import com.example.helphero.models.User

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE userId = :id")
    fun get(id: String): User

    @Update
    fun update(user: User)
}