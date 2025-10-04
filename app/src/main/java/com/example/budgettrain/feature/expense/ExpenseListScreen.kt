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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.budgettrain.data.dao.ExpenseWithCategory
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

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("All Expenses", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.padding(6.dp))
        
        // Date range filter
        Card(elevation = CardDefaults.cardElevation(2.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Text("Filter by Date Range", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        val c = Calendar.getInstance().apply { timeInMillis = startDate ?: System.currentTimeMillis() }
                        DatePickerDialog(context, { _, y, m, d ->
                            Calendar.getInstance().apply { set(y, m, d, 0, 0, 0); startDate = timeInMillis }
                        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
                    }) { Text("Start: ${startDate?.let { sdfRange.format(it) } ?: "Select"}") }
                    
                    Button(onClick = {
                        val c = Calendar.getInstance().apply { timeInMillis = endDate ?: System.currentTimeMillis() }
                        DatePickerDialog(context, { _, y, m, d ->
                            Calendar.getInstance().apply { set(y, m, d, 23, 59, 59); endDate = timeInMillis }
                        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
                    }) { Text("End: ${endDate?.let { sdfRange.format(it) } ?: "Select"}") }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { vm.setDateRange(startDate, endDate) }) { Text("Apply Filter") }
                    Button(onClick = { 
                        startDate = null
                        endDate = null
                        vm.setDateRange(null, null)
                    }) { Text("Clear Filter") }
                }
            }
        }
        
        Spacer(Modifier.height(12.dp))
        
        if (state.filteredExpenses.isEmpty()) {
            Text("No expenses logged yet.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.filteredExpenses) { row ->
                    Card(elevation = CardDefaults.cardElevation(2.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(sdfDate.format(row.date))
                                Text(currency.format(row.amount))
                            }
                            Text(row.description ?: "")
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                val time = if (row.startTime != null && row.endTime != null) {
                                    sdfTime.format(row.startTime) + " – " + sdfTime.format(row.endTime)
                                } else ""
                                Text(time)
                                Text(row.categoryName)
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
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                OutlinedButton(onClick = { vm.deleteExpense(row.id) }) { Text("Delete") }
                            }
                        }
                    }
                }
            }
        }
    }
}


