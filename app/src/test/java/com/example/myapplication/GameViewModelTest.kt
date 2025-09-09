package com.example.myapplication

import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test

class GameViewModelTest {
    private val viewModel = GameViewModel()

    @Test
    fun `test movePieceWhite within bounds and no overlap`() {
        viewModel.move(Set.WHITE)
        val positionWhite = viewModel.gameState.value.positionsWhite.first()
        val positionBlack = viewModel.gameState.value.positionsBlack.first()

        assertTrue("White piece out of bounds", positionWhite[0] in 0..7 && positionWhite[1] in 0..7)
        assertTrue("Pieces overlap", positionWhite != positionBlack)
    }

    @Test
    fun `test movePieceBlack within bounds and no overlap`() {
        viewModel.move(Set.BLACK)
        val positionBlack = viewModel.gameState.value.positionsBlack.first()
        val positionWhite = viewModel.gameState.value.positionsWhite.first()

        assertTrue("Black piece out of bounds", positionBlack[0] in 0..7 && positionBlack[1] in 0..7)
        assertTrue("Pieces overlap", positionBlack != positionWhite)
    }

    @Test
    fun `play until game over and ensure no overlap`() {
        while(! viewModel.gameState.value.gameEnded) {
            viewModel.move(Set.WHITE)
            viewModel.move(Set.BLACK)

            val positionWhite = viewModel.gameState.value.positionsWhite.first()
            val positionBlack = viewModel.gameState.value.positionsBlack.first()

            assertTrue("White piece out of bounds at (${positionWhite[0]}, ${positionWhite[1]})", positionWhite[0] in 0..7 && positionWhite[1] in 0..7)
            assertTrue("Black piece out of bounds at (${positionBlack[0]}, ${positionBlack[1]})", positionBlack[0] in 0..7 && positionBlack[1] in 0..7)
            assertTrue("Pieces overlap at (${positionBlack[0]}, ${positionBlack[1]})", positionWhite != positionBlack)
        }
    }

    @Test
    fun `verify King is in a bad position`() {
        val kingPosition = listOf(3,3)
        val knightPositions = listOf(
            listOf(1,2),listOf(1,4),listOf(2,1),listOf(2,5),
            listOf(4,1),listOf(4,5),listOf(5,2),listOf(5,4)
        )
        val queenPositions = listOf(
            listOf(1,1),listOf(1,3),listOf(1,5),listOf(3,1),
            listOf(3,7),listOf(5,1),listOf(3,6),listOf(7,7)
        )

        val killingGameState = GameUiState(
            positionsBlack = knightPositions + queenPositions,
            positionsWhite = listOf(kingPosition),
            piecesBlack = listOf(
                Knight(Set.BLACK),Knight(Set.BLACK),Knight(Set.BLACK),Knight(Set.BLACK),
                Knight(Set.BLACK),Knight(Set.BLACK),Knight(Set.BLACK),Knight(Set.BLACK),
                Queen(Set.BLACK),Queen(Set.BLACK),Queen(Set.BLACK),Queen(Set.BLACK),
                Queen(Set.BLACK),Queen(Set.BLACK),Queen(Set.BLACK),Queen(Set.BLACK),
            ),
            piecesWhite = listOf(King(Set.WHITE))
        )

        assertTrue(
            (killingGameState.piecesWhite.first() as King).amIDead(
                position = Pair(3,3),
                enemyPositions = killingGameState.positionsBlack,
                enemyPieces = killingGameState.piecesBlack,
                allyPositions = killingGameState.positionsWhite
            )
        )
    }

    @Test
    fun `the King is not safe from Knights`() {
        val kingPositionPair = Pair(3,3)
        val knightPositions = listOf(
            listOf(1,2),listOf(1,4),listOf(2,1),listOf(2,5),
            listOf(4,1),listOf(4,5),listOf(5,2),listOf(5,4)
        )
        val whitePositions = listOf(
            listOf(1,3),listOf(2,2),listOf(2,3),listOf(2,4),
            listOf(3,1),listOf(3,2),listOf(3,4),listOf(3,5),
            listOf(4,2),listOf(4,3),listOf(4,4),listOf(5,3)
        )

        val killingGameState = GameUiState(
            positionsBlack = knightPositions,
            positionsWhite = whitePositions,
            piecesBlack = listOf(
                Knight(Set.BLACK),Knight(Set.BLACK),Knight(Set.BLACK),Knight(Set.BLACK),
                Knight(Set.BLACK),Knight(Set.BLACK),Knight(Set.BLACK),Knight(Set.BLACK)
            ),
            piecesWhite = listOf(King(Set.WHITE)) // we don't care about pieces, just locations
        )

        assertTrue(
            "Knights should be able to capture King even when allies are in between",
            (killingGameState.piecesWhite.first() as King).amIDead(
                position = kingPositionPair,
                enemyPositions = killingGameState.positionsBlack,
                enemyPieces = killingGameState.piecesBlack,
                allyPositions = killingGameState.positionsWhite
            )
        )
    }

    @Test
    fun `the King is safe with allies blocking`() {
        val kingPairPosition = Pair(3,3)
        val whitePositions = listOf(
            listOf(2,2),listOf(2,3),listOf(2,4),
            listOf(3,2),listOf(3,4),
            listOf(4,2),listOf(4,3),listOf(4,4)
        )
        val queenPositions = listOf(
            listOf(1,1),listOf(1,3),listOf(1,5),listOf(3,1),
            listOf(3,7),listOf(5,1),listOf(3,6),listOf(7,7)
        )

        val killingGameState = GameUiState(
            positionsBlack = queenPositions,
            positionsWhite = whitePositions,
            piecesBlack = listOf(
                Queen(Set.BLACK),Queen(Set.BLACK),Queen(Set.BLACK),Queen(Set.BLACK),
                Queen(Set.BLACK),Queen(Set.BLACK),Queen(Set.BLACK),Queen(Set.BLACK),
            ),
            piecesWhite = listOf(King(Set.WHITE)) // we don't care about pieces, just locations
        )

        val kingIsDead = (killingGameState.piecesWhite.first() as King).amIDead(
            position = kingPairPosition,
            enemyPositions = killingGameState.positionsBlack,
            enemyPieces = killingGameState.piecesBlack,
            allyPositions = killingGameState.positionsWhite
        )

        assertTrue(
            "Queens should be blocked by King's allies",
            !kingIsDead
        )
    }
}