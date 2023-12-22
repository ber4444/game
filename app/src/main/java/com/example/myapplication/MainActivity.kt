package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.theme.MyApplicationTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background) {
                    var gameState by remember { mutableStateOf(GameUiState()) }
                    val scrollState = rememberScrollState()
                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.verticalScroll(scrollState)) {
                        Board(gameState)
                        Button(
                            onClick = {
                                // TODO - will let the robot make its move:
                                //  set a new state to reflect a legal move
                            }) {
                            Text(text = "Move")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Square(isDarkSquare: Boolean, pieceModel: Piece) {
    Box(modifier = Modifier
        .size(40.dp)
        .background(color = if (isDarkSquare) Color(0xFFBCC0C0) else Color.White),
        contentAlignment = Alignment.Center
    ) {
        Piece(pieceModel)
    }
}

@Composable
fun Board(state: GameUiState) {
    Box {
        Column {
            repeat(8) { row ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    repeat(8) { column ->
                        // TODO: put some of the pieces in their correct initial position
                        Square((row + column) % 2 == 1, Pawn(Set.WHITE))
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
