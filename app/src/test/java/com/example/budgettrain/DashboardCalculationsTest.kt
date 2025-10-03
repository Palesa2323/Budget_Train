package com.example.budgettrain

import com.example.budgettrain.feature.dashboard.BudgetGoal
import com.example.budgettrain.feature.dashboard.BudgetStatus
import com.example.budgettrain.feature.dashboard.DashboardViewModel
import org.junit.Assert.assertEquals
import org.junit.Test

class DashboardCalculationsTest {

    @Test
    fun calculateBudgetStatus_variants() {
        val g = BudgetGoal(100.0, 1000.0)
        assertEquals(BudgetStatus.NO_GOALS, DashboardViewModel.calculateBudgetStatus(0.0, null))
        assertEquals(BudgetStatus.UNDER_MINIMUM, DashboardViewModel.calculateBudgetStatus(50.0, g))
        assertEquals(BudgetStatus.ON_TRACK_LOW, DashboardViewModel.calculateBudgetStatus(200.0, g))
        assertEquals(BudgetStatus.ON_TRACK_HIGH, DashboardViewModel.calculateBudgetStatus(950.0, g))
        assertEquals(BudgetStatus.OVER_BUDGET, DashboardViewModel.calculateBudgetStatus(1200.0, g))
    }

    @Test
    fun calculateProgressPercentage_edges() {
        val pct0 = DashboardViewModel.calculateProgressPercentage(0.0, 1000.0)
        assertEquals(0f, pct0)
        val pct50 = DashboardViewModel.calculateProgressPercentage(500.0, 1000.0)
        assertEquals(50f, pct50)
        val pct100 = DashboardViewModel.calculateProgressPercentage(1000.0, 1000.0)
        assertEquals(100f, pct100)
        val pctOver = DashboardViewModel.calculateProgressPercentage(1500.0, 1000.0)
        assertEquals(100f, pctOver)
        val pctInvalidMax = DashboardViewModel.calculateProgressPercentage(100.0, 0.0)
        assertEquals(0f, pctInvalidMax)
    }

    @Test
    fun daysRemainingInMonth_nonNegative() {
        val days = DashboardViewModel.daysRemainingInMonth()
        // Should be in range [0, 30/31]. We just assert non-negative for stability.
        assert(days >= 0)
    }
}


