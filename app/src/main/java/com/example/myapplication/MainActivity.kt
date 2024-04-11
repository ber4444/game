package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val viewModel: GameViewModel by viewModels()

        setContent {
            MyApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background) {
                    val gameState by viewModel.gameState.collectAsState()
                    val scrollState = rememberScrollState()

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.verticalScroll(scrollState)
                    ) {
                        Board(gameState)

                        Button(
                            onClick = {
                                viewModel.gameMover()
                            }
                        ) {
                            Text(text = "Move")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Square(isDarkSquare: Boolean, content: @Composable ()-> Unit) {
    Box(modifier = Modifier
        .size(40.dp)
        .background(color = if (isDarkSquare) Color(0xFFBCC0C0) else Color.White),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
fun Board(state: GameUiState) {
    Box {
        Column {
            repeat(8) { row ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    repeat(8) { column ->
                        Square((row + column) % 2 == 1) {
                            when {
                                (row == state.positionBlack.first() && column == state.positionBlack.last()) ->
                                    Piece(pieceModel = King(Set.BLACK))
                                (row == state.positionWhite.first() && column == state.positionWhite.last()) ->
                                    Piece(pieceModel = King(Set.WHITE))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Piece(pieceModel: Piece) {
    Text(
        text = pieceModel.symbol,
        fontSize = 28.sp,
    )
}

@Composable
@Preview(showBackground = true)
fun GamePreview() {
    MyApplicationTheme {
        Board(GameUiState())
    }
}
