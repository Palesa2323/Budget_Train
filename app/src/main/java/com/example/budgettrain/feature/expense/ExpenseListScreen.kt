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

@Composable
fun ExpenseListScreen(vm: ExpenseViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    val currency = NumberFormat.getCurrencyInstance(Locale.getDefault())
    val sdfDate = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    val sdfTime = SimpleDateFormat("HH:mm", Locale.getDefault())

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("All Expenses", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.padding(6.dp))
        if (state.expenses.isEmpty()) {
            Text("No expenses logged yet.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.expenses) { row ->
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


