package com.example.budgettrain.feature.expense

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.Image
import coil.compose.AsyncImage
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.budgettrain.data.repository.FirebaseRepository
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Calendar
import android.app.DatePickerDialog
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton

@Composable
fun ExpenseListScreen(vm: ExpenseViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    val context = LocalContext.current
    val currency = NumberFormat.getCurrencyInstance(Locale("en", "ZA")).apply {
        currency = java.util.Currency.getInstance("ZAR")
    }
    val sdfDate = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val sdfTime = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val sdfRange = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    var startDate by remember { mutableStateOf<Long?>(null) }
    var endDate by remember { mutableStateOf<Long?>(null) }
    var showImageViewer by remember { mutableStateOf(false) }
    var selectedImagePath by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            BrandHeader()
        }
        item {
            Spacer(modifier = Modifier.height(8.dp))
        }
        item {
            Text("All Expenses", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        item {
            Spacer(modifier = Modifier.height(12.dp))
        }
        
        // Date range filter
        item {
            Card(
                elevation = CardDefaults.cardElevation(4.dp), 
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Filter by Date Range", style = MaterialTheme.typography.titleSmall, color = Color(0xFF2196F3))
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = {
                                val c = Calendar.getInstance().apply { timeInMillis = startDate ?: System.currentTimeMillis() }
                                DatePickerDialog(context, { _, y, m, d ->
                                    Calendar.getInstance().apply { set(y, m, d, 0, 0, 0); startDate = timeInMillis }
                                }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
                            },
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
                        ) { Text("Start: ${startDate?.let { sdfRange.format(it) } ?: "Select"}") }
                        
                        Button(
                            onClick = {
                                val c = Calendar.getInstance().apply { timeInMillis = endDate ?: System.currentTimeMillis() }
                                DatePickerDialog(context, { _, y, m, d ->
                                    Calendar.getInstance().apply { set(y, m, d, 23, 59, 59); endDate = timeInMillis }
                                }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
                            },
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
                        ) { Text("End: ${endDate?.let { sdfRange.format(it) } ?: "Select"}") }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = { vm.setDateRange(startDate, endDate) },
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                        ) { Text("Apply Filter") }
                        Button(
                            onClick = { 
                                startDate = null
                                endDate = null
                                vm.setDateRange(null, null)
                            },
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFF607D8B))
                        ) { Text("Clear Filter") }
                    }
                }
            }
        }
        
        // Show error if any
        if (!state.error.isNullOrBlank()) {
            item {
                Card(
                    elevation = CardDefaults.cardElevation(4.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            "Error: ${state.error}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFD32F2F),
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Check Logcat for detailed error messages. Common issues:\n" +
                            "1. Firestore security rules not configured\n" +
                            "2. Missing composite indexes (check Firebase Console)",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF757575)
                        )
                    }
                }
            }
        }
        
        if (state.filteredExpenses.isEmpty() && state.error.isNullOrBlank()) {
            item {
                Card(
                    elevation = CardDefaults.cardElevation(4.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                    ) {
                        Text(
                            "No expenses logged yet.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color(0xFF757575)
                        )
                    }
                }
            }
        } else if (state.filteredExpenses.isNotEmpty()) {
            items(state.filteredExpenses) { row: FirebaseRepository.ExpenseWithCategory ->
                Card(
                    elevation = CardDefaults.cardElevation(4.dp), 
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                sdfDate.format(row.date),
                                style = MaterialTheme.typography.titleMedium,
                                color = Color(0xFF2196F3)
                            )
                            Text(
                                currency.format(row.amount),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4CAF50)
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            row.description ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF424242)
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            val time = if (row.startTime != null && row.endTime != null) {
                                sdfTime.format(row.startTime) + " – " + sdfTime.format(row.endTime)
                            } else ""
                            Text(
                                time,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF757575)
                            )
                            Text(
                                row.categoryName,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF9C27B0)
                            )
                        }
                        if (!row.imagePath.isNullOrBlank()) {
                            Spacer(Modifier.padding(4.dp))
                            AsyncImage(
                                model = row.imagePath,
                                contentDescription = "Receipt Photo",
                                modifier = Modifier.fillMaxWidth().height(160.dp),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            if (!row.imagePath.isNullOrBlank()) {
                                IconButton(
                                    onClick = {
                                        selectedImagePath = row.imagePath
                                        showImageViewer = true
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Visibility,
                                        contentDescription = "View Image",
                                        tint = Color(0xFF2196F3)
                                    )
                                }
                            }
                            OutlinedButton(
                                onClick = { vm.deleteExpense(row.id) }
                            ) { 
                                Text(
                                    "Delete",
                                    color = Color(0xFFF44336)
                                ) 
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Image viewer dialog
    if (showImageViewer) {
        ImageViewerDialog(
            imagePath = selectedImagePath,
            onDismiss = { showImageViewer = false }
        )
    }
}

@Composable
private fun BrandHeader() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Budget Train",
            style = MaterialTheme.typography.headlineMedium,
            color = Color(0xFF2196F3)
        )
        Text(
            text = "FOR KEEPING YOUR\nBUDGETS ON TRACK",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF607D8B)
        )
    }
}
