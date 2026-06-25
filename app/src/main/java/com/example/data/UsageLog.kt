package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "usage_logs")
data class UsageLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val commandName: String,
    val timestamp: Long,
    val modelName: String,
    val inputTokens: Int,
    val outputTokens: Int,
    val cost: Double,
    val isPlaygroundQuery: Boolean,
    val prompt: String = "",
    val responseText: String = ""
)
