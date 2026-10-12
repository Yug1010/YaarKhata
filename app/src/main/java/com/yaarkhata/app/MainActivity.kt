package com.yaarkhata.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.yaarkhata.app.data.AppDatabase
import com.yaarkhata.app.repository.FinanceRepository
import com.yaarkhata.app.ui.navigation.AppNavHost
import com.yaarkhata.app.ui.theme.FinanceTrackerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val database = AppDatabase.getInstance(applicationContext)
        val repository = FinanceRepository(database)

        setContent {
            FinanceTrackerTheme {
                AppNavHost(repository = repository)
            }
        }
    }
}
