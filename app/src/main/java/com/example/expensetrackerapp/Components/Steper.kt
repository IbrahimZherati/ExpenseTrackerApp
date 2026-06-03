package com.example.expensetrackerapp.Components

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import com.example.expensetrackerapp.data.AppDatabase
import com.example.expensetrackerapp.data.Expense
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun Steper() {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }
    val dao = db.expenseDao()
    val scope = rememberCoroutineScope()
    var step by remember { mutableIntStateOf(1) }
    var isLoggedIn by remember { mutableStateOf(false) }
    var expenses by remember { mutableStateOf(listOf<Expense>()) }
    var editingExpense by remember { mutableStateOf<Expense?>(null) }
    var totalAmount by remember { mutableDoubleStateOf(0.0) }

    val calendar = remember { Calendar.getInstance() }
    var selectedYear by remember { mutableIntStateOf(calendar.get(Calendar.YEAR)) }
    var selectedMonth by remember { mutableIntStateOf(calendar.get(Calendar.MONTH) + 1) }

    fun getMonthBoundaries(year: Int, month: Int): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(year, month - 1, 1, 0, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startOfMonth = cal.timeInMillis
        cal.add(Calendar.MONTH, 1)
        val startOfNextMonth = cal.timeInMillis
        return Pair(startOfMonth, startOfNextMonth)
    }

    // Load expenses from database filtered by selected month
    LaunchedEffect(selectedYear, selectedMonth) {
        val (start, end) = getMonthBoundaries(selectedYear, selectedMonth)
        scope.launch {
            dao.getExpensesForMonth(start, end).collect { expenseList ->
                expenses = expenseList
                totalAmount = expenseList.sumOf { it.amount }
            }
        }
    }

    fun refreshTotal() {
        totalAmount = expenses.sumOf { it.amount }
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
                selectedMonth = selectedMonth,
                selectedYear = selectedYear,
                onPreviousMonth = {
                    if (selectedMonth == 1) {
                        selectedMonth = 12
                        selectedYear -= 1
                    } else {
                        selectedMonth -= 1
                    }
                },
                onNextMonth = {
                    if (selectedMonth == 12) {
                        selectedMonth = 1
                        selectedYear += 1
                    } else {
                        selectedMonth += 1
                    }
                },
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
                        refreshTotal()
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