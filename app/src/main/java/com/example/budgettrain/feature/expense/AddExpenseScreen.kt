package com.example.budgettrain.feature.expense

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.io.File
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun AddExpenseScreen(onSaved: () -> Unit, onViewExpenses: () -> Unit, vm: ExpenseViewModel = viewModel()) {
    val context = LocalContext.current
    val cal = remember { Calendar.getInstance() }
    var dateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var startTime: Long? by remember { mutableStateOf(null) }
    var endTime: Long? by remember { mutableStateOf(null) }
    var description by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }
    var categoryName by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var pendingPhotoUri by remember { mutableStateOf<Uri?>(null) }

    val sdfDate = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val sdfTime = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val currency = remember { NumberFormat.getCurrencyInstance(Locale.getDefault()) }

    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        imageUri = uri
    }

    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            imageUri = pendingPhotoUri
        } else {
            pendingPhotoUri = null
        }
    }

    fun createImageUri(): Uri? {
        return try {
            val imagesDir = File(context.cacheDir, "images").apply { mkdirs() }
            val file = File.createTempFile("expense_", ".jpg", imagesDir)
            FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
        } catch (_: Throwable) { null }
    }

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
            Text("Add Expense", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        item {
            Spacer(modifier = Modifier.height(12.dp))
        }
        item {
            Card(
                elevation = CardDefaults.cardElevation(4.dp), 
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Date and Time Section
                    Text(
                        "Date & Time",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color(0xFF2196F3)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                val c = Calendar.getInstance().apply { timeInMillis = dateMillis }
                                DatePickerDialog(
                                    context,
                                    { _, y, m, d -> Calendar.getInstance().apply { set(y, m, d, 0, 0, 0); dateMillis = timeInMillis } },
                                    c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            },
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                            modifier = Modifier.weight(1f)
                        ) { Text("Date: ${sdfDate.format(dateMillis)}") }

                        Button(
                            onClick = {
                                TimePickerDialog(
                                    context,
                                    { _, h, min -> Calendar.getInstance().apply { set(0,0,0,h,min); startTime = timeInMillis } },
                                    12, 0, true
                                ).show()
                            },
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                            modifier = Modifier.weight(1f)
                        ) { Text("Start: ${startTime?.let { sdfTime.format(it) } ?: "--:--"}") }

                        Button(
                            onClick = {
                                TimePickerDialog(
                                    context,
                                    { _, h, min -> Calendar.getInstance().apply { set(0,0,0,h,min); endTime = timeInMillis } },
                                    13, 0, true
                                ).show()
                            },
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                            modifier = Modifier.weight(1f)
                        ) { Text("End: ${endTime?.let { sdfTime.format(it) } ?: "--:--"}") }
                    }

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = { amountStr = it.filter { ch -> ch.isDigit() || ch == '.' } },
                        label = { Text("Amount") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Category input (type to create or use existing)
                    Text(
                        "Category", 
                        style = MaterialTheme.typography.titleSmall,
                        color = Color(0xFF2196F3)
                    )
                    OutlinedTextField(
                        value = categoryName,
                        onValueChange = { categoryName = it },
                        label = { Text("Type category name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "Tip: typing a new name will create the category.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF757575)
                    )

                    // Photo Section
                    Text(
                        "Receipt Photo",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color(0xFF2196F3)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = { pickImage.launch("image/*") },
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFF9C27B0)),
                            modifier = Modifier.weight(1f)
                        ) { Text("Add Photo") }
                        Button(
                            onClick = {
                                val uri = createImageUri()
                                if (uri != null) {
                                    pendingPhotoUri = uri
                                    takePicture.launch(uri)
                                }
                            },
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFF9C27B0)),
                            modifier = Modifier.weight(1f)
                        ) { Text("Take Photo") }
                    }
                    if (imageUri != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                "✓ Photo Attached",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF4CAF50)
                            )
                        }
                    }

                    // Action Buttons Section
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = {
                                val amt = amountStr.toDoubleOrNull() ?: -1.0
                                vm.saveExpenseWithCategoryName(
                                    userId = 1L,
                                    categoryName = categoryName,
                                    fallbackCategoryId = null,
                                    amount = amt,
                                    date = dateMillis,
                                    startTime = startTime,
                                    endTime = endTime,
                                    description = description,
                                    imagePath = imageUri?.toString()
                                )
                                if (vm.state.value.error == null) onSaved()
                            },
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                            modifier = Modifier.weight(1f)
                        ) { Text("Save Expense") }
                        Button(
                            onClick = onViewExpenses,
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFF607D8B)),
                            modifier = Modifier.weight(1f)
                        ) { Text("View Expenses") }
                    }
                    vm.state.value.error?.let { 
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                it, 
                                color = Color(0xFFF44336),
                                style = MaterialTheme.typography.bodySmall
                            ) 
                        }
                    }
                }
            }
        }
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