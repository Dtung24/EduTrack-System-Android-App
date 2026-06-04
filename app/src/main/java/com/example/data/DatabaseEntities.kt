package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "students")
data class StudentEntity(
    @PrimaryKey val uid: String,
    val name: String,
    val studentId: String,
    val className: String?,
    val photo: String? // Base64
)

@Entity(tableName = "classes")
data class ClassEntity(
    @PrimaryKey val className: String
)

@Entity(tableName = "logs")
data class LogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val firebaseKey: String?, // Key from Firebase if synced
    val uid: String,
    val deviceId: String,
    val event: String, // CHECK_IN, CHECK_OUT, APB_VIOLATION
    val durationSeconds: Long?, // Nullable
    val timestamp: Long,
    val displayTime: String? // Custom formatted time string
)
