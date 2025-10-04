package com.example.budgettrain.feature.rewards

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class Reward(val title: String, val description: String)
data class Badge(val name: String, val criteria: String)

class RewardsViewModel : ViewModel() {

    private val _rewards = MutableStateFlow(
        listOf(
            Reward("Weekly Saver", "Saved R500 this week!"),
            Reward("No Spend Day", "Avoided spending for one full day.")
        )
    )
    val rewards = _rewards.asStateFlow()

    private val _badges = MutableStateFlow(
        listOf(
            Badge("Smart Saver", "Reached 3 saving goals."),
            Badge("Budget Master", "Tracked expenses for 30 consecutive days.")
        )
    )
    val badges = _badges.asStateFlow()
}
