package com.example.expensetrackerapp.Components

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.expensetrackerapp.data.AppDatabase
import com.example.expensetrackerapp.data.Expense
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun Steper() {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }
    val dao = db.expenseDao()
    val scope = rememberCoroutineScope()
    var step by remember { mutableStateOf(1) }
    var isLoggedIn by remember { mutableStateOf(false) }
    var expenses by remember { mutableStateOf(listOf<Expense>()) }
    var editingExpense by remember { mutableStateOf<Expense?>(null) }
    var totalAmount by remember { mutableStateOf(0.0) }

    // Load expenses from database
    LaunchedEffect(Unit) {
        scope.launch {
            dao.getAllExpenses().collect { expenseList ->
                expenses = expenseList
            }
        }
    }

    // Load total amount
    LaunchedEffect(Unit) {
        scope.launch {
            totalAmount = dao.getTotalAmount()
        }
    }

    // Refresh total after add/update/delete
    fun refreshTotal() {
        scope.launch {
            totalAmount = dao.getTotalAmount()
        }
    }

    suspend fun exportToCsv(context: android.content.Context, expenses: List<Expense>) {
        withContext(Dispatchers.IO) {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val csv = buildString {
                appendLine("id,description,amount,category,date")
                expenses.forEach { e ->
                    appendLine("${e.id},${e.description},${e.amount},${e.category},${dateFormat.format(Date(e.date))}")
                }
            }
            val file = File(context.cacheDir, "expenses.csv")
            file.writeText("\uFEFF$csv", Charsets.UTF_8)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Export Expenses"))
        }
    }

    if (!isLoggedIn) {
        LoginPage(onLoginSuccess = { isLoggedIn = true })
    } else when(step) {
            1 -> {
            MainPage(
                expenses = expenses,
                totalAmount = totalAmount,
                onAddClick = { step = 2 },
                onExportClick = {
                    scope.launch {
                        exportToCsv(context, expenses)
                    }
                },
                onEditClick = { expense ->
                    editingExpense = expense
                    step = 3
                },
                onDeleteClick = { expense ->
                    scope.launch {
                        dao.deleteExpense(expense)
                        refreshTotal()  // ✅ Refresh total after delete
                    }
                }
            )
        }
        2 -> {
            AddItem(
                onSave = { newExpense ->
                    scope.launch {
                        dao.insert(newExpense)
                        refreshTotal()  // ✅ Refresh total after insert
                        step = 1
                    }
                },
                onCancel = { step = 1 }
            )
        }
        3 -> {
            editingExpense?.let { expense ->
                UpdateItem(
                    expense = expense,
                    onUpdate = { updatedExpense ->
                        scope.launch {
                            dao.updateExpense(updatedExpense)
                            refreshTotal()  // ✅ Refresh total after update
                            editingExpense = null
                            step = 1
                        }
                    },
                    onCancel = {
                        editingExpense = null
                        step = 1
                    }
                )
            }
        }
    }
}