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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
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
    var minGoalText by remember { mutableStateOf(minGoal.toInt().toString()) }
    var maxGoalText by remember { mutableStateOf(maxGoal.toInt().toString()) }
    val currency = remember { 
        NumberFormat.getCurrencyInstance(Locale("en", "ZA")).apply {
            currency = java.util.Currency.getInstance("ZAR")
        }
    }

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
                Spacer(Modifier.height(8.dp))
                
                // Text input for minimum goal
                OutlinedTextField(
                    value = minGoalText,
                    onValueChange = { newValue ->
                        minGoalText = newValue.filter { it.isDigit() }
                        val newMinGoal = minGoalText.toFloatOrNull()?.coerceIn(0f, 20000f) ?: minGoal
                        if (newMinGoal != minGoal) {
                            minGoal = newMinGoal
                        }
                    },
                    label = { Text("Minimum Amount (ZAR)") },
                    placeholder = { Text("Enter minimum spending goal in Rands") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    suffix = { Text("R") }
                )
                
                Spacer(Modifier.height(8.dp))
                
                // Slider for minimum goal
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(currency.format(minGoal.toDouble()))
                    Text("R0 - R20,000")
                }
                Slider(
                    value = minGoal,
                    onValueChange = { 
                        minGoal = it.coerceIn(0f, 20000f)
                        minGoalText = minGoal.toInt().toString()
                    },
                    valueRange = 0f..20000f
                )

                Spacer(Modifier.height(16.dp))

                Text("Maximum Monthly Spending Goal", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                
                // Text input for maximum goal
                OutlinedTextField(
                    value = maxGoalText,
                    onValueChange = { newValue ->
                        maxGoalText = newValue.filter { it.isDigit() }
                        val newMaxGoal = maxGoalText.toFloatOrNull()?.coerceIn(0f, 20000f) ?: maxGoal
                        if (newMaxGoal != maxGoal) {
                            maxGoal = newMaxGoal
                        }
                    },
                    label = { Text("Maximum Amount (ZAR)") },
                    placeholder = { Text("Enter maximum spending goal in Rands") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    suffix = { Text("R") }
                )
                
                Spacer(Modifier.height(8.dp))
                
                // Slider for maximum goal
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(currency.format(maxGoal.toDouble()))
                    Text("R0 - R20,000")
                }
                Slider(
                    value = maxGoal,
                    onValueChange = { 
                        maxGoal = it.coerceIn(0f, 20000f)
                        maxGoalText = maxGoal.toInt().toString()
                    },
                    valueRange = 0f..20000f
                )

                Spacer(Modifier.height(16.dp))
                
                // Budget summary
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Budget Summary", style = MaterialTheme.typography.titleSmall, color = Color(0xFF666666))
                        Spacer(Modifier.height(4.dp))
                        Text("Range: ${currency.format(minGoal.toDouble())} - ${currency.format(maxGoal.toDouble())}", 
                             style = MaterialTheme.typography.bodyMedium)
                        Text("Budget Window: ${currency.format((maxGoal - minGoal).toDouble())}", 
                             style = MaterialTheme.typography.bodySmall, color = Color(0xFF666666))
                    }
                }
                
                Spacer(Modifier.height(16.dp))
                
                // Validation and save button
                Button(
                    onClick = {
                        // Validate text inputs
                        val minValue = minGoalText.toFloatOrNull()
                        val maxValue = maxGoalText.toFloatOrNull()
                        
                        when {
                            minValue == null -> {
                                Toast.makeText(context, "Please enter a valid minimum amount", Toast.LENGTH_SHORT).show()
                            }
                            maxValue == null -> {
                                Toast.makeText(context, "Please enter a valid maximum amount", Toast.LENGTH_SHORT).show()
                            }
                            minValue < 0 || minValue > 20000 -> {
                                Toast.makeText(context, "Minimum must be between R0 and R20,000", Toast.LENGTH_SHORT).show()
                            }
                            maxValue < 0 || maxValue > 20000 -> {
                                Toast.makeText(context, "Maximum must be between R0 and R20,000", Toast.LENGTH_SHORT).show()
                            }
                            maxValue < minValue -> {
                                Toast.makeText(context, "Maximum must be ≥ Minimum", Toast.LENGTH_SHORT).show()
                            }
                            else -> {
                                // Update slider values to match text inputs
                                minGoal = minValue
                                maxGoal = maxValue
                                
                                // Save to preferences
                                prefs.edit().putFloat("min_goal", minGoal).putFloat("max_goal", maxGoal).apply()
                                Toast.makeText(context, "Budget goals saved successfully!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { 
                    Text("Save Budget Goals", style = MaterialTheme.typography.titleMedium) 
                }
            }
        }
    }
}


