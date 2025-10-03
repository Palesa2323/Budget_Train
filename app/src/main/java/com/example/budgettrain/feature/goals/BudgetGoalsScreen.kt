package com.example.budgettrain.feature.goals

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import java.util.Locale

@Composable
fun BudgetGoalsScreen() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("budget_goals", Context.MODE_PRIVATE) }
    var minGoal by remember { mutableFloatStateOf(prefs.getFloat("min_goal", 1000f)) }
    var maxGoal by remember { mutableFloatStateOf(prefs.getFloat("max_goal", 5000f)) }
    val currency = remember { NumberFormat.getCurrencyInstance(Locale.getDefault()) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
    ) {
        // Brand header and section title to match mock
        Text("Budget Train", style = MaterialTheme.typography.headlineMedium, color = Color(0xFF2196F3))
        Text("FOR KEEPING YOUR\nBUDGETS ON TRACK", style = MaterialTheme.typography.labelSmall, color = Color(0xFF607D8B))
        Spacer(Modifier.height(8.dp))
        Text("Budget Goals", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        Card(
            elevation = CardDefaults.cardElevation(0.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE0F2F1)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(20.dp)) {
                Text("Minimum Monthly Spending Goal", style = MaterialTheme.typography.titleSmall)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(currency.format(minGoal.toDouble()))
                    Text("0 - 20,000")
                }
                Slider(
                    value = minGoal,
                    onValueChange = { minGoal = it.coerceIn(0f, 20000f) },
                    valueRange = 0f..20000f
                )

                Spacer(Modifier.height(16.dp))

                Text("Maximum Monthly Spending Goal", style = MaterialTheme.typography.titleSmall)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(currency.format(maxGoal.toDouble()))
                    Text("0 - 20,000")
                }
                Slider(
                    value = maxGoal,
                    onValueChange = { maxGoal = it.coerceIn(0f, 20000f) },
                    valueRange = 0f..20000f
                )

                Spacer(Modifier.height(16.dp))
                Button(onClick = {
                    if (maxGoal < minGoal) {
                        Toast.makeText(context, "Max must be ≥ Min", Toast.LENGTH_SHORT).show()
                    } else {
                        prefs.edit().putFloat("min_goal", minGoal).putFloat("max_goal", maxGoal).apply()
                        Toast.makeText(context, "Goals saved", Toast.LENGTH_SHORT).show()
                    }
                }) { Text("Add Budget Goal") }
            }
        }
    }
}


