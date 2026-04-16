package com.example.myapplication

import android.os.Bundle
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.myapplication.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val viewModel: GameViewModel by viewModels()

        // Initialize Stockfish engine if available (falls back to built-in AI otherwise)
        viewModel.initStockfish(
            nativeLibraryDir = applicationInfo.nativeLibraryDir,
            filesDir = filesDir,
            assetManager = assets,
            supportedAbis = Build.SUPPORTED_ABIS
        )

        setContent {
            MyApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background) {
                    val windowSize = calculateWindowSizeClass(activity = this)
                    GameScreen(windowSize = windowSize.widthSizeClass, viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
@Preview(showBackground = true, widthDp = 700, heightDp = 1000)
fun GameMediumPreview() {
    MyApplicationTheme {
        GameScreen(windowSize = WindowWidthSizeClass.Medium, GameViewModel())
    }
}

@Composable
@Preview(showBackground = true, widthDp = 1000, heightDp = 1300)
fun GameLargePreview() {
    MyApplicationTheme {
        GameScreen(windowSize = WindowWidthSizeClass.Expanded, viewModel = GameViewModel())
    }
}

@Composable
@Preview(showBackground = true)
fun GamePreview() {
    MyApplicationTheme {
        GameScreen(windowSize = WindowWidthSizeClass.Compact, viewModel = GameViewModel())
    }
}

