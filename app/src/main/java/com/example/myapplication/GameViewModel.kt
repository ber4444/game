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

    // Update the game state's isMoving variable (used to pace AutoPlay mode)
    fun setIsMoving(moving : Boolean) {
        _gameState.value = gameState.value.copy(isMoving = moving)
    }

    // DEBUG: View gameState in the console (New move)
    fun printMove(previousPositions : List<List<Int>>,
        currentTurn : Set, currentPositions : List<List<Int>> = when(currentTurn) {
        Set.BLACK -> _gameState.value.positionsBlack
        Set.WHITE -> _gameState.value.positionsWhite
    }, currentPieces : List<Piece> = when(currentTurn) {
        Set.BLACK -> _gameState.value.piecesBlack
        Set.WHITE -> _gameState.value.piecesWhite
    }) {
        // Figure out what the new move was TODO [CLEANUP]: Could just call inside move(), pass newPosition
        val newMove = previousPositions.toSet().filter { it !in currentPositions.toSet() }
        if(newMove.isEmpty()) { println("No move was made")}
        else {
            println("New move: $newMove")

            val pieceIndex = previousPositions.indexOf(newMove[0])
            val pieceType = currentPieces[pieceIndex].name

            // ERROR: New position is still [-1, -1] here, fixed later
            println("$currentTurn $pieceType (index $pieceIndex) moved from ${previousPositions[pieceIndex]} to ${currentPositions[pieceIndex]}")
            //println("$currentTurn Team: $currentPositions")
        }
    }

    fun gameMover() {
        gameMoves?.cancel()

        // Have the White team take its turn
        val currentTurn = Set.WHITE
        val previousPositions = when(currentTurn) {
            Set.WHITE ->  _gameState.value.positionsWhite
            Set.BLACK ->  _gameState.value.positionsBlack
        }
        gameMoves = viewModelScope.launch {
            delay(500) // Matches AnimatedChessPiece's tween(500)
            move(currentTurn) // TODO [CLEANUP]: rename to randomMove or AIMove to indicate it is unrelated to user input
            // this is just for testing purposes, in a real game this would be user input
            // also, it would account for whether you are in a check or checkmate or pinned situation

            // ERROR: Not enough delay for 'cancel' Button to be effective
            // TODO [UI]: Add delay to allow the User to press 'cancel' on AutoPlay mode before next turn occurs
            //printMove(previousPositions, currentTurn)
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
            // Unlock the button, the current turn has been finished and animated
            _gameState.value = _gameState.value.copy(
                buttonLock = false,
                isMoving = false
            )
        }
    }

    fun resetGame() {
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
        if(allyPieces.isEmpty() || _gameState.value.gameEnded) {
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
                gameEnded = true,
                winner = WinState.STALEMATE
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
        if (_gameState.value.winner != WinState.NONE) {
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
                    gameEnded = true,
                    winner = winner
                )
            }
        }

        // TODO [BUG]: Where is the [-1, -1] position fixed (Piece placed back on board)?
        //  The move function is incomplete and causes the test case issue (out of bounds at [-1, -1], not a randomNextPosition() issue)
        // DEBUG: Removing this line results in an infinite loop on test (gameOver is never reached since both Teams run out of usable Pieces)
        // Piece is put into an invalid position when it is being animated above the board to prevent duplicate
        mutableAllyPositions[pieceIndex] = listOf(-1,-1)

        // Otherwise, just update Pieces and their positions (only the enemy team could have lost a Piece)
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

