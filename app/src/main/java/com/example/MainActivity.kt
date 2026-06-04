package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.ui.DashboardScreen
import com.example.ui.LoginScreen
import com.example.ui.MainViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val isLight by viewModel.isLightMode.collectAsState()
            val currentUser by viewModel.currentUser.collectAsState()

            MyApplicationTheme(darkTheme = !isLight, dynamicColor = false) {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (currentUser == null) {
                        LoginScreen(viewModel = viewModel)
                    } else {
                        DashboardScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }
}

