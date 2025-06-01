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

            assertTrue("White piece out of bounds", positionWhite[0] in 0..7 && positionWhite[1] in 0..7)
            assertTrue("Black piece out of bounds", positionBlack[0] in 0..7 && positionBlack[1] in 0..7)
            assertTrue("Pieces overlap", positionWhite != positionBlack)
        }
    }
}