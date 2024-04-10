package com.example.myapplication

import androidx.compose.runtime.Immutable

@Immutable
data class GameUiState(
    val piece: Piece = King(Set.BLACK),
    val positionBlack: List<Int> = listOf(7, 4),
    val positionWhite: List<Int> = listOf(0, 4)
)
