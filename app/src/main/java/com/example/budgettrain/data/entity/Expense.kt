package com.example.budgettrain.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val categoryId: Long,
    val amount: Double,
    val date: Long,            // epoch millis
    val startTime: Long? = null,
    val endTime: Long? = null,
    val description: String? = null,
    val imagePath: String? = null
)


