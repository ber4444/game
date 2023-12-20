package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Board()
                }
            }
        }
    }
}

@Composable
fun Square(isDarkSquare: Boolean) {
    Box(modifier = Modifier
        .size(40.dp)
        .background(color = if (isDarkSquare) Color(0xFFBCC0C0) else Color.White)
    )
}

@Composable
fun Board() {
    Box(Modifier.fillMaxSize()) {
        Column {
            repeat(8) { row ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    repeat(8) { column ->
                        Square((row + column) % 2 == 1)
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
        fontSize = 28.sp
    )
}
