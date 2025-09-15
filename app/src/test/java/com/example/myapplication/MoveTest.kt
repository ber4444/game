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
            "Distance traveled was $x,$y which is a King's distance and the King should have no moves",
            kingDidNotMove
        )
    }


    // TODO [TEST]: Write more tests
    // TODO [CLEANUP]: Rename move() to turn()? Clarify that things other than Piece movement are occurring
    // - When there are no available moves, move() does not alter Piece positions, game over is updated
    // - When in Check, move() does not alter Piece positions, game over is updated
    // - When in Check, smarterRandom() prioritizes a move to escape Check

    // TODO [TEST]: Finish test setup for CPU pickMoves
    /*@Test
    fun `CPU pickMoves prioritizes a move to escape Check` () {
        // Setup
        // White in Check, with 1 possible way to escape Check
        val turn : Set = Set.WHITE
        val blackPositions : List<List<Int>> = listOf()
        val blackPieces : List<Piece> = listOf()
        val whitePositions : List<List<Int>> = listOf()
        val whitePieces : List<Piece> =  listOf()

        val expectedMove : Pair<List<Int>, Int> = Pair(listOf(), 0)

        // Execute
        val pickedMove = pickMoveCPU(turn, blackPositions, blackPieces, whitePositions, whitePieces)

        // Verify
        assertTrue(pickedMove == expectedMove)
    }*/

    // TODO: Move to ViewModel/GameUIState test?
    /*
    @Test
    fun `Game over when Black in Check`() {
        // Setup
        val whitePieces = listOf(King(Set.WHITE), Rook(Set.WHITE), Rook(Set.WHITE), Rook(Set.WHITE))
        val whitePositions = listOf(listOf(0,0), listOf(0,1), listOf(1,0), listOf(1,1))
        val blackPieces = listOf(King(Set.BLACK))
        val blackPositions = listOf(listOf(7,7))

        // Create a GameUIState (expected) and GameUIState (actual)
        val initialState = GameUIState()
        var testState = GameUIState()

        // Execute
        // Receive updated gameState from move()
        testState = move()

        // Verify
        assertTrue(initialState.whitePositions == testState.whitePositions) // No movement of White or Black Pieces
        assertTrue(initialState.blackPositions == testState.blackPositions)
        assertTrue(testState.winState == WinState.WHITE)) // Game is over
    }*/
}