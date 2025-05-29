package com.example.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

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

        Spacer(modifier = Modifier.padding(16.dp))

        Button(
            onClick = {
                viewModel.gameMover()
            },
            enabled = !gameState.gameEnded  // button is enabled only when game has not ended
        ) {
            Text(text = stringResource(R.string.move_button), style = MaterialTheme.typography.titleLarge)
        }
        
        if(gameState.gameEnded == true){
            Spacer(modifier = Modifier.padding(16.dp))
            Text(
                text = stringResource(R.string.game_end_message, gameState.winner ?: "Draw"),
                color = Color.Red,
                style = MaterialTheme.typography.titleLarge
            )
        }
    }
}


@Composable
fun RowScope.Square(isDarkSquare: Boolean, content: @Composable ()-> Unit) {
    Box(modifier = Modifier
        .weight(1f)
        .aspectRatio(1f)
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
                        Square((row + column) % 2 == 1) {
                        // display a piece on the board if it exists at the given row and column
                            // pair each white piece with its position
                            // find the first pair where the position matches the current row and column
                            // extract the piece from the pair if it exists
                            val pieceWhite = state.piecesWhite
                                .zip(state.positionsWhite)
                                .firstOrNull { it.second == listOf(row, column) }
                                ?.first
                            
                            val pieceBlack = state.piecesBlack
                                .zip(state.positionsBlack)
                                .firstOrNull{ it.second == listOf(row, column) }
                                ?.first
                            
                            pieceWhite?.let { Piece(pieceModel = it) } ?:
                            pieceBlack?.let { Piece(pieceModel = it) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Piece(pieceModel: Piece) { // TODO animate the piece movement
    Icon(
        painter = painterResource(id = pieceModel.asset),
        tint = Color.Unspecified,
        contentDescription = pieceModel.asset.toString()
    )
}