package com.example.myapplication

data class PieceAnimationState (
    val pieceToAnimate: Piece? = null,
    val pieceIndex: Int = 0,
    val animatePositionStart: List<Int> = listOf(0,0),
    val animatePositionEnd: List<Int> = listOf(0,0),
)