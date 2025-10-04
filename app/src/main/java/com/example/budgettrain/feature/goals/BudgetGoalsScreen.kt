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
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.budgettrain.feature.dashboard.BrandHeader
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

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            BrandHeader()
        }
        
        item {
            Text(
                "Budget Goals",
                style = MaterialTheme.typography.titleLarge,
                color = Color(0xFF2196F3)
            )
        }
        
        item {
            Card(
                elevation = CardDefaults.cardElevation(4.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        "Minimum Monthly Spending Goal",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color(0xFF2196F3)
                    )
                    
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
                    
                    // Slider for minimum goal
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            currency.format(minGoal.toDouble()),
                            color = Color(0xFF4CAF50)
                        )
                        Text(
                            "R0 - R20,000",
                            color = Color(0xFF757575)
                        )
                    }
                    Slider(
                        value = minGoal,
                        onValueChange = { 
                            minGoal = it.coerceIn(0f, 20000f)
                            minGoalText = minGoal.toInt().toString()
                        },
                        valueRange = 0f..20000f
                    )

                    Text(
                        "Maximum Monthly Spending Goal",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color(0xFF2196F3)
                    )
                    
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
                    
                    // Slider for maximum goal
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            currency.format(maxGoal.toDouble()),
                            color = Color(0xFF4CAF50)
                        )
                        Text(
                            "R0 - R20,000",
                            color = Color(0xFF757575)
                        )
                    }
                    Slider(
                        value = maxGoal,
                        onValueChange = { 
                            maxGoal = it.coerceIn(0f, 20000f)
                            maxGoalText = maxGoal.toInt().toString()
                        },
                        valueRange = 0f..20000f
                    )

                    // Budget summary
                    Card(
                        elevation = CardDefaults.cardElevation(2.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                "Budget Summary",
                                style = MaterialTheme.typography.titleSmall,
                                color = Color(0xFF2196F3)
                            )
                            Text(
                                "Range: ${currency.format(minGoal.toDouble())} - ${currency.format(maxGoal.toDouble())}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF4CAF50)
                            )
                            Text(
                                "Budget Window: ${currency.format((maxGoal - minGoal).toDouble())}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF757575)
                            )
                        }
                    }
                    
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
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                        modifier = Modifier.fillMaxWidth()
                    ) { 
                        Text(
                            "Save Budget Goals",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        ) 
                    }
                }
            }
        }
    }
}