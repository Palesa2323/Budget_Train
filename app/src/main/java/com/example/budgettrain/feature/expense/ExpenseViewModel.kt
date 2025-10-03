package com.example.budgettrain.feature.expense

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgettrain.data.dao.ExpenseWithCategory
import com.example.budgettrain.data.db.DatabaseProvider
import com.example.budgettrain.data.entity.Expense
import com.example.budgettrain.data.entity.Category
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ExpenseState(
    val categories: List<Pair<Long, String>> = emptyList(),
    val expenses: List<ExpenseWithCategory> = emptyList(),
    val error: String? = null,
    val saving: Boolean = false
)

class ExpenseViewModel(app: Application) : AndroidViewModel(app) {
    private val db = DatabaseProvider.get(app)
    private val _state = MutableStateFlow(ExpenseState())
    val state: StateFlow<ExpenseState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            db.categoryDao().getAll().collect { cats ->
                _state.value = _state.value.copy(categories = cats.map { it.id to it.name })
            }
        }
        viewModelScope.launch {
            db.expenseDao().getAllExpensesWithCategory().collect { rows ->
                _state.value = _state.value.copy(expenses = rows)
            }
        }
    }

    fun saveExpense(
        userId: Long,
        categoryId: Long,
        amount: Double,
        date: Long,
        startTime: Long?,
        endTime: Long?,
        description: String?,
        imagePath: String?
    ) {
        if (amount <= 0.0) {
            _state.value = _state.value.copy(error = "Please enter a valid amount")
            return
        }
        if (description.isNullOrBlank() || description.length < 3) {
            _state.value = _state.value.copy(error = "Description must be at least 3 characters")
            return
        }
        if (startTime != null && endTime != null && endTime < startTime) {
            _state.value = _state.value.copy(error = "End time must be ≥ start time")
            return
        }
        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(saving = true, error = null)
                db.expenseDao().insert(
                    Expense(
                        userId = userId,
                        categoryId = categoryId,
                        amount = amount,
                        date = date,
                        startTime = startTime,
                        endTime = endTime,
                        description = description,
                        imagePath = imagePath
                    )
                )
                _state.value = _state.value.copy(saving = false)
            } catch (t: Throwable) {
                _state.value = _state.value.copy(saving = false, error = t.message)
            }
        }
    }

    fun saveExpenseWithCategoryName(
        userId: Long,
        categoryName: String?,
        fallbackCategoryId: Long?,
        amount: Double,
        date: Long,
        startTime: Long?,
        endTime: Long?,
        description: String?,
        imagePath: String?
    ) {
        val trimmed = categoryName?.trim().orEmpty()
        if (trimmed.isEmpty() && fallbackCategoryId == null) {
            _state.value = _state.value.copy(error = "Please specify a category")
            return
        }
        if (amount <= 0.0) {
            _state.value = _state.value.copy(error = "Please enter a valid amount")
            return
        }
        if (description.isNullOrBlank() || description.length < 3) {
            _state.value = _state.value.copy(error = "Description must be at least 3 characters")
            return
        }
        if (startTime != null && endTime != null && endTime < startTime) {
            _state.value = _state.value.copy(error = "End time must be ≥ start time")
            return
        }
        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(saving = true, error = null)
                val resolvedCategoryId: Long = if (trimmed.isNotEmpty()) {
                    val existing = db.categoryDao().getByName(trimmed)
                    existing?.id ?: db.categoryDao().upsert(Category(userId = userId, name = trimmed, color = 0xFF607D8B))
                } else {
                    fallbackCategoryId!!
                }
                db.expenseDao().insert(
                    Expense(
                        userId = userId,
                        categoryId = resolvedCategoryId,
                        amount = amount,
                        date = date,
                        startTime = startTime,
                        endTime = endTime,
                        description = description,
                        imagePath = imagePath
                    )
                )
                _state.value = _state.value.copy(saving = false)
            } catch (t: Throwable) {
                _state.value = _state.value.copy(saving = false, error = t.message)
            }
        }
    }

    fun createDefaultCategories(userId: Long = 1L) {
        viewModelScope.launch {
            try {
                val defaults = listOf(
                    Category(userId = userId, name = "General", color = 0xFF607D8B),
                    Category(userId = userId, name = "Food", color = 0xFF4CAF50),
                    Category(userId = userId, name = "Transport", color = 0xFF2196F3)
                )
                db.categoryDao().upsertAll(defaults)
            } catch (t: Throwable) {
                _state.value = _state.value.copy(error = t.message)
            }
        }
    }

    fun deleteExpense(id: Long) {
        viewModelScope.launch {
            try {
                db.expenseDao().deleteById(id)
            } catch (t: Throwable) {
                _state.value = _state.value.copy(error = t.message)
            }
        }
    }
}


