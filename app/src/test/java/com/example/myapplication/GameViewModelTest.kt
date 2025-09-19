package com.example.myapplication

import org.junit.Assert.assertTrue
import org.junit.Test

class GameViewModelTest {
    private val viewModel = GameViewModel()

    @Test
    fun `test movePieceWhite within bounds and no overlap`() {
        viewModel.moveCPU(Set.WHITE)  {
            enemyPositions: List<Pair<Int, Int>>,
            enemyPieces: List<Piece>,
            allyPositions: List<Pair<Int,Int>>,
            allyPieces: List<Piece> ->
            pickMoveRandom(enemyPositions, enemyPieces, allyPositions, allyPieces)
        }
        val positionWhite = viewModel.gameState.value.positionsWhite.first()
        val positionBlack = viewModel.gameState.value.positionsBlack.first()

        assertTrue("White piece out of bounds", positionWhite.first in 0 until BOARD_SIZE && positionWhite.second in 0 until BOARD_SIZE)
        assertTrue("Pieces overlap", positionWhite != positionBlack)
    }

    @Test
    fun `test movePieceBlack within bounds and no overlap`() {
        viewModel.moveCPU(Set.BLACK) {
            enemyPositions: List<Pair<Int, Int>>,
            enemyPieces: List<Piece>,
            allyPositions: List<Pair<Int, Int>>,
            allyPieces: List<Piece> ->
            pickMoveRandom(enemyPositions, enemyPieces, allyPositions, allyPieces)
        }
        val positionBlack = viewModel.gameState.value.positionsBlack.first()
        val positionWhite = viewModel.gameState.value.positionsWhite.first()

        assertTrue("Black piece out of bounds", positionBlack.first in 0 until BOARD_SIZE && positionBlack.second in 0 until BOARD_SIZE)
        assertTrue("Pieces overlap", positionBlack != positionWhite)
    }

    @Test
    fun `play until game over and ensure no overlap`() {
        while(viewModel.gameState.value.winState == WinState.NONE) {
            viewModel.moveCPU(Set.WHITE) {
                enemyPositions: List<Pair<Int, Int>>,
                enemyPieces: List<Piece>,
                allyPositions: List<Pair<Int, Int>>,
                allyPieces: List<Piece> ->
                pickMoveRandom(enemyPositions, enemyPieces, allyPositions, allyPieces)
            }
            viewModel.moveCPU(Set.BLACK) {
                enemyPositions: List<Pair<Int, Int>>,
                enemyPieces: List<Piece>,
                allyPositions: List<Pair<Int, Int>>,
                allyPieces: List<Piece> ->
                pickMoveRandom(enemyPositions, enemyPieces, allyPositions, allyPieces)
            }

            val positionWhite = viewModel.gameState.value.positionsWhite.first()
            val positionBlack = viewModel.gameState.value.positionsBlack.first()

            assertTrue("White piece out of bounds", positionWhite.first in 0 until BOARD_SIZE && positionWhite.second in 0 until BOARD_SIZE)
            assertTrue("Black piece out of bounds", positionBlack.first in 0 until BOARD_SIZE && positionBlack.second in 0 until BOARD_SIZE)
            assertTrue("Pieces overlap", positionWhite != positionBlack)
        }
    }

    @Test
    fun `verify King is in a bad position`() {
        val kingPosition = Pair(3,3)
        val knightPositions = listOf(
            Pair(1,2),Pair(1,4),Pair(2,1),Pair(2,5),
            Pair(4,1),Pair(4,5),Pair(5,2),Pair(5,4)
        )
        val queenPositions = listOf(
            Pair(1,1),Pair(1,3),Pair(1,5),Pair(3,1),
            Pair(3,7),Pair(5,1),Pair(3,6),Pair(7,7)
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
            checkCheck(
                kingPosition = Pair(3,3),
                enemyPositions = killingGameState.positionsBlack,
                enemyPieces = killingGameState.piecesBlack,
                allyPositions = killingGameState.positionsWhite
            )
        )
    }

    @Test
    fun `the King is not safe from Knights`() {
        val kingPosition = Pair(3,3)
        val knightPositions = listOf(
            Pair(1,2),Pair(1,4),Pair(2,1),Pair(2,5),
            Pair(4,1),Pair(4,5),Pair(5,2),Pair(5,4)
        )
        val whitePositions = listOf(
            Pair(1,3),Pair(2,2),Pair(2,3),Pair(2,4),
            Pair(3,1),Pair(3,2),Pair(3,4),Pair(3,5),
            Pair(4,2),Pair(4,3),Pair(4,4),Pair(5,3)
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
            checkCheck(
                kingPosition = kingPosition,
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
            Pair(2,2),Pair(2,3),Pair(2,4),
            Pair(3,2),Pair(3,4),
            Pair(4,2),Pair(4,3),Pair(4,4)
        )
        val queenPositions = listOf(
            Pair(1,1),Pair(1,3),Pair(1,5),Pair(3,1),
            Pair(3,7),Pair(5,1),Pair(3,6),Pair(7,7)
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

        val kingIsDead = checkCheck(
            kingPosition = kingPairPosition,
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