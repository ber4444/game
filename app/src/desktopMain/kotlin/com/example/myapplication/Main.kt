package com.example.myapplication

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.example.myapplication.ui.theme.MyApplicationTheme

fun main() = application {
    val viewModel = remember { GameViewModel() }
    DisposableEffect(Unit) {
        onDispose { viewModel.close() }
    }

    Window(
        onCloseRequest = ::exitApplication,
        title = "Chess"
    ) {
        MyApplicationTheme {
            ChessApp(viewModel = viewModel)
        }
    }
}
