package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.AppPreferences
import com.example.data.local.PoreyDatabase
import com.example.data.repository.PoreyRepository
import com.example.ui.PoreyApp
import com.example.ui.state.PoreyViewModel
import com.example.ui.theme.PoreyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val db = PoreyDatabase.getInstance(applicationContext)
        val repository = PoreyRepository(db)
        val appPreferences = AppPreferences(applicationContext)

        setContent {
            val poreyViewModel: PoreyViewModel = viewModel(
                factory = PoreyViewModel.Factory(repository, appPreferences)
            )
            val uiState by poreyViewModel.uiState.collectAsStateWithLifecycle()

            PoreyTheme(darkTheme = uiState.isDarkMode) {
                PoreyApp(viewModel = poreyViewModel)
            }
        }
    }
}
