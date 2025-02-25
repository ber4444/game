package com.example.myapplication

import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

class GameViewModel : ViewModel() {
    private val _gameState = MutableStateFlow(GameUiState())
    val gameState: StateFlow<GameUiState> = _gameState

    // Board Representation: (row, col) -> Piece
    private val _board = MutableStateFlow<Map<Pair<Int, Int>, Piece?>>(
        mutableMapOf()
    )
    val board: StateFlow<Map<Pair<Int, Int>, Piece?>> = _board
    

    init {
        setupBoard()
    }

    private fun setupBoard() {
        val boardSetup = mutableMapOf<Pair<Int, Int>, Piece?>()

        // Place Kings
        boardSetup[Pair(0, 4)] = King(Set.WHITE)
        boardSetup[Pair(7, 4)] = King(Set.BLACK)

        // Place Queens
        boardSetup[Pair(0, 3)] = Queen(Set.WHITE)
        boardSetup[Pair(7, 3)] = Queen(Set.BLACK)


        _board.value = boardSetup
    }

    fun getValidMoves(piece: Piece, position: Pair<Int, Int>): List<List<Int>> {
        return piece.getValidMovesPositions(position.first, position.second, GameMode.FAST, _board.value)
    }

    private var gameMoves: Job? = null
    
    fun gameMover() {
        gameMoves?.cancel()
        gameMoves = viewModelScope.launch {
            delay(500)
            moveAllPiecesWhite()
            delay(500)
            moveAllPiecesBlack()
        }
    }
    
    @VisibleForTesting
    fun moveAllPiecesWhite() {
        var state = _gameState.value
        val newPositions = state.positionsWhite.toMutableList()
        if(state.piecesWhite.isEmpty()) return      // trying to perform moves when there are no pieces crashes app, so return early
        
        // for all white pieces, place them at a new position
        for(i in 0 until state.piecesWhite.size){
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
                        gameEnded = true,
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
        }
        _gameState.value = state
    }
    
    @VisibleForTesting
    fun moveAllPiecesBlack() {
        var state = _gameState.value
        val newPositions = state.positionsBlack.toMutableList()
        if(state.piecesBlack.isEmpty()) return      // trying to perform moves when there are no pieces crashes app, so return early
        
        // for all black pieces, place them at a new position
        for(i in 0 until state.piecesBlack.size){
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
        }
        _gameState.value = state
    }

    private fun randomMove(
        piece: Piece,
        currentPosition: List<Int>,
        enemyPositions: List<List<Int>>,
        allyPositions: List<List<Int>>
    ): List<Int> {
        val possibleMoves = piece.getValidMovesPositions(
            currentPosition[0], currentPosition[1], getGameMode(piece), _board.value
        )

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


    private fun getGameMode(piece: Piece): GameMode {
        return when (piece) {
            is King -> GameMode.SLOW
            is Queen -> GameMode.FAST
            else -> GameMode.SLOW // Default mode for other pieces
        }
    }


    //1. Check whether its moving slowly(1 square) or fast mode(to end of the square)
    //2. King could Slow and queen could move Fast
}
