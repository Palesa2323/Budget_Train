package com.example.budgettrain.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.budgettrain.data.dao.CategoryDao
import com.example.budgettrain.data.dao.ExpenseDao
import com.example.budgettrain.data.entity.BudgetGoalEntity
import com.example.budgettrain.data.entity.Category
import com.example.budgettrain.data.entity.Expense

@Database(
    entities = [Expense::class, Category::class, BudgetGoalEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao
    abstract fun categoryDao(): CategoryDao
}


