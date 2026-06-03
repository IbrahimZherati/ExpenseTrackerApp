package com.example.expensetrackerapp.Components

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
import com.example.expensetrackerapp.data.AppDatabase
import com.example.expensetrackerapp.data.Expense
import kotlinx.coroutines.launch

@Composable
fun Steper() {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }
    val dao = db.expenseDao()
    val scope = rememberCoroutineScope()
    var step by remember { mutableStateOf(1) }
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

    when(step) {
        1 -> {
            MainPage(
                expenses = expenses,
                totalAmount = totalAmount,  // ✅ Pass total amount
                onAddClick = { step = 2 },
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