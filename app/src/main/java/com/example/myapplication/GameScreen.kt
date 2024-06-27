package com.example.myapplication

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
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
            },
            enabled = !gameState.gameEnded  // button is enabled only when game has not ended
        ) {
            Text(text = stringResource(R.string.move_button))
        }
        
        if(gameState.gameEnded == true){
            Text(
                text = stringResource(R.string.game_end_message, gameState.winner ?: "No one")
            )
        }
    }
}


@Composable
fun Square(isDarkSquare: Boolean, windowSize: WindowWidthSizeClass, content: @Composable ()-> Unit) {
    Box(modifier = Modifier
        .size(
            when (windowSize) {
                WindowWidthSizeClass.Expanded -> 60.dp
                WindowWidthSizeClass.Medium -> 50.dp
                else -> 40.dp
            }
        )
        .background(color = if (isDarkSquare) MaterialTheme.colorScheme.secondary
            else Color.White),
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
                            val pieceWhite = state.piecesWhite
                                .zip(state.positionsWhite)
                                .firstOrNull { it.second == listOf(row, column) }
                                ?.first
                            
                            val pieceBlack = state.piecesBlack
                                .zip(state.positionsBlack)
                                .firstOrNull{ it.second == listOf(row, column) }
                                ?.first
                            
                            pieceWhite?.let { Piece(pieceModel = it, windowSize) } ?: pieceBlack?.let { Piece(pieceModel = it, windowSize) }
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