package com.example.myapplication

// TODO [CLEANUP]: Rename to viewState to clarify more than Piece animation is being tracked
data class PieceAnimationState (
    val pieceToAnimate: Piece? = null,
    val pieceIndex: Int = 0,
    val animatePositionStart: List<Int> = listOf(0,0),
    val animatePositionEnd: List<Int> = listOf(0,0),

    val hideWindow: Boolean = false
)