package com.example.myapplication

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class GameViewModel(
    gameState: GameUiState = GameUiState()
) : ViewModel() {
    private val _gameState = MutableStateFlow(gameState)
    val gameState: StateFlow<GameUiState> = _gameState

    private var gameMoves: Job? = null

    fun gameMover() {
        gameMoves?.cancel()
        gameMoves = viewModelScope.launch {
            delay(500)
            move(Set.WHITE)
            delay(500)
            move(Set.BLACK) // this is just for testing purposes, in a real game this would be user input
            // also, it would account for whether you are in a check or checkmate or pinned situation
        }
    }

    @VisibleForTesting
    fun move(turn: Set) {
        var state = _gameState.value
        val allyPositions: List<List<Int>>
        val allyPieces: List<Piece>
        val enemyPositions: List<List<Int>>
        val enemyPieces: List<Piece>
        when (turn) {
            Set.WHITE -> {
                allyPositions = state.positionsWhite
                allyPieces = state.piecesWhite
                enemyPositions = state.positionsBlack
                enemyPieces = state.piecesBlack
            }
            Set.BLACK -> {
                allyPositions = state.positionsBlack
                allyPieces = state.piecesBlack
                enemyPositions = state.positionsWhite
                enemyPieces = state.piecesWhite
            }
        }

        // trying to perform moves when there are no pieces crashes app, so return early
        // white can win while black is trying to take another turn, so bail if we know there is a winner
        if(allyPieces.isEmpty() || state.gameEnded) {
            return
        }

        val newPositions = allyPositions.toMutableList()
        val positionIndexPair = randomMove(
            pieces = allyPieces,
            turn = turn,
            enemyPositions = enemyPositions,
            enemyPieces = enemyPieces,
            allyPositions = allyPositions
        )
        val newPosition = positionIndexPair.first
        // a stalemate happens when a player has no moves
        if (newPosition.isEmpty()) {
            _gameState.value = state.copy(
                gameEnded = true,
                winner = null
            )
            return
        }
        newPositions[positionIndexPair.second] = newPosition

        _gameState.value = deriveNewGameState(
            newPosition = newPosition,
            turn = turn,
            allyPositions = newPositions,
            enemyPositions = enemyPositions,
            enemyPieces = enemyPieces
        )
    }

    private fun randomMove(
        pieces: List<Piece>,
        turn: Set,
        enemyPositions: List<List<Int>>,
        enemyPieces: List<Piece>,
        allyPositions: List<List<Int>>
    ): Pair<List<Int>, Int> {
        val pieceIndexes = (0 until pieces.size).toList().shuffled()
        var newPosition: List<Int> = emptyList()
        var newPositionIndex = 0
        for (i in 0 until pieceIndexes.size) {
            val position = randomNextPosition(
                    pieces[pieceIndexes[i]],
                    turn,
                    allyPositions[pieceIndexes[i]],
                    enemyPositions,
                    enemyPieces,
                    allyPositions
                )
            if (position.isNotEmpty()) {
                newPosition = position
                newPositionIndex = pieceIndexes[i]
                break
            } else if (i == pieceIndexes.size - 1) {
                emptyList<Int>()
            }
        }

        return Pair(newPosition, newPositionIndex)
    }

    private fun randomNextPosition(
        piece: Piece,
        turn: Set,
        currentPosition: List<Int>,
        enemyPositions: List<List<Int>>,
        enemyPieces: List<Piece>,
        allyPositions: List<List<Int>>
    ): List<Int> {
        val possibleMoves = piece.getValidMovesPositions(
            Pair(currentPosition[0], currentPosition[1]), enemyPositions, allyPositions
        )
        if (possibleMoves.isEmpty()) return emptyList()

        val teamPositions = allyPositions - currentPosition

        val validMoves = possibleMoves.filter { move ->
            val newPosition = listOf(move[0], move[1])
            newPosition[0] in 0..7 && newPosition[1] in 0..7 && newPosition !in teamPositions
        }

        if (validMoves.isEmpty()) return emptyList()

        // Prioritize capturing enemy King
        val enemyKingIndex = enemyPieces.indexOfFirst { it is King }
        val kingKillMove = validMoves.find { it == enemyPositions[enemyKingIndex] }
        return if(enemyKingIndex != -1 && kingKillMove != null) {
            println("${turn.name} ${piece.name} takes King at $kingKillMove!")
            kingKillMove
        } else {
            validMoves.random()
        }
    }

    private fun deriveNewGameState(
        newPosition: List<Int>,
        turn: Set,
        allyPositions: List<List<Int>>,
        enemyPositions: List<List<Int>>,
        enemyPieces: List<Piece>
    ): GameUiState {
        var newState: GameUiState
        val updatedEnemyPieces = enemyPieces.toMutableList()
        val updatedEnemyPositions = enemyPositions.toMutableList()

        // if piece is on top of the other color's piece (capturing),
        // remove the other color's piece and its corresponding position
        if(newPosition in enemyPositions) {
            val pos = enemyPositions.indexOf(newPosition)
            val removedPiece = updatedEnemyPieces.removeAt(pos)
            updatedEnemyPositions.removeAt(pos)

            if(removedPiece is King) {
                val winner = if (turn == Set.WHITE) { "White" } else { "Black" }
                newState = _gameState.value.copy(
                    gameEnded = true,
                    winner = winner
                )
                return newState
            }
        }

        newState = when(turn) {
            Set.WHITE -> {
                _gameState.value.copy(
                    piecesBlack = updatedEnemyPieces,
                    positionsBlack = updatedEnemyPositions,
                    positionsWhite = allyPositions
                )
            }
            Set.BLACK -> {
                _gameState.value.copy(
                    piecesWhite = updatedEnemyPieces,
                    positionsWhite = updatedEnemyPositions,
                    positionsBlack = allyPositions
                )
            }
        }

        return newState
    }
}

