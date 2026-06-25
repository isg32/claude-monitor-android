package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.example.data.AppDatabase
import com.example.data.UsageRepository
import com.example.ui.MonitorScreen
import com.example.ui.MonitorViewModel
import com.example.ui.MonitorViewModelFactory
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Room persistence database & repository
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = UsageRepository(database.usageDao())

        // Obtain Viewmodel via Factory
        val viewModel: MonitorViewModel by viewModels {
            MonitorViewModelFactory(repository)
        }

        setContent {
            MyApplicationTheme {
                MonitorScreen(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
