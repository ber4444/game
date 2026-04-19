package com.example.myapplication

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document
import com.example.myapplication.ui.theme.MyApplicationTheme
import androidx.compose.ui.ExperimentalComposeUiApi

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    document.title = "Chess"
    ComposeViewport("ComposeTarget") {
        val viewModel = remember { GameViewModel() }
        DisposableEffect(Unit) {
            onDispose { viewModel.close() }
        }

        MyApplicationTheme(darkTheme = false) {
            ChessApp(viewModel = viewModel)
        }
    }
}
