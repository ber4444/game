package com.example.myapplication

import androidx.compose.runtime.Immutable

@Immutable
data class GameUiState(
    val piecesWhite: List<Piece> = listOf(
        Rook(Set.WHITE), Knight(Set.WHITE), Bishop(Set.WHITE), King(Set.WHITE),
        Queen(Set.WHITE), Bishop(Set.WHITE), Knight(Set.WHITE), Rook(Set.WHITE)
    ) + List(8) { Pawn(Set.WHITE) },

    val positionsWhite: List<List<Int>> = List(8) { listOf(0, it) } + List(8) { listOf(1, it) },

    val piecesBlack: List<Piece> = listOf(
        Rook(Set.BLACK), Knight(Set.BLACK), Bishop(Set.BLACK), King(Set.BLACK),
        Queen(Set.BLACK), Bishop(Set.BLACK), Knight(Set.BLACK), Rook(Set.BLACK)
    ) + List(8) { Pawn(Set.BLACK) },

    val positionsBlack: List<List<Int>> = List(8) { listOf(7, it) } + List(8) { listOf(6, it) },

    val gameEnded: Boolean = false,
    val winner: String? = null,
)

