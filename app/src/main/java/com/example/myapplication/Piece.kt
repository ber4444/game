package com.example.myapplication

import androidx.compose.runtime.Immutable

interface Piece {
    val set: Set
    val symbol: String
    fun getValidMovesPositions( position1: Int, position2: Int, mode: GameMode, board: Map<Pair<Int, Int>, Piece?>): List<List<Int>>

}

enum class Set {
    WHITE, BLACK
}

@Immutable
class King(override val set: Set) : Piece {
    override val symbol: String = when (set) {
        Set.WHITE -> "♔"
        Set.BLACK -> "♚"
    }
    //Returning all Valid position
    // It will take the current positions of the kind in 0th index
    // and the destination position of the KIng at 1st index
    // The size will be 2 always
    // Mode is slow or Fast
    override fun getValidMovesPositions(
        position1: Int, position2: Int, mode: GameMode, board: Map<Pair<Int, Int>, Piece?>
    ): List<List<Int>> {
        val moves = mutableListOf<List<Int>>()

        for (dx in -1..1) {
            for (dy in -1..1) {
                if (dx != 0 || dy != 0) {
                    val newX = position1 + dx
                    val newY = position2 + dy
                    if (newX in 0..7 && newY in 0..7) { // Ensure within bounds
                        val pieceAtTarget = board[Pair(newX, newY)]
                        if (pieceAtTarget == null || pieceAtTarget.set != this.set) {
                            moves.add(listOf(newX, newY)) // Add if empty or enemy piece
                        }
                    }
                }
            }
        }
        return moves
    }

}
@Immutable
class Bishop(override val set: Set) : Piece {
    override val symbol: String = when (set) {
        Set.WHITE -> "♗"
        Set.BLACK -> "♝"
    }

    override fun getValidMovesPositions(
        position1: Int,
        position2: Int,
        mode: GameMode,
        board: Map<Pair<Int, Int>, Piece?>
    ): List<List<Int>> {
        TODO("Not yet implemented")
    }

}
@Immutable
class Knight(override val set: Set) : Piece {
    override val symbol: String = when (set) {
        Set.WHITE -> "♘"
        Set.BLACK -> "♞"
    }

    override fun getValidMovesPositions(
        position1: Int,
        position2: Int,
        mode: GameMode,
        board: Map<Pair<Int, Int>, Piece?>
    ): List<List<Int>> {
        TODO("Not yet implemented")
    }
}
@Immutable
class Pawn(override val set: Set) : Piece {
    override val symbol: String = when (set) {
        Set.WHITE -> "♙"
        Set.BLACK -> "♟︎"
    }

    override fun getValidMovesPositions(
        position1: Int,
        position2: Int,
        mode: GameMode,
        board: Map<Pair<Int, Int>, Piece?>
    ): List<List<Int>> {
        TODO("Not yet implemented")
    }

}
@Immutable
class Queen(override val set: Set) : Piece {
    override val symbol: String = when (set) {
        Set.WHITE -> "♕"
        Set.BLACK -> "♛"
    }

    override fun getValidMovesPositions(
        position1: Int,
        position2: Int,
        mode: GameMode,
        board: Map<Pair<Int, Int>, Piece?>
    ): List<List<Int>> {
        val moves = mutableListOf<List<Int>>()
        val directions = listOf(
            Pair(1, 0), Pair(-1, 0), // Vertical up/down
            Pair(0, 1), Pair(0, -1), // Horizontal left/right
            Pair(1, 1), Pair(-1, -1), // Diagonal top-right & bottom-left
            Pair(1, -1), Pair(-1, 1)  // Diagonal bottom-right & top-left
        )

        for ((dx, dy) in directions) {
            var x = position1
            var y = position2

            while (true) {
                x += dx
                y += dy

                if (x !in 0..7 || y !in 0..7) break // Stop if out of bounds

                val pieceAtTarget = board[Pair(x, y)]
                if (pieceAtTarget == null) {
                    moves.add(listOf(x, y)) // Empty square, valid move
                } else {
                    if (pieceAtTarget.set != this.set) {
                        moves.add(listOf(x, y)) // Capture enemy piece
                    }
                    break // Stop moving in this direction
                }

                if (mode == GameMode.SLOW) break // If slow mode, stop after one move
            }
        }

        return moves
    }


}
@Immutable
class Rook(override val set: Set) : Piece {
    override val symbol: String = when (set) {
        Set.WHITE -> "♖"
        Set.BLACK -> "♜"
    }

    override fun getValidMovesPositions(
        position1: Int,
        position2: Int,
        mode: GameMode,
        board: Map<Pair<Int, Int>, Piece?>
    ): List<List<Int>> {
        TODO("Not yet implemented")
    }

}