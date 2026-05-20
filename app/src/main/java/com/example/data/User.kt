package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val email: String,
    val passwordHash: String,
    val fullName: String,
    val isGoogleUser: Boolean = false,
    val isFacebookUser: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
