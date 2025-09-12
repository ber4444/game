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

    // Update the gameState's autoPlay variable (used to start/stop AutoPlay mode)
    fun setAutoPlay(newVal : Boolean) {
        _gameState.value = gameState.value.copy(autoPlay = newVal)
    }

    // End the game in a draw
    fun setGameOver() {
        _gameState.value = gameState.value.copy(winState = WinState.DRAW)
    }

    // TODO [CLEANUP]: Rename to clarify White is taking turn
    fun gameMover() {
        gameMoves?.cancel()

        // Have the White team take its turn
        gameMoves = viewModelScope.launch {
            delay(500) // Matches AnimatedChessPiece's tween(500)
            move(Set.WHITE) // TODO [CLEANUP]: rename to randomMove or AIMove to indicate it is unrelated to user input
            // this is just for testing purposes, in a real game this would be user input
            // also, it would account for whether you are in a check or checkmate or pinned situation
        }
    }

    // TODO [CLEANUP]: Rename function to clarify another turn is being taken after the other team's Piece has been animated
    fun animationEnd() {
        // Only continue if there is a Piece to animate, set to null to ensure no repeating
        if (_animState.value.pieceToAnimate == null) return
        _animState.value = _animState.value.copy(
            pieceToAnimate = null
        )

        // Move a random Black Piece
        if (_gameState.value.turn != Set.BLACK) {
            move(Set.BLACK)
        } else {
            // Unlock the UI buttons, the current turn has been finished and animated
            _gameState.value = _gameState.value.copy(
                buttonLock = false,
            )
        }
    }

    fun resetGame() {
        println("Game reset")
        _gameState.value = GameUiState()
        _animState.value = PieceAnimationState()
    }

    @VisibleForTesting
    fun move(turn: Set) {
        // Update the gameState to reflect the current Team in play, disable buttons (prevent additional coroutines)
        _gameState.value = _gameState.value.copy(
            turn = turn,
            buttonLock = true,
        )

        // Depending on who's turn it is, different Ally and Enemy values are used
        val allyPositions: List<List<Int>>
        val allyPieces: List<Piece>
        val enemyPositions: List<List<Int>>
        val enemyPieces: List<Piece>
        when (turn) {
            Set.WHITE -> {
                allyPositions = _gameState.value.positionsWhite
                allyPieces = _gameState.value.piecesWhite
                enemyPositions = _gameState.value.positionsBlack
                enemyPieces = _gameState.value.piecesBlack
            }
            Set.BLACK -> {
                allyPositions = _gameState.value.positionsBlack
                allyPieces = _gameState.value.piecesBlack
                enemyPositions = _gameState.value.positionsWhite
                enemyPieces = _gameState.value.piecesWhite
            }
        }

        // Cannot perform moves when there are no pieces, return early
        // white can win while black is trying to take another turn, so bail if we know there is a winner
        if(allyPieces.isEmpty() || _gameState.value.winState != WinState.NONE) {
            return
        }

        val positionIndexPair = randomMove(
            turn = turn,
            enemyPositions = enemyPositions,
            enemyPieces = enemyPieces,
            allyPositions = allyPositions,
            allyPieces = allyPieces
        )
        val newPosition = positionIndexPair.first

        // a stalemate happens when a player has no moves
        if (newPosition.isEmpty()) {
            _gameState.value = _gameState.value.copy(
                winState = WinState.STALEMATE
            )
            return
        }

        // Update the position of the Piece
        val newPositions = allyPositions.toMutableList()
        newPositions[positionIndexPair.second] = newPosition

        // Update the game state, includes capturing of Pieces
        _gameState.value = deriveNewGameState(
            newPosition = newPosition,
            pieceIndex = positionIndexPair.second,
            turn = turn,
            enemyPieces =  enemyPieces,
            enemyPositions = enemyPositions,
            allyPositions = allyPositions
        )

        // If someone won, skip updating of animation state
        if (_gameState.value.winState != WinState.NONE) {
            // TODO [EXTRA]: Highlight Piece that will take the King/resulted in Checkmate
            return
        }

        // Update the animation state
        _animState.value = PieceAnimationState(
            pieceToAnimate = allyPieces[positionIndexPair.second],
            pieceIndex = positionIndexPair.second,
            animatePositionStart = allyPositions[positionIndexPair.second],
            animatePositionEnd = positionIndexPair.first
        )
    }

    // Given game parameters, returns an updated version of the GameUIState
    private fun deriveNewGameState(
        pieceIndex : Int, // The index of the Ally Piece being moved
        newPosition: List<Int>, // The new position of the Ally Piece
        turn : Set,
        enemyPieces : List<Piece>,
        enemyPositions: List<List<Int>>,
        allyPositions : List<List<Int>>
    ): GameUiState {
        // Will be updating the Enemy's Pieces (if capturing) and the position of the given Ally Piece
        var mutableEnemyPieces = enemyPieces.toMutableList()
        var mutableEnemyPositions = enemyPositions.toMutableList()
        var mutableAllyPositions = allyPositions.toMutableList()

        // if piece is on top of the other color's piece (capturing),
        // remove the other color's piece and its corresponding position
        if(newPosition in enemyPositions) {
            // Remove the Enemy Piece from the game (both in Piece list and position list)
            val index = enemyPositions.indexOf(newPosition)
            val removedPiece = mutableEnemyPieces.removeAt(index)
            mutableEnemyPositions.removeAt(index)

            // TODO [EXTRA]: Chess always ends in Check/Checkmate, King doesn't have to be captured (update King's amIDead logic)
            // TODO [EXTRA]: In randomNextPosition, if team is in Check, prioritize movement of the King/attacking Piece that is threatening the King
            // If the King was taken,
            if(removedPiece is King) {
                // Update the gameState to reflect the winner
                val winner = if(turn == Set.WHITE) WinState.WHITE else WinState.BLACK
                return _gameState.value.copy(
                    winState = winner
                )
            }
        }

        // Update Pieces and their positions (only the enemy team could have lost a Piece)
        mutableAllyPositions[pieceIndex] = newPosition
        return when(turn) {
            Set.WHITE -> {
                _gameState.value.copy(
                    piecesBlack = mutableEnemyPieces,
                    positionsBlack = mutableEnemyPositions,
                    positionsWhite = mutableAllyPositions
                )
            }
            Set.BLACK -> {
                _gameState.value.copy(
                    piecesWhite = mutableEnemyPieces,
                    positionsWhite = mutableEnemyPositions,
                    positionsBlack = mutableAllyPositions
                )
            }
        }
    }
}