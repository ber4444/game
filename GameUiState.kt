package com.example.myapplication

import androidx.compose.runtime.Immutable

// "alive" pieces are stored in piecesWhite or piecesBlack
// each piece's position is stored respectively in order in positionsWhite or positionsBlack
@Immutable
data class GameUiState(
    val piecesWhite: List<Piece> = listOf(King(Set.WHITE), Queen(Set.WHITE)),
    val positionsWhite: List<List<Int>> = listOf(listOf(0, 4), listOf(0, 3)),
    
    val piecesBlack: List<Piece> = listOf(King(Set.BLACK), Queen(Set.BLACK)),
    val positionsBlack: List<List<Int>> = listOf(listOf(7, 4), listOf(7, 3)),
    
    val gameEnded: Boolean = false,
    val winner: String? = null,

    val gameMode: GameMode? = GameMode.SLOW
    // TODO add "slow" and "fast" game modes - "slow" mode moves pieces one at a time,
    //  "fast" moves pieces by the maximum possible distance
)

enum class GameMode {
    SLOW, FAST
}
