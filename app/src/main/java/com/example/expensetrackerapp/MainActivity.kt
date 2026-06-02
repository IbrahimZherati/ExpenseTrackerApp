package com.example.expensetrackerapp  // ← Changed from "expensetracker"

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.expensetrackerapp.data.AppDatabase  // ← Fixed
import com.example.expensetrackerapp.data.Expense      // ← Fixed
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ExpenseScreen()
        }
    }
}

@Composable
fun ExpenseScreen() {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }
    val dao = db.expenseDao()

    var expenses by remember { mutableStateOf<List<Expense>>(emptyList()) }
    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        expenses = dao.getAllExpenses()

        if (expenses.isEmpty()) {
            dao.insert(Expense(description = "Coffee", amount = 4.50, category = "Food", date = System.currentTimeMillis()))
            dao.insert(Expense(description = "Movie", amount = 15.00, category = "Entertainment", date = System.currentTimeMillis()))
            dao.insert(Expense(description = "Groceries", amount = 45.00, category = "Food", date = System.currentTimeMillis()))
            expenses = dao.getAllExpenses()
        }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        TextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description") }
        )

        TextField(
            value = amount,
            onValueChange = { amount = it },
            label = { Text("Amount") }
        )

        TextField(
            value = category,
            onValueChange = { category = it },
            label = { Text("Category") }
        )

        Button(onClick = {
            scope.launch {
                val amountValue = amount.toDoubleOrNull() ?: 0.0
                dao.insert(Expense(
                    description = description,
                    amount = amountValue,
                    category = category,
                    date = System.currentTimeMillis()
                ))
                expenses = dao.getAllExpenses()
                description = ""
                amount = ""
                category = ""
            }
        }) {
            Text("Add Expense")
        }

        Spacer(modifier = Modifier.height(16.dp))

        expenses.forEach { expense ->
            Text(text = "${expense.description}: $${expense.amount} (${expense.category})")
        }
    }
}