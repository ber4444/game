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
    val positionsWhite: List<List<Int>> = List(8) { listOf(7, it) }, // + List(8) { listOf(6, it) },

    // Black team's pieces
    val piecesBlack: List<Piece> = listOf(
        Rook(Set.BLACK), Knight(Set.BLACK), Bishop(Set.BLACK), Queen(Set.BLACK),
        King(Set.BLACK), Bishop(Set.BLACK), Knight(Set.BLACK), Rook(Set.BLACK)
    ), // + List(8) { Pawn(Set.BLACK) },

    // Where all the Black Pieces are
    val positionsBlack: List<List<Int>> = List(8) { listOf(0, it) }, // + List(8) { listOf(1, it) },

    val gameEnded: Boolean = false,
    val winner: WinState = WinState.NONE,

    val buttonLock: Boolean = false
)

// Current win state of the game
enum class WinState {
    NONE,
    WHITE,
    BLACK,
    STALEMATE
}