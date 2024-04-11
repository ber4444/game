package com.example.myapplication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

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

    private fun movePieceWhite(){
        val state = _gameState.value
        val movePosition = state.positionWhite.toMutableList()
        if (state.positionWhite[0] < 7) {
            if (state.positionWhite[0] +1 != state.positionBlack[0]) {
                movePosition[0] += 1
            } else {
                movePosition[1] += 1
            }
        }
        _gameState.value = state.copy(positionWhite = movePosition)
    }

    private fun movePieceBlack(){
        val state = _gameState.value
        val movePosition = state.positionBlack.toMutableList()
        if (state.positionBlack[0] > 0) {
            movePosition[0] -= 1
        }
        _gameState.value = state.copy(positionBlack = movePosition)
    }
}