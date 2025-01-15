package com.example.helphero.databases.users

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Update
import com.example.helphero.models.User

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE email = :email")
    fun get(email: String): User

    @Update
    suspend fun update(user: User)

//    @Delete
//    fun delete(id: String)
}