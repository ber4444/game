package com.example.myapplication

import androidx.compose.ui.window.ComposeUIViewController
import com.example.myapplication.ui.theme.MyApplicationTheme

fun MainViewController() = ComposeUIViewController {
    MyApplicationTheme {
        GameScreen(viewModel = GameViewModel())
    }
}
