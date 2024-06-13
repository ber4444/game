package com.example.myapplication

import org.junit.Assert.assertTrue
import org.junit.Test

class GameViewModelTest {
    private val viewModel = GameViewModel()

    @Test
    fun `test movePieceWhite within bounds and no overlap`() {
        viewModel.movePieceWhite()
        val positionWhite = viewModel.gameState.value.positionWhite
        val positionBlack = viewModel.gameState.value.positionBlack

        assertTrue("White piece out of bounds", positionWhite[0] in 0..7 && positionWhite[1] in 0..7)
        assertTrue("Pieces overlap", positionWhite != positionBlack)
    }

    @Test
    fun `test movePieceBlack within bounds and no overlap`() {
        viewModel.movePieceBlack()
        val positionBlack = viewModel.gameState.value.positionBlack
        val positionWhite = viewModel.gameState.value.positionWhite

        assertTrue("Black piece out of bounds", positionBlack[0] in 0..7 && positionBlack[1] in 0..7)
        assertTrue("Pieces overlap", positionBlack != positionWhite)
    }

    @Test
    fun `test multiple moves to ensure no overlap`() {
        repeat(10) {
            viewModel.movePieceWhite()
            viewModel.movePieceBlack()

            val positionWhite = viewModel.gameState.value.positionWhite
            val positionBlack = viewModel.gameState.value.positionBlack

            assertTrue("White piece out of bounds", positionWhite[0] in 0..7 && positionWhite[1] in 0..7)
            assertTrue("Black piece out of bounds", positionBlack[0] in 0..7 && positionBlack[1] in 0..7)
            assertTrue("Pieces overlap", positionWhite != positionBlack)
        }
    }
}