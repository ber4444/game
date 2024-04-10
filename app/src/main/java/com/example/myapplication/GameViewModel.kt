package com.example.myapplication

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class GameViewModel: ViewModel() {
    private val _gameState = MutableStateFlow(GameUiState())
    val gameState: StateFlow<GameUiState> = _gameState

    fun movePiece(){
        val state = _gameState.value
        val movePosition = state.positionWhite.toMutableList()
        if (state.positionWhite[0] < 7) {
            movePosition[0] += 1
        }
        _gameState.value = state.copy(positionWhite = movePosition)
    }

}