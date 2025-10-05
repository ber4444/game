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

    @Test
    fun `black King in check does not immediately end the game after playerMove`() {
        val blackKingPosition = Pair(0, 4)
        val whiteRookPosition = Pair(4, 0)
        val whiteKingPosition = Pair(7, 4)

        val initialState = GameUiState(
            turn = Set.WHITE,
            piecesWhite = listOf(Rook(Set.WHITE), King(Set.WHITE)),
            positionsWhite = listOf(whiteRookPosition, whiteKingPosition),
            inCheckWhite = false,
            piecesBlack = listOf(King(Set.BLACK)),
            positionsBlack = listOf(blackKingPosition),
            inCheckBlack = false,
            winState = WinState.NONE
        )

        val viewModel = GameViewModel(initialState)

        val newWhiteRookPosition = Pair(4, 4)

        viewModel.playerMove(
            selectedPieceIndex = 0,
            newPosition = newWhiteRookPosition
        )

        val gameState = viewModel.gameState.value

        assertTrue("Black should be in check after player move", gameState.inCheckBlack)
        assertTrue("Game should continue after King is in check", gameState.winState == WinState.NONE)
    }

    @Test
    fun `white King in check does not immediately end the game after moveCPU`() {
        val whiteKingPosition = Pair(7, 4)
        val blackBishopPosition = Pair(0, 5)
        val blackKingPosition = Pair(0, 4)

        val initialState = GameUiState(
            turn = Set.WHITE,
            piecesWhite = listOf(King(Set.WHITE)),
            positionsWhite = listOf(whiteKingPosition),
            inCheckWhite = false,
            piecesBlack = listOf(Bishop(Set.BLACK), King(Set.BLACK)),
            positionsBlack = listOf(blackBishopPosition, blackKingPosition),
            inCheckBlack = false,
            winState = WinState.NONE
        )

        val viewModel = GameViewModel(initialState)

        val newBlackBishopPosition = Pair(4, 1)

        viewModel.moveCPU(Set.BLACK) {
            enemyPositions: List<Pair<Int, Int>>,
            enemyPieces: List<Piece>,
            allyPositions: List<Pair<Int, Int>>,
            allyPieces: List<Piece> ->
            Pair(newBlackBishopPosition, 0)
        }

        val gameState = viewModel.gameState.value

        assertTrue("White should be in check after CPU move", gameState.inCheckWhite)
        assertTrue("Game should continue after King is in check", gameState.winState == WinState.NONE)
    }
}