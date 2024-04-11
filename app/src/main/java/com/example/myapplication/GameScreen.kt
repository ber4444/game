package com.example.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun GameScreen(
    windowSize: WindowWidthSizeClass,
    viewModel: GameViewModel
){
    val gameState by viewModel.gameState.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.verticalScroll(scrollState)
    ) {
        Board(gameState, windowSize)
        Button(
            onClick = {
                viewModel.gameMover()
            }
        ) {
            Text(text = "Move")
        }
    }
}


@Composable
fun Square(isDarkSquare: Boolean, windowSize: WindowWidthSizeClass, content: @Composable ()-> Unit) {
    Box(modifier = Modifier
        .size(
            when (windowSize){
                WindowWidthSizeClass.Expanded -> 60.dp
                WindowWidthSizeClass.Medium -> 50.dp
                else -> 40.dp
            }
        )
        .background(color = if (isDarkSquare) Color(0xFFBCC0C0) else Color.White),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
fun Board(state: GameUiState, windowSize: WindowWidthSizeClass) {
    Box(
        modifier = Modifier
            .padding(
                when (windowSize){
                    WindowWidthSizeClass.Expanded -> 18.dp
                    WindowWidthSizeClass.Medium -> 12.dp
                    else -> 8.dp
                }
            )
    ) {
        Column {
            repeat(8) { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(8) { column ->
                        Square((row + column) % 2 == 1, windowSize) {
                            when {
                                (row == state.positionBlack.first() && column == state.positionBlack.last()) ->
                                    Piece(pieceModel = King(Set.BLACK), windowSize)
                                (row == state.positionWhite.first() && column == state.positionWhite.last()) ->
                                    Piece(pieceModel = King(Set.WHITE), windowSize)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Piece(pieceModel: Piece, windowSize: WindowWidthSizeClass) {
    Text(
        text = pieceModel.symbol,
        fontSize = (
                when (windowSize) {
                    WindowWidthSizeClass.Expanded -> 48.sp
                    WindowWidthSizeClass.Medium -> 38.sp
                    else -> 28.sp
                })
    )
}