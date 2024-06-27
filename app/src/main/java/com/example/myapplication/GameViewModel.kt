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
    
    val moves = mutableMapOf<String, List<List<Int>>>()
    
    init {
        val kingMoves = mutableListOf<List<Int>>()
        for(distance in -1 .. 1){
            kingMoves.add(listOf(distance, distance))
            kingMoves.add(listOf(distance, 0))
            kingMoves.add(listOf(distance, distance * -1))
            kingMoves.add(listOf(0, distance))
        }
        moves["King"] = kingMoves
    
        // could create a function that takes in a range and return a List<List<Int>> of moves, but kings and queens are only pieces with similar moveset
        // would have to create a List<List<Int>> of moves for every piece, as pieces have unique moves
        val queenMoves = mutableListOf<List<Int>>()
        for(distance in -7 .. 7){
            queenMoves.add(listOf(distance, distance))
            queenMoves.add(listOf(distance, 0))
            queenMoves.add(listOf(distance, distance * -1))
            queenMoves.add(listOf(0, distance))
        }
        moves["Queen"] = queenMoves
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
    
    private fun randomMove(piece: Piece, currentPosition: List<Int>, enemyPositions: List<List<Int>>, allyPositions: List<List<Int>>): List<Int> {
        val possibleMoves = mutableListOf<List<Int>>()
        
        // retrieve the list of moves depending on class type of piece
        val pieceMoves = when(piece) {
            is King -> moves["King"]
            is Queen -> moves["Queen"]
            else -> null
        }
        
        val teamPositions = allyPositions - currentPosition
    
        if (pieceMoves != null) {
            for(move in pieceMoves) {
                val newPosition = listOf(currentPosition[0] + move[0], currentPosition[1] + move[1])
                
                // new position has to be in the board, and cannot be on top of a team's piece. it CAN however be on top of an enemy piece (capturing)
                if(newPosition[0] in 0 .. 7 && newPosition[1] in 0 .. 7 && newPosition !in teamPositions){
                    possibleMoves.add(newPosition)
                }
            }
        }
        
        // no possible moves
        if(possibleMoves.isEmpty()) {
            return currentPosition
        }
        
        // pieces have preference to capture enemy king (to win the game immediately)
        val enemyKingPos = enemyPositions[0]
        if(enemyKingPos in possibleMoves){
            return enemyKingPos
        }
        
        return possibleMoves[Random.nextInt(possibleMoves.size)]
    }
}
