package com.example.myapplication

import kotlin.test.assertTrue
import kotlin.test.Test
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
        val whitePositions = listOf(Pair(0,0), Pair(0,1), Pair(1,0), Pair(1,1))
        val blackPieces = listOf(King(Set.BLACK))
        val blackPositions = listOf(Pair(7,7))

        val randomPosition = pickMoveRandom(
            enemyPositions = blackPositions,
            enemyPieces = blackPieces,
            allyPositions = whitePositions,
            allyPieces = whitePieces
        )

        val x = abs(randomPosition.first.first - whitePositions[0].first)
        val y = abs(randomPosition.first.second - whitePositions[0].second)

        val kingDidNotMove = when {
            x == 0 && y == 1 ||
                x == 1 && y == 1 ||
                x == 1 && y == 0 -> false
            else -> true
        }

        assertTrue(
            kingDidNotMove,
            "Distance traveled was $x,$y which is a King's distance and the King should have no moves"
        )
    }
}