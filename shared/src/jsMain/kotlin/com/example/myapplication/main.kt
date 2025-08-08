package com.example.myapplication

import androidx.compose.ui.window.Window
import org.jetbrains.skiko.wasm.onWasmReady
import com.example.myapplication.ui.theme.MyApplicationTheme

fun main() {
    onWasmReady {
        Window("Chess") {
            MyApplicationTheme {
                GameScreen(viewModel = GameViewModel())
            }
        }
    }
}
