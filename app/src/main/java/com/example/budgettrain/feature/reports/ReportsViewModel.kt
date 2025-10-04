package com.example.budgettrain.feature.reports

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgettrain.data.dao.CategoryTotal
import com.example.budgettrain.data.db.DatabaseProvider
import com.example.budgettrain.data.entity.Expense
import com.example.budgettrain.data.entity.Category
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReportsState(
    val startMillis: Long = 0L,
    val endMillis: Long = 0L,
    val expenses: List<Expense> = emptyList(),
    val categoryTotals: List<CategoryTotal> = emptyList(),
    val previousPeriodExpenses: List<Expense> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null
)

class ReportsViewModel(app: Application) : AndroidViewModel(app) {
    private val db = DatabaseProvider.get(app)
    private val _state = MutableStateFlow(ReportsState())
    val state: StateFlow<ReportsState> = _state.asStateFlow()
    private var loadJob: Job? = null

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
                combine(
                    db.expenseDao().getExpensesInRange(start, end),
                    db.expenseDao().getCategoryTotals(start, end),
                    db.expenseDao().getExpensesInRange(previousStart, previousEnd)
                ) { expenses, totals, previousExpenses ->
                    Triple(expenses, totals, previousExpenses)
                }.collect { (expenses, totals, previousExpenses) ->
                    _state.update { 
                        it.copy(
                            expenses = expenses, 
                            categoryTotals = totals, 
                            previousPeriodExpenses = previousExpenses,
                            loading = false
                        ) 
                    }
                }
            } catch (t: Throwable) {
                _state.update { it.copy(loading = false, error = t.message) }
            }
        }
    }

    // Demo seeding removed now that real expense creation is available
}


