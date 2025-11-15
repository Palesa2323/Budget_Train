package com.example.budgettrain.feature.reports

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgettrain.data.repository.FirebaseRepository
import com.example.budgettrain.data.repository.FirebaseAuthRepository
import com.example.budgettrain.data.entity.Expense
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

data class ReportsState(
    val startMillis: Long = 0L,
    val endMillis: Long = 0L,
    val expenses: List<Expense> = emptyList(),
    val categoryTotals: List<FirebaseRepository.CategoryTotal> = emptyList(),
    val previousPeriodExpenses: List<Expense> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
    val hasLoadedData: Boolean = false
)

class ReportsViewModel(app: Application) : AndroidViewModel(app) {
    private val authRepository = FirebaseAuthRepository()
    private val _state = MutableStateFlow(ReportsState())
    val state: StateFlow<ReportsState> = _state.asStateFlow()
    private var loadJob: Job? = null

    init {
        // Initialize with default date range (current month)
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startOfMonth = cal.timeInMillis
        val endOfMonth = System.currentTimeMillis()
        
        _state.update { 
            it.copy(
                startMillis = startOfMonth, 
                endMillis = endOfMonth
            ) 
        }
    }

    fun setRange(start: Long, end: Long) {
        _state.update { it.copy(startMillis = start, endMillis = end) }
    }

    fun load() {
        val start = _state.value.startMillis
        val end = _state.value.endMillis
        if (end < start) {
            _state.update { it.copy(error = "Invalid range", loading = false) }
            return
        }
        
        // Calculate previous period (same duration, before current period)
        val duration = end - start
        val previousEnd = start - 1
        val previousStart = previousEnd - duration
        
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            try {
                val userId = authRepository.currentUserId
                
                // Use first() to get a single emission from each flow instead of collecting indefinitely
                // This prevents the coroutine from being cancelled
                val (expenses, totals, previousExpenses) = combine(
                    FirebaseRepository.getExpensesInRange(userId, start, end),
                    FirebaseRepository.getCategoryTotals(userId, start, end),
                    FirebaseRepository.getExpensesInRange(userId, previousStart, previousEnd)
                ) { expenses, totals, previousExpenses ->
                    Triple(expenses, totals, previousExpenses)
                }.first()
                
                _state.update { 
                    it.copy(
                        expenses = expenses, 
                        categoryTotals = totals, 
                        previousPeriodExpenses = previousExpenses,
                        loading = false,
                        hasLoadedData = true,
                        error = null
                    ) 
                }
            } catch (t: Throwable) {
                android.util.Log.e("ReportsViewModel", "Error loading reports: ${t.message}", t)
                _state.update { 
                    it.copy(
                        loading = false, 
                        error = "Failed to load reports: ${t.message ?: "Unknown error"}"
                    ) 
                }
            }
        }
    }
}


