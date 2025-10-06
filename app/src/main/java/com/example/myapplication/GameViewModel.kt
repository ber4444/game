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

    private val _viewState = MutableStateFlow(ViewState())
    val viewState: StateFlow<ViewState> = _viewState

    private var gameMoves: Job? = null

    // Update the gameState's autoPlay variable (used to start/stop AutoPlay mode)
    fun setAutoPlay(newVal : Boolean) {
        _gameState.value = gameState.value.copy(autoPlay = newVal)
    }

    // DEBUG: End the game in a draw
    fun setGameOver() {
        _gameState.value = gameState.value.copy(winState = WinState.DRAW)
    }

    // Hide the game over window, but don't allow the user to edit the gameState
    fun hideWindow() {
        _viewState.value = viewState.value.copy(buttonLock = true, hideWindow = true)
    }

    // Update the currently selected square on the board
    fun updateSelected(position: Pair<Int, Int>) {
        _gameState.value = gameState.value.copy(selectedSquare = position)
    }

    // TODO [BUG]: inCheck should be checked before user selects a move
    // Check if the Player is able to play (!gameOver, etc)
    fun playerMoveCheck() : Boolean {
        // Check if Player or opponent is in Check
        // Update the game to show if CPU won or Stalemate

        return true
    }

    // Move the Piece the player selected
    fun playerMove(selectedPieceIndex : Int, newPosition: Pair<Int, Int>) {
        if(gameState.value.turn == Set.WHITE && _gameState.value.winState == WinState.NONE && _gameState.value.piecesWhite.isNotEmpty()) {
            if(selectedPieceIndex == -1) { throw Exception("Cannot identify selected Piece!") }

            // Check occurs if a King can be attacked by the enemy
            // Checkmate occurs when there are no possible moves to escape Check
            //  Would require filtering possibleMoves or rechecking after movement to see if
            //  the King moved into enemy range or an ally is no longer blocking enemy movement

            // TODO [CLEANUP]: Move to a different function (called before allowing Player to select a move)
            // If the current player is in Check,
            if((_gameState.value.inCheckWhite && _gameState.value.turn == Set.WHITE) || (_gameState.value.inCheckBlack && _gameState.value.turn == Set.BLACK)) {
                println("Must escape check!")
            }
            // If the opposing player is in Check
            if((_gameState.value.inCheckWhite && _gameState.value.turn != Set.WHITE) || (_gameState.value.inCheckBlack && _gameState.value.turn != Set.BLACK)) {
                println("The enemy is already in Check, game over! You win!")
                // Current player wins
                _gameState.value = _gameState.value.copy(
                    winState = if(_gameState.value.turn == Set.WHITE) WinState.WHITE else WinState.BLACK
                )
                return
            }

            // Otherwise, let the player take their turn,
            // Update the game state, includes capturing of Pieces
            _gameState.value = deriveNewGameState(
                newPosition = newPosition,
                pieceIndex = selectedPieceIndex,
                turn = gameState.value.turn,
                enemyPieces = gameState.value.piecesBlack,
                enemyPositions = gameState.value.positionsBlack,
                allyPositions = gameState.value.positionsWhite,
                allyPieces = _gameState.value.piecesWhite
            )

            // TODO [BUG]: Animation doesn't play
            // Update the animation state
            _animState.value = PieceAnimationState(
                pieceToAnimate = gameState.value.piecesWhite[selectedPieceIndex],
                animatePositionStart = gameState.value.positionsWhite[selectedPieceIndex],
                animatePositionEnd = newPosition
            )
        }
    }

    // Autoplay
    // Allows the user to take their turn in the chess game
    fun startUserTurn() {
        gameMoves?.cancel()

        // Have the User's team take its turn
        gameMoves = viewModelScope.launch {
            delay(500) // Delay for? Matches AnimatedChessPiece's tween(500)
            // Uses randomMove for testing purposes, in a real game this would be user input
            moveCPU {
                enemyPositions: List<Pair<Int, Int>>,
                enemyPieces: List<Piece>,
                allyPositions: List<Pair<Int, Int>>,
                allyPieces: List<Piece> ->
                pickMoveCPU(enemyPositions, enemyPieces, allyPositions, allyPieces) // DEBUG: CPU vs CPU movement
                //pickMoveRandom(enemyPositions, enemyPieces, allyPositions, allyPieces)
            }
        }
    }

    // Called after AnimatedChessPiece is finished moving a Piece from one position to another
    fun animationEnd() {
        // Only continue if there is a Piece being animated, immediately set to null to ensure no repeating
        if (_animState.value.pieceToAnimate == null) return
        _animState.value = _animState.value.copy(
            pieceToAnimate = null
        )

        // If it is Black's turn, move a Black Piece
        if (_gameState.value.turn == Set.BLACK) {
            moveCPU {
                enemyPositions: List<Pair<Int, Int>>,
                enemyPieces: List<Piece>,
                allyPositions: List<Pair<Int, Int>>,
                allyPieces: List<Piece> ->
                pickMoveCPU(enemyPositions, enemyPieces, allyPositions, allyPieces)
            }
        } else {
            // Unlock the UI buttons for the user
            _viewState.value = _viewState.value.copy(moveButtonLock = false)
        }
    }

    // TODO [LOGIC]: Separate UI and logic updates
    // pieceToAnimate shouldn't be used to determine game logic,
    //  can instead have _gameState.value.hasTakenTurn after a move has been done
    //  (set to true after move(), set to false when turn is updated)
    fun updateUI() {
        // Only continue if there is a Piece being animated, immediately set to null to ensure no repeating
        if (_animState.value.pieceToAnimate == null) return
        _animState.value = _animState.value.copy(pieceToAnimate = null)

        // If it is the user's turn,
        if (_gameState.value.turn == Set.WHITE) {
            // Unlock the UI buttons, the user can now take their turn
            _viewState.value = _viewState.value.copy(moveButtonLock = false)
        }
    }

    // Reset the game's game state, view state, and animation state
    fun resetGame() {
        println("Game reset")
        _gameState.value = GameUiState()
        _viewState.value = ViewState()
        _animState.value = PieceAnimationState()
    }

    // Move a Piece from the given (or current) Set with the given move selecting algorithm
    @VisibleForTesting
    fun moveCPU(turn : Set = _gameState.value.turn,
        pickMove : (enemyPositions : List<Pair<Int, Int>>,
            enemyPieces : List<Piece>,
            allyPositions : List<Pair<Int, Int>>,
            allyPieces : List<Piece>) -> Pair<Pair<Int, Int>, Int>)
    {
        // Lock the UI buttons while an automatic turn is being taken
        _gameState.value = _gameState.value.copy(turn = turn, selectedSquare = INVALID_POSITION)
        _viewState.value = _viewState.value.copy(moveButtonLock = true)

        // Depending on whose turn it is, different Ally and Enemy values are used
        val allyPositions: List<Pair<Int, Int>>
        val allyPieces: List<Piece>
        val enemyPositions: List<Pair<Int, Int>>
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

        // Check occurs if a King can be attacked by the enemy
        // Checkmate occurs when there are no possible moves to escape Check
        //  Would require filtering possibleMoves or rechecking after movement to see if
        //  the King moved into enemy range or an ally is no longer blocking enemy movement

        // If the current team is in Check,
        if((_gameState.value.inCheckWhite && _gameState.value.turn == Set.WHITE) || (_gameState.value.inCheckBlack && _gameState.value.turn == Set.BLACK)) {
            println("Must escape check!")
        }
        // If the opposite team is in Check,
        if((_gameState.value.inCheckWhite && _gameState.value.turn != Set.WHITE) || (_gameState.value.inCheckBlack && _gameState.value.turn != Set.BLACK)) {
            // Current team wins
            _gameState.value = _gameState.value.copy(
                winState = if(_gameState.value.turn == Set.WHITE) WinState.WHITE else WinState.BLACK)
            return
        }

        // Pick a move using the given pickMove function
        val positionIndexPair = pickMove(enemyPositions, enemyPieces, allyPositions, allyPieces)
        val newPosition = positionIndexPair.first

        // TODO [LOGIC ERROR]: Can put self in Check
        // If the current team is in Check,
        if((_gameState.value.inCheckWhite && _gameState.value.turn == Set.WHITE) || (_gameState.value.inCheckBlack && _gameState.value.turn == Set.BLACK)) {
            // Current team loses if no legal moves to escape Check
            if (hasLegalMoves(enemyPositions = enemyPositions,
                    enemyPieces = enemyPieces,
                    allyPositions = allyPositions,
                    allyPieces = allyPieces
            )) {
                println("Must escape check!")
            } else {
                println("No legal moves to escape check! You lose!")
                _gameState.value = _gameState.value.copy(
                    winState = if(_gameState.value.turn == Set.BLACK) WinState.WHITE else WinState.BLACK)
                return
            }
        }

        // TODO [LOGIC]: Logic does not include endless moves (i.e. 2 kings left)
        // A Stalemate happens when neither player is in Check,
        //  but all possible moves for the current player will put them in Check
        // if(!inCheck && (possibleMove.filter { it.(doesn't put self in Check) } ).isEmpty())
        else if ((!_gameState.value.inCheckWhite && _gameState.value.turn == Set.WHITE) || (!_gameState.value.inCheckBlack && _gameState.value.turn == Set.BLACK)) {
            if (hasLegalMoves(enemyPositions = enemyPositions,
                    enemyPieces = enemyPieces,
                    allyPositions = allyPositions,
                    allyPieces = allyPieces
                )) {
                println("Continue playing, legal moves available.")
            } else {
                println("No legal moves available, Stalemate!")
                _gameState.value = _gameState.value.copy(
                    winState = WinState.STALEMATE
                )
                return
            }
        }

        // Update the game state, includes capturing of Pieces
        _gameState.value = deriveNewGameState(
            newPosition = newPosition,
            pieceIndex = positionIndexPair.second,
            turn = _gameState.value.turn,
            enemyPieces =  enemyPieces,
            enemyPositions = enemyPositions,
            allyPositions = allyPositions,
            allyPieces = allyPieces
        )

        // Update the animation state
        _animState.value = PieceAnimationState(
            pieceToAnimate = allyPieces[positionIndexPair.second],
            animatePositionStart = allyPositions[positionIndexPair.second],
            animatePositionEnd = positionIndexPair.first
        )
    }

    // Given game parameters, returns an updated version of the GameUIState
    private fun deriveNewGameState(
        pieceIndex : Int, // The index of the Ally Piece being moved
        newPosition: Pair<Int, Int>, // The new position of the Ally Piece
        turn : Set,
        enemyPieces : List<Piece>,
        enemyPositions: List<Pair<Int, Int>>,
        allyPositions : List<Pair<Int, Int>>,
        allyPieces: List<Piece>
    ): GameUiState {
        // Will be updating the Enemy's Pieces (if capturing) and the position of the given Ally Piece
        var mutableEnemyPieces = enemyPieces.toMutableList()
        var mutableEnemyPositions = enemyPositions.toMutableList()
        var mutableAllyPositions = allyPositions.toMutableList()

        // DEBUG: Print move information
        println("Moving $turn ${allyPieces[pieceIndex].name} from ${allyPositions[pieceIndex]} to $newPosition")

        // if piece is on top of the other color's piece (capturing),
        // remove the other color's piece and its corresponding position
        if(newPosition in enemyPositions) {
            // Remove the Enemy Piece from the game (both in Piece list and position list)
            val index = enemyPositions.indexOf(newPosition)

            // DEBUG: Ensure capturing is working
            println("${when(turn) { Set.WHITE -> { Set.BLACK.name } Set.BLACK -> { Set.WHITE.name } } } ${enemyPieces[index].name} was captured!")

            mutableEnemyPositions.removeAt(index)
            mutableEnemyPieces.removeAt(index)
        }

        // Update Pieces and their positions (only the enemy team could have lost a Piece)
        mutableAllyPositions[pieceIndex] = newPosition

        // Update the inCheck status of both teams
        // Is the ally team inCheck?
        val allyKingIndex = allyPieces.indexOfFirst { it::class == King::class }
        val allyInCheck = checkCheck(mutableAllyPositions[allyKingIndex], mutableEnemyPositions, mutableEnemyPieces, mutableAllyPositions)

        // Is the enemy team inCheck?
        val enemyKingIndex = mutableEnemyPieces.indexOfFirst { it::class == King::class }
        val enemyInCheck = checkCheck(mutableEnemyPositions[enemyKingIndex], mutableAllyPositions, allyPieces, mutableEnemyPositions)

        // Next turn will be the opposite Set
        val nextTurn = when(_gameState.value.turn) {
            Set.WHITE -> Set.BLACK
            Set.BLACK -> Set.WHITE
        }

        // DEBUG: Check which team is inCheck
        if(allyInCheck) { println("Ally $turn in Check!") }
        else if(enemyInCheck) { println("Enemy $nextTurn in Check!")}

        return when(turn) {
            Set.WHITE -> {
                _gameState.value.copy(
                    turn = nextTurn,
                    piecesBlack = mutableEnemyPieces,
                    positionsBlack = mutableEnemyPositions,
                    positionsWhite = mutableAllyPositions,
                    inCheckWhite = allyInCheck,
                    inCheckBlack = enemyInCheck
                )
            }
            Set.BLACK -> {
                _gameState.value.copy(
                    turn = nextTurn,
                    piecesWhite = mutableEnemyPieces,
                    positionsWhite = mutableEnemyPositions,
                    positionsBlack = mutableAllyPositions,
                    inCheckWhite = enemyInCheck,
                    inCheckBlack = allyInCheck
                )
            }
        }
    }
}