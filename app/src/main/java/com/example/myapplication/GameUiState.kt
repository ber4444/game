package com.example.myapplication

import androidx.compose.runtime.Immutable

@Immutable
data class GameUiState(
    val turn: Set = Set.WHITE,

    // White team's Pieces
    val piecesWhite: List<Piece> = listOf(
        Rook(Set.WHITE), Knight(Set.WHITE), Bishop(Set.WHITE), Queen(Set.WHITE),
        King(Set.WHITE), Bishop(Set.WHITE), Knight(Set.WHITE), Rook(Set.WHITE)
    ), // + List(8) { Pawn(Set.WHITE) },

    // Where all the White Pieces are
    // Usage: positionsWhite[pieceIndex][0 = vertical/ 1 = horizontal]
    // TODO [CLEANUP]: Rewrite as List<Pair<Int, Int>> to match Piece directions, might need to adjust tests to accommodate type change
    val positionsWhite: List<List<Int>> = List(8) { listOf(7, it) }, // + List(8) { listOf(6, it) },

    // Black team's pieces
    val piecesBlack: List<Piece> = listOf(
        Rook(Set.BLACK), Knight(Set.BLACK), Bishop(Set.BLACK), Queen(Set.BLACK),
        King(Set.BLACK), Bishop(Set.BLACK), Knight(Set.BLACK), Rook(Set.BLACK)
    ), // + List(8) { Pawn(Set.BLACK) },

    // Where all the Black Pieces are
    val positionsBlack: List<List<Int>> = List(8) { listOf(0, it) }, // + List(8) { listOf(1, it) },

    // TODO [CLEANUP]: Remove gameEnded and instead just use winner (rename to gameWinState)
    // The end conditions for the game
    val gameEnded: Boolean = false,
    val winner: WinState = WinState.NONE,

    // If the 'Move' button is locked
    val buttonLock: Boolean = false,

    // If the game is in 'AutoPlay' mode
    val autoPlay : Boolean = false
)

// Current win state of the game
enum class WinState {
    NONE, // The game has not been won
    WHITE, // White won the game
    BLACK, // Black won the game
    STALEMATE // The game is over, but there is no winner
}