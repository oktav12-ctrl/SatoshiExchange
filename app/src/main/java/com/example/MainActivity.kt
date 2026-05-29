package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AppDatabase
import com.example.data.TaskRepository
import com.example.network.TodoPlaceholderApi
import com.example.ui.TaskApp
import com.example.ui.TaskViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize singletons & MVVM architecture blocks
        val database = AppDatabase.getDatabase(applicationContext)
        val api = TodoPlaceholderApi.create()
        val repository = TaskRepository(database.taskDao(), api)
        val viewModelFactory = TaskViewModel.Factory(repository)

        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // Main layout
                    val viewModel: TaskViewModel = viewModel(factory = viewModelFactory)
                    TaskApp(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
