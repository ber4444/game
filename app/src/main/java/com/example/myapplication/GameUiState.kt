package com.example.myapplication

import androidx.compose.runtime.Immutable

@Immutable
data class GameUiState(
    val turn: Set = Set.WHITE,

    // Team Pieces and their locations
    // Usage: positionsWhite[pieceIndex][0 = vertical/ 1 = horizontal]
    // TODO [CLEANUP]: Rewrite as List<Pair<Int, Int>> to match Piece directions,
    //  might need to adjust tests to accommodate type change

    // White team's Pieces and their positions
    val piecesWhite: List<Piece> = listOf(
        Rook(Set.WHITE), Knight(Set.WHITE), Bishop(Set.WHITE), Queen(Set.WHITE),
        King(Set.WHITE), Bishop(Set.WHITE), Knight(Set.WHITE), Rook(Set.WHITE)
    ), // + List(8) { Pawn(Set.WHITE) },
    val positionsWhite: List<List<Int>> = List(8) { listOf(7, it) }, // + List(8) { listOf(6, it) },

    // Black team's Pieces and their positions
    val piecesBlack: List<Piece> = listOf(
        Rook(Set.BLACK), Knight(Set.BLACK), Bishop(Set.BLACK), Queen(Set.BLACK),
        King(Set.BLACK), Bishop(Set.BLACK), Knight(Set.BLACK), Rook(Set.BLACK)
    ), // + List(8) { Pawn(Set.BLACK) },
    val positionsBlack: List<List<Int>> = List(8) { listOf(0, it) }, // + List(8) { listOf(1, it) },

    val winState: WinState = WinState.NONE, // The current WinState of the game
    val autoPlay : Boolean = false,         // If the game is in autoplay mode

    // TODO [CLEANUP]: move to animState/viewState
    val buttonLock : Boolean = false,       // Lock all game modifying buttons (doesn't include reset/exit)
    val moveButtonLock: Boolean = false,    // If the 'Move' button is locked
)

// Current win state of the game
enum class WinState {
    NONE,       // The game has not been won
    WHITE,      // White won the game
    BLACK,      // Black won the game
    DRAW,       // THe game is over, but there is no winner
    STALEMATE   // The game is over because no more moves can be made by a Player (no winner)
}