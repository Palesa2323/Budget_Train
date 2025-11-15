package com.example.budgettrain.feature.expense

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgettrain.data.repository.FirebaseRepository
import com.example.budgettrain.data.repository.FirebaseAuthRepository
import com.example.budgettrain.data.entity.Expense
import com.example.budgettrain.data.entity.Category
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

typealias ExpenseWithCategory = FirebaseRepository.ExpenseWithCategory

data class ExpenseState(
    val categories: List<Pair<Long, String>> = emptyList(),
    val expenses: List<ExpenseWithCategory> = emptyList(),
    val filteredExpenses: List<ExpenseWithCategory> = emptyList(),
    val startDate: Long? = null,
    val endDate: Long? = null,
    val error: String? = null,
    val saving: Boolean = false
)

class ExpenseViewModel(app: Application) : AndroidViewModel(app) {
    private val authRepository = FirebaseAuthRepository()
    private val _state = MutableStateFlow(ExpenseState())
    val state: StateFlow<ExpenseState> = _state.asStateFlow()

    init {
        val userId = authRepository.currentUserId
        android.util.Log.d("ExpenseViewModel", "Initializing ExpenseViewModel with userId: $userId")
        if (userId == null) {
            _state.value = _state.value.copy(error = "User not logged in")
        } else {
            viewModelScope.launch {
                FirebaseRepository.getAllCategories(userId).collect { cats ->
                    android.util.Log.d("ExpenseViewModel", "Received ${cats.size} categories")
                    _state.value = _state.value.copy(categories = cats.map { it.id to it.name })
                }
            }
            viewModelScope.launch {
                FirebaseRepository.getExpensesWithCategory(userId).collect { rows ->
                    android.util.Log.d("ExpenseViewModel", "Received ${rows.size} expenses with categories")
                    if (rows.isEmpty() && _state.value.expenses.isEmpty()) {
                        // Only show error if we've been waiting and still have no data
                        // This helps identify if it's a real error vs just no data yet
                        android.util.Log.w("ExpenseViewModel", "No expenses found. Check Logcat for Firestore errors.")
                    }
                    _state.value = _state.value.copy(expenses = rows, filteredExpenses = rows)
                }
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
                FirebaseRepository.addExpense(
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
                val firebaseUserId = authRepository.currentUserId
                    ?: throw IllegalStateException("User not logged in")
                
                // Convert Firebase UID string to Long using hashCode for consistency
                val firebaseUserIdLong = firebaseUserId.hashCode().toLong()
                
                val resolvedCategoryId: Long = if (trimmed.isNotEmpty()) {
                    val existing = FirebaseRepository.getCategoryByName(firebaseUserId, trimmed)
                    existing?.id ?: run {
                        val newCategory = Category(userId = firebaseUserIdLong, name = trimmed, color = 0xFF607D8B)
                        val categoryDocId = FirebaseRepository.addCategory(newCategory)
                        // Convert Firebase document ID to Long using hashCode for consistency
                        categoryDocId.hashCode().toLong()
                    }
                } else {
                    fallbackCategoryId!!
                }
                val expenseDocId = FirebaseRepository.addExpense(
                    Expense(
                        userId = firebaseUserIdLong,
                        categoryId = resolvedCategoryId,
                        amount = amount,
                        date = date,
                        startTime = startTime,
                        endTime = endTime,
                        description = description,
                        imagePath = imagePath
                    )
                )
                android.util.Log.d("ExpenseViewModel", "Expense saved successfully with document ID: $expenseDocId")
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
                defaults.forEach { category ->
                    FirebaseRepository.addCategory(category)
                }
            } catch (t: Throwable) {
                _state.value = _state.value.copy(error = t.message)
            }
        }
    }

    fun deleteExpense(documentId: String) {
        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(error = null)
                android.util.Log.d("ExpenseViewModel", "Deleting expense with document ID: $documentId")
                FirebaseRepository.deleteExpense(documentId)
                android.util.Log.d("ExpenseViewModel", "Expense deleted successfully")
            } catch (t: Throwable) {
                android.util.Log.e("ExpenseViewModel", "Error deleting expense: ${t.message}", t)
                _state.value = _state.value.copy(error = "Failed to delete expense: ${t.message}")
            }
        }
    }

    fun deleteCategory(documentId: String) {
        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(error = null)
                android.util.Log.d("ExpenseViewModel", "Deleting category with document ID: $documentId")
                FirebaseRepository.deleteCategory(documentId)
                android.util.Log.d("ExpenseViewModel", "Category deleted successfully")
            } catch (t: Throwable) {
                android.util.Log.e("ExpenseViewModel", "Error deleting category: ${t.message}", t)
                _state.value = _state.value.copy(error = "Failed to delete category: ${t.message}")
            }
        }
    }

    fun deleteCategoryByName(categoryName: String) {
        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(error = null)
                val firebaseUserId = authRepository.currentUserId
                    ?: throw IllegalStateException("User not logged in")
                
                val documentId = FirebaseRepository.getCategoryDocumentIdByName(firebaseUserId, categoryName)
                    ?: throw IllegalArgumentException("Category '$categoryName' not found")
                
                android.util.Log.d("ExpenseViewModel", "Deleting category '$categoryName' with document ID: $documentId")
                FirebaseRepository.deleteCategory(documentId)
                android.util.Log.d("ExpenseViewModel", "Category deleted successfully")
            } catch (t: Throwable) {
                android.util.Log.e("ExpenseViewModel", "Error deleting category: ${t.message}", t)
                _state.value = _state.value.copy(error = "Failed to delete category: ${t.message}")
            }
        }
    }

    fun setDateRange(start: Long?, end: Long?) {
        val startDate = start?.let { 
            java.util.Calendar.getInstance().apply {
                timeInMillis = it
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }.timeInMillis
        }
        val endDate = end?.let {
            java.util.Calendar.getInstance().apply {
                timeInMillis = it
                set(java.util.Calendar.HOUR_OF_DAY, 23)
                set(java.util.Calendar.MINUTE, 59)
                set(java.util.Calendar.SECOND, 59)
                set(java.util.Calendar.MILLISECOND, 999)
            }.timeInMillis
        }
        _state.value = _state.value.copy(startDate = startDate, endDate = endDate)
        applyDateFilter()
    }

    private fun applyDateFilter() {
        val expenses = _state.value.expenses
        val start = _state.value.startDate
        val end = _state.value.endDate
        val filtered = if (start != null && end != null) {
            expenses.filter { expense ->
                expense.date >= start && expense.date <= end
            }
        } else {
            expenses
        }
        _state.value = _state.value.copy(filteredExpenses = filtered)
    }
}


