package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "api_configs")
data class ApiConfig(
    @PrimaryKey val id: Int = 1,
    val apiKey: String = "",
    val monthlyBudget: Double = 100.0,
    val isDemoMode: Boolean = true
)
