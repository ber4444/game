package com.example.myapplication

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.window.CanvasBasedWindow
import com.example.myapplication.ui.theme.MyApplicationTheme

fun main() {
    CanvasBasedWindow(title = "Chess") {
        val viewModel = remember { GameViewModel() }
        DisposableEffect(Unit) {
            onDispose { viewModel.close() }
        }

        MyApplicationTheme {
            ChessApp(viewModel = viewModel)
        }
    }
}
