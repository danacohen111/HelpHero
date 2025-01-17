package com.example.helphero.converters

import androidx.room.TypeConverter
import com.example.helphero.models.Comment
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

class Converters {
    @TypeConverter
    fun fromList(list: List<Comment?>?): String {
        val gson = Gson()
        return gson.toJson(list)
    }

    @TypeConverter
    fun fromString(value: String?): List<Comment> {
        if (value.isNullOrEmpty()) {
            return emptyList()
        }
        val listType: Type = object : TypeToken<List<Comment?>?>() {}.getType()
        return Gson().fromJson(value, listType)
    }
}
