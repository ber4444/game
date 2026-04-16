package com.example.myapplication

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.ViewModel
import com.example.myapplication.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private val holder: AndroidGameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        holder.gameViewModel.attachEngine(createStockfishEngine())

        setContent {
            MyApplicationTheme {
                ChessApp(viewModel = holder.gameViewModel)
            }
        }
    }

    private fun createStockfishEngine(): ChessEngine? {
        val engine = StockfishEngine(
            nativeLibraryDir = applicationInfo.nativeLibraryDir,
            filesDir = filesDir,
            assetManager = assets,
            supportedAbis = Build.SUPPORTED_ABIS
        )
        return if (engine.isAvailable() && engine.start()) {
            println("Stockfish engine initialized successfully")
            engine
        } else {
            println("Stockfish engine is unavailable")
            null
        }
    }
}

class AndroidGameViewModel : ViewModel() {
    val gameViewModel = GameViewModel()

    override fun onCleared() {
        super.onCleared()
        gameViewModel.close()
    }
}
