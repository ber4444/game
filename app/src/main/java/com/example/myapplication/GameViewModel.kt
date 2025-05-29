package com.example.myapplication

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class GameViewModel : ViewModel() {
    private val _gameState = MutableStateFlow(GameUiState())
    val gameState: StateFlow<GameUiState> = _gameState

    private var gameMoves: Job? = null

    fun gameMover() {
        gameMoves?.cancel()
        gameMoves = viewModelScope.launch {
            delay(500)
            moveRandomWhite()
            delay(500)
            moveRandomBlack() // this is just for testing purposes, in a real game this would be user input
            // also, it would account for whether you are in a check or checkmate or pinned situation
        }
    }

    @VisibleForTesting
    fun moveRandomWhite() {
        var state = _gameState.value
        val newPositions = state.positionsWhite.toMutableList()
        if(state.piecesWhite.isEmpty()) return      // trying to perform moves when there are no pieces crashes app, so return early

        val i = 4//Random.nextInt(0, state.piecesWhite.size)
        val newPosition = randomMove(state.piecesWhite[i], state.positionsWhite[i], state.positionsBlack, state.positionsWhite)
        newPositions[i] = newPosition

        // if piece is on top of the other color's piece (capturing), remove the other color's piece and its corresponding position
        if(newPosition in state.positionsBlack){
            val pos = state.positionsBlack.indexOf(newPosition)
            val piecesWithRemovedPiece = state.piecesBlack.toMutableList()
            piecesWithRemovedPiece.removeAt(pos)
            val positionsWithRemovedPiece = state.positionsBlack.toMutableList()
            positionsWithRemovedPiece.removeAt(pos)

            // enemy's king is always at position 0. if captured, end the game
            if(pos == 0){
                state = state.copy(
                    piecesBlack = piecesWithRemovedPiece,
                    positionsBlack = positionsWithRemovedPiece,
                    gameEnded = true, // TODO implement stalemate too
                    winner = "White"
                )
                _gameState.value = state
                return
            }
            else{
                state = state.copy(
                    piecesBlack = piecesWithRemovedPiece,
                    positionsBlack = positionsWithRemovedPiece
                )
            }
        }
        state = state.copy(positionsWhite = newPositions)
        _gameState.value = state
    }

    @VisibleForTesting
    fun moveRandomBlack() {
        var state = _gameState.value
        val newPositions = state.positionsBlack.toMutableList()
        if(state.piecesBlack.isEmpty()) return      // trying to perform moves when there are no pieces crashes app, so return early

        val i = 4//Random.nextInt(0, state.piecesBlack.size)

        val newPosition = randomMove(state.piecesBlack[i], state.positionsBlack[i], state.positionsWhite, state.positionsBlack)
        newPositions[i] = newPosition

        // if piece is on top of the other color's piece (capturing), remove the other color's piece and its corresponding position
        if(newPosition in state.positionsWhite) {
            val pos = state.positionsWhite.indexOf(newPosition)
            val piecesWithRemovedPiece = state.piecesWhite.toMutableList()
            piecesWithRemovedPiece.removeAt(pos)
            val positionsWithRemovedPiece = state.positionsWhite.toMutableList()
            positionsWithRemovedPiece.removeAt(pos)

            // enemy's king is always at position 0. if captured, end the game
            if(pos == 0){
                state = state.copy(
                    piecesWhite = piecesWithRemovedPiece,
                    positionsWhite = positionsWithRemovedPiece,
                    gameEnded = true,
                    winner = "Black"
                )
                _gameState.value = state
                return
            }
            else{
                state = state.copy(
                    piecesWhite = piecesWithRemovedPiece,
                    positionsWhite = positionsWithRemovedPiece
                )
            }
        }
        state = state.copy(positionsBlack = newPositions)
        _gameState.value = state
    }

    private fun randomMove(
        piece: Piece,
        currentPosition: List<Int>,
        enemyPositions: List<List<Int>>,
        allyPositions: List<List<Int>>
    ): List<Int> {
        val possibleMoves = piece.getValidMovesPositions(
            Pair(currentPosition[0], currentPosition[1]), enemyPositions, allyPositions
        )
        if (possibleMoves.isEmpty()) return currentPosition

        val teamPositions = allyPositions - currentPosition

        val validMoves = possibleMoves.filter { move ->
            val newPosition = listOf(move[0], move[1])
            newPosition[0] in 0..7 && newPosition[1] in 0..7 && newPosition !in teamPositions
        }

        if (validMoves.isEmpty()) return currentPosition

        // Prioritize capturing enemy King
        val enemyKingPos = enemyPositions[0]
        return validMoves.find { it == enemyKingPos } ?: validMoves.random()
    }
}

