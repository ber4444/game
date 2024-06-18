package com.example.myapplication

import org.junit.Assert.assertTrue
import org.junit.Test

class GameViewModelTest {
    private val viewModel = GameViewModel()

    @Test
    fun `test movePieceWhite stays within bounds and does not overlap with Black piece`() {
        viewModel.movePieceWhite()
        val positionWhite = viewModel.gameState.value.positionWhite
        val positionBlack = viewModel.gameState.value.positionBlack

        assertTrue("White piece out of bounds", positionWhite[0] in 0..7 && positionWhite[1] in 0..7)
        assertTrue("Pieces overlap", positionWhite != positionBlack)
        assertTrue("White piece in threatened zone", positionWhite !in viewModel.calculateThreatenedZones(positionBlack))
    }

    @Test
    fun `test movePieceBlack stays within bounds and does not overlap with White piece`() {
        viewModel.movePieceBlack()
        val positionBlack = viewModel.gameState.value.positionBlack
        val positionWhite = viewModel.gameState.value.positionWhite

        assertTrue("Black piece out of bounds", positionBlack[0] in 0..7 && positionBlack[1] in 0..7)
        assertTrue("Pieces overlap", positionBlack != positionWhite)
        assertTrue("Black piece in threatened zone", positionBlack !in viewModel.calculateThreatenedZones(positionWhite))
    }

    @Test
    fun `test multiple moves to ensure pieces stay within bounds, do not overlap, and avoid threatened zones`() {
        repeat(10) {
            viewModel.movePieceWhite()
            viewModel.movePieceBlack()

            val positionWhite = viewModel.gameState.value.positionWhite
            val positionBlack = viewModel.gameState.value.positionBlack

            assertTrue("White piece out of bounds", positionWhite[0] in 0..7 && positionWhite[1] in 0..7)
            assertTrue("Black piece out of bounds", positionBlack[0] in 0..7 && positionBlack[1] in 0..7)
            assertTrue("Pieces overlap", positionWhite != positionBlack)
            assertTrue("White piece in threatened zone", positionWhite !in viewModel.calculateThreatenedZones(positionBlack))
            assertTrue("Black piece in threatened zone", positionBlack !in viewModel.calculateThreatenedZones(positionWhite))
        }
    }
}
