package com.example.myapplication

data class PieceAnimationState (
    val pieceToAnimate: Piece? = null,
    val pieceIndex: Int = -1,
    val animatePositionStart: Pair<Int, Int> = INVALID_POSITION,
    val animatePositionEnd: Pair<Int, Int> = INVALID_POSITION
) {
    // Ensure all values are valid before animating
    fun moveIsValid() : Boolean {
        return pieceToAnimate != null &&
                pieceIndex != -1 &&
                animatePositionStart!= INVALID_POSITION &&
                animatePositionEnd != INVALID_POSITION
    }
}