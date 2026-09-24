package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.ui.VisualizerApp
import com.example.ui.VisualizerViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: VisualizerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VisualizerApp(viewModel = viewModel)
        }
    }
}
