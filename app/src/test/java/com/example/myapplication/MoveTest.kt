package com.example.myapplication

import junit.framework.TestCase.assertTrue
import org.junit.Test
import kotlin.math.abs

class MoveTest {

    /**
    * This isn't apparent. We've forced the first piece to be moved to be the King, but we surround
    * the King with Rooks so it can't move. When randomMove() is called, it will try to get a
    * position for the King and fail, so it should move on to a Rook. We can confirm it by
    * subtracting the King's position from the randomPosition returned. If the answer
    * is not 0,1/1,1/1,0 in some form, it wasn't a King that moved!
     **/
    @Test
    fun `randomMove returns a move even if the first piece has none`() {
        val whitePieces = listOf(King(Set.WHITE), Rook(Set.WHITE), Rook(Set.WHITE), Rook(Set.WHITE))
        val whitePositions = listOf(listOf(0,0), listOf(0,1), listOf(1,0), listOf(1,1))
        val blackPieces = listOf(King(Set.BLACK))
        val blackPositions = listOf(listOf(7,7))

        val randomPosition = pickMoveRandom(
            turn = Set.WHITE,
            enemyPositions = blackPositions,
            enemyPieces = blackPieces,
            allyPositions = whitePositions,
            allyPieces = whitePieces
        )

        val x = abs(randomPosition.first[0] - whitePositions[0][0])
        val y = abs(randomPosition.first[1] - whitePositions[0][1])

        val kingDidNotMove = when {
            x == 0 && y == 1 ||
                x == 1 && y == 1 ||
                x == 1 && y == 0 -> false
            else -> true
        }

        assertTrue(
            "Distance traveled was $x,$y which is a King's distance and the King should have no moves",
            kingDidNotMove
        )
    }

    // TODO [TEST]: Write more tests
    // TODO [CLEANUP]: Rename move() to turn()? Clarify that things other than Piece movement are occurring
    // - When there are no available moves, move() does not alter Piece positions, game over is updated
    // - When in Check, move() does not alter Piece positions, game over is updated
    // - When in Check, smarterRandom() prioritizes a move to escape Check
}