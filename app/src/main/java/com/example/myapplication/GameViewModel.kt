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

    // Hide the game over window, but don't allow the user to edit the gameState
    fun hideWindow() {
        _gameState.value = _gameState.value.copy(buttonLock = true)
        _animState.value = _animState.value.copy(hideWindow = true)
    }

    // Play game with User input
    fun playGame() {
        // If/While the game isn't over,
        if(_gameState.value.winState == WinState.NONE) {
        //while(_gameState.value.winState == WinState.NONE) {
            if(_animState.value.pieceToAnimate != null) {
                // Don't continue until animation has completed
            }
            else {
                // Update turn
                _gameState.value = _gameState.value.copy(
                    // Set to the next team's turn
                    turn = when(_gameState.value.turn) {
                        Set.WHITE -> Set.BLACK
                        Set.BLACK -> Set.WHITE
                    }
                )

                // If autoplay isn't being used and it is the user's turn,
                if(!_gameState.value.autoPlay && _gameState.value.turn == Set.WHITE) {
                    // Player plays
                } else {
                    // Randomly move a Piece on the current Team
                    move { turn: Set,
                        enemyPositions: List<List<Int>>,
                        enemyPieces: List<Piece>,
                        allyPositions: List<List<Int>>,
                        allyPieces: List<Piece> ->
                        pickMoveRandom(turn, enemyPositions, enemyPieces, allyPositions, allyPieces)
                    }
                }
            }
        }
    }

    // Allows the user to take their turn in the chess game
    fun startUserTurn() {
        gameMoves?.cancel()

        // Have the User's team take its turn
        gameMoves = viewModelScope.launch {
            delay(500) // Delay for? Matches AnimatedChessPiece's tween(500)
            // Uses randomMove for testing purposes, in a real game this would be user input
            move { turn: Set,
                enemyPositions: List<List<Int>>,
                enemyPieces: List<Piece>,
                allyPositions: List<List<Int>>,
                allyPieces: List<Piece> ->
                pickMoveRandom(turn, enemyPositions, enemyPieces, allyPositions, allyPieces)
            }
        }
    }

    // Called after AnimatedChessPiece is finished moving a Piece from one position to another
    // TODO [LOGIC - EXTRA]: Would need to be reworked (list of Pieces to move, remove one at a time) to add castling/multiple moving Pieces in one turn
    fun animationEnd() {
        // Only continue if there is a Piece being animated, immediately set to null to ensure no repeating
        if (_animState.value.pieceToAnimate == null) return
        _animState.value = _animState.value.copy(
            pieceToAnimate = null
        )

        // Swap to next team
        _gameState.value = _gameState.value.copy(
            // Set to the next team's turn
            turn = when(_gameState.value.turn) {
                Set.WHITE -> Set.BLACK
                Set.BLACK -> Set.WHITE
            }
        )

        // If it is Black's turn, move a random Black Piece
        if (_gameState.value.turn == Set.BLACK) {
            move { turn: Set,
                enemyPositions: List<List<Int>>,
                enemyPieces: List<Piece>,
                allyPositions: List<List<Int>>,
                allyPieces: List<Piece> ->
                pickMoveRandom(turn, enemyPositions, enemyPieces, allyPositions, allyPieces)
            }
        } else {
            // Unlock the UI buttons, the user can now take their turn
            _gameState.value = _gameState.value.copy(
                moveButtonLock = false,
            )
        }
    }

    // GOAL: Separate animState updates from game logic updates (changing turn, moving Black/CPU Piece)
    // pieceToAnimate shouldn't be used to determine game logic,
    //  can instead have _gameState.value.hasTakenTurn to separate Logic and UI
    //  (set to true after move(), set to false when turn is updated)
    fun updateUI() {
        // Only continue if there is a Piece being animated, immediately set to null to ensure no repeating
        if (_animState.value.pieceToAnimate == null) return
        _animState.value = _animState.value.copy(
            pieceToAnimate = null
        )

        // If it is the user's turn,
        if (_gameState.value.turn == Set.WHITE) {
            // Unlock the UI buttons, the user can now take their turn
            _gameState.value = _gameState.value.copy(
                moveButtonLock = false,
            )
        }
    }

    // Reset the game's game state and animation state
    fun resetGame() {
        println("Game reset")
        _gameState.value = GameUiState()
        _animState.value = PieceAnimationState()
    }

    fun checkKing() {

    }


    // Move a Piece from the given (or current) Set with the given move selecting algorithm
    @VisibleForTesting
    fun move(turn : Set = _gameState.value.turn,
        pickMove : (turn : Set,
            enemyPositions : List<List<Int>>,
            enemyPieces : List<Piece>,
            allyPositions : List<List<Int>>,
            allyPieces : List<Piece>) -> Pair<List<Int>, Int>)
    {
        // Lock the UI buttons while an automatic turn is being taken
        _gameState.value = _gameState.value.copy(
            turn = turn,
            moveButtonLock = true
        )

        // Depending on whose turn it is, different Ally and Enemy values are used
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

        // Cannot perform moves when there are no Pieces, return early
        if(allyPieces.isEmpty() || _gameState.value.winState != WinState.NONE) {
            return
        }

        // TODO [UI - EXTRA]: Highlight the square a King is on if it is check
        // Check occurs if a King can be attacked by the enemy
        // Checkmate occurs when there are no possible moves to escape Check
        //  Would require filtering possibleMoves or rechecking after movement to see if
        //  the King moved into enemy range or an Ally is no longer blocking enemy movement

        // TODO [BUG]: Should've been caught by ally check in previous call of move()
        // TEMP fix - Check if the enemy King was put in Check
        val enemyKingIndex = enemyPieces.indexOfFirst { it::class == King::class }
        val enemyKingPosition = Pair(enemyPositions[enemyKingIndex][0], enemyPositions[enemyKingIndex][1])
        val enemyKingInCheck = checkCheck(enemyKingPosition, allyPositions, allyPieces, enemyPositions)
        if (enemyKingInCheck) {
            // Current player wins
            _gameState.value = _gameState.value.copy(
                winState = if(_gameState.value.turn == Set.WHITE) WinState.WHITE else WinState.BLACK)
            return
        }

        // TODO [LOGIC]: If currently in Check, pickMove should return a move that escapes Check
        //  Otherwise the current team is in Checkmate and the game is over
        // TEMP: Game over if in Check
        // GOAL: Allow current team a chance to escape Check
        val allyKingIndex = allyPieces.indexOfFirst { it::class == King::class }
        var allyKingPosition = Pair(allyPositions[allyKingIndex][0], allyPositions[allyKingIndex][1])
        var allyKingInCheck = checkCheck(allyKingPosition, enemyPositions, enemyPieces, allyPositions)
        if(allyKingInCheck) {
            // Current player loses
            _gameState.value = _gameState.value.copy(
                winState = if(_gameState.value.turn == Set.BLACK) WinState.WHITE else WinState.BLACK)
            return
        }

        // Pick a move using the given pickMove function
        val positionIndexPair = pickMove(turn, enemyPositions, enemyPieces, allyPositions, allyPieces)
        val newPosition = positionIndexPair.first

        // a stalemate happens when a player has no moves
        if (newPosition.isEmpty()) {
            _gameState.value = _gameState.value.copy(
                winState = WinState.STALEMATE
            )
            return
        }

        // DEBUG: Print move information
        println("Moving $turn ${allyPieces[positionIndexPair.second].name} from ${allyPositions[positionIndexPair.second]}to ${newPosition}")

        // Update the game state, includes capturing of Pieces
        _gameState.value = deriveNewGameState(
            newPosition = newPosition,
            pieceIndex = positionIndexPair.second,
            turn = _gameState.value.turn,
            enemyPieces =  enemyPieces,
            enemyPositions = enemyPositions,
            allyPositions = allyPositions
        )

        // TODO [LOGIC ERROR]: Currently possible to put yourself in Check
        // TEMP: After moving, check if you put your own King in Check
        // GOAL: Filter from possible moves within pickMove()
        allyKingPosition = Pair(allyPositions[allyKingIndex][0], allyPositions[allyKingIndex][1])
        allyKingInCheck = checkCheck(allyKingPosition, enemyPositions, enemyPieces, allyPositions)
        if(allyKingInCheck) {
            // Current player loses
            _gameState.value = _gameState.value.copy(
            winState = if(_gameState.value.turn == Set.BLACK) WinState.WHITE else WinState.BLACK)
            return
        }

        // If someone won, skip updating of animation state
        if (_gameState.value.winState != WinState.NONE) {
            return
        }

        // If the game is continuing, update the animation state
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

            // TODO [UI - EXTRA]: Could also save capture position and animate Piece being defeated
            val removedPiece = mutableEnemyPieces.removeAt(index)
            mutableEnemyPositions.removeAt(index)

            // NOTE: Chess always ends in Check/Checkmate, King doesn't have to be captured. Remove?
            // If the King was taken,
            if(removedPiece is King) {
                // Update the gameState to reflect the winner
                return _gameState.value.copy(
                    winState = if(turn == Set.WHITE) WinState.WHITE else WinState.BLACK
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