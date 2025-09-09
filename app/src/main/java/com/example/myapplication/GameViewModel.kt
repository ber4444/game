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

    private val _animState = MutableStateFlow(PieceAnimationState())
    val animState: StateFlow<PieceAnimationState> = _animState

    private var gameMoves: Job? = null

    fun gameMover() {
        gameMoves?.cancel()
        gameMoves = viewModelScope.launch {
            delay(500)
            move(Set.WHITE)
            // this is just for testing purposes, in a real game this would be user input
            // also, it would account for whether you are in a check or checkmate or pinned situation
        }
    }

    fun animationEnd() {
        // ignore on init of board and if position gets updated without a piece
        if (_animState.value.pieceToAnimate == null) return
        val state = _gameState.value
        val anim = _animState.value

        when (state.turn) {
            Set.WHITE -> {
                val positions = state.positionsWhite.toMutableList()
                val positionIndex = anim.pieceIndex
                positions[positionIndex] = anim.animatePositionEnd
                _gameState.value = state.copy(
                    positionsWhite = positions
                )
            }
            Set.BLACK -> {
                val positions = state.positionsBlack.toMutableList()
                val positionIndex = anim.pieceIndex
                positions[positionIndex] = anim.animatePositionEnd
                _gameState.value = state.copy(
                    positionsBlack = positions
                )
            }
        }

        _animState.value = anim.copy(
            pieceToAnimate = null
        )

        if (_gameState.value.turn != Set.BLACK) {
            move(Set.BLACK)
        } else {
            _gameState.value = _gameState.value.copy(
                buttonLock = false
            )
        }
    }

    fun resetGame() {
        _gameState.value = GameUiState()
        _animState.value = PieceAnimationState()
    }

    @VisibleForTesting
    fun move(turn: Set) {
        _gameState.value = _gameState.value.copy(
            turn = turn,
            buttonLock = true
        )

        var state = _gameState.value
        val allyPositions: List<List<Int>>
        val allyPieces: List<Piece>
        val enemyPositions: List<List<Int>>
        val enemyPieces: List<Piece>

        // Depending on who's turn it is, different values are used
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
        val shuffledIndexes = (0 until allyPieces.size).toList().shuffled()
        val positionIndexPair = randomMove(
            turn = turn,
            enemyPositions = enemyPositions,
            enemyPieces = enemyPieces,
            allyPositions = allyPositions,
            allyPieces = allyPieces,
            shuffledAllyIndexes = shuffledIndexes
        )
        val newPosition = positionIndexPair.first
        // a stalemate happens when a player has no moves
        if (newPosition.isEmpty()) {
            _gameState.value = state.copy(
                gameEnded = true,
                winner = WinState.STALEMATE
            )
            return
        }
        // Update the position of the Piece
        newPositions[positionIndexPair.second] = newPosition

        _gameState.value = deriveNewGameState(
            newPosition = newPosition,
            turn = turn,
            enemyPositions = enemyPositions,
            enemyPieces = enemyPieces
        )

        // if someone won, stop all the animatin'
        if (_gameState.value.winner != WinState.NONE) {
            return
        }

        _animState.value = PieceAnimationState(
            pieceToAnimate = allyPieces[positionIndexPair.second],
            pieceIndex = positionIndexPair.second,
            animatePositionStart = allyPositions[positionIndexPair.second],
            animatePositionEnd = positionIndexPair.first
        )
        // temporarily remove piece
        val mutableAllyPositions = allyPositions.toMutableList()
        mutableAllyPositions[positionIndexPair.second] = listOf(-1,-1)
        when (turn) {
            Set.WHITE -> _gameState.value = _gameState.value.copy(
                positionsWhite = mutableAllyPositions
            )
            Set.BLACK -> _gameState.value = _gameState.value.copy(
                positionsBlack = mutableAllyPositions
            )
        }
    }

    private fun deriveNewGameState(
        newPosition: List<Int>,
        turn: Set,
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
                val winner = if (turn == Set.WHITE) {
                    WinState.WHITE
                } else {
                    WinState.BLACK
                }

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
                    positionsBlack = updatedEnemyPositions
                )
            }
            Set.BLACK -> {
                _gameState.value.copy(
                    piecesWhite = updatedEnemyPieces,
                    positionsWhite = updatedEnemyPositions
                )
            }
        }

        return newState
    }
}

