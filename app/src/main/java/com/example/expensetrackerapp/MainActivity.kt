package com.example.expensetrackerapp  // ← Changed from "expensetracker"

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.example.expensetrackerapp.Components.ExpenseItem
import com.example.expensetrackerapp.Components.Steper
import com.example.expensetrackerapp.data.AppDatabase  // ← Fixed
import com.example.expensetrackerapp.data.Expense      // ← Fixed
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()

        // ✅ Method 2: Make status bar transparent and content behind it
        window.statusBarColor = android.graphics.Color.TRANSPARENT

        // ✅ Method 3: Hide status bar completely (content goes full screen)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
        super.onCreate(savedInstanceState)
        setContent {
            Steper()
        }
    }
}

