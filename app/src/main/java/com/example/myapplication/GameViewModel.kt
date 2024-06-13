package com.example.myapplication

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

class GameViewModel: ViewModel() {
    private val _gameState = MutableStateFlow(GameUiState())
    val gameState: StateFlow<GameUiState> = _gameState

    private var gameMoves: Job? = null

    fun gameMover(){
        gameMoves?.cancel()
        gameMoves = viewModelScope.launch {
            delay(500)
            movePieceWhite()
            delay(500)
            movePieceBlack()
        }
    }

    @VisibleForTesting
    fun movePieceWhite(){
        val state = _gameState.value
        val newPosition = randomMove(state.positionWhite, listOf(state.positionBlack))
        _gameState.value = state.copy(positionWhite = newPosition)
    }

    @VisibleForTesting
    fun movePieceBlack(){
        val state = _gameState.value
        val newPosition = randomMove(state.positionBlack, listOf(state.positionWhite))
        _gameState.value = state.copy(positionBlack = newPosition)
    }

    private fun randomMove(currentPosition: List<Int>, otherPiecePositions: List<List<Int>>): List<Int> {
        val possibleMoves = mutableListOf<List<Int>>()

        val moves = listOf(
            listOf(-1, -1), listOf(-1, 0), listOf(-1, 1),
            listOf(0, -1), listOf(0, 1),
            listOf(1, -1), listOf(1, 0), listOf(1, 1)
        )
        for (move in moves) {
            val newPosition = listOf(currentPosition[0] + move[0], currentPosition[1] + move[1])
            if (newPosition[0] in 0..7 && newPosition[1] in 0..7 && newPosition !in otherPiecePositions) {
                possibleMoves.add(newPosition)
            }
        }

        if (possibleMoves.isEmpty()) {
            return currentPosition
        }

        return possibleMoves[Random.nextInt(possibleMoves.size)]
    }
}
