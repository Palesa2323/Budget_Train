package com.example.budgettrain.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budget_goals")
data class BudgetGoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val monthKey: String,   // e.g., "2025-10"
    val minimumGoal: Double,
    val maximumGoal: Double
)


