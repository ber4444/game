package com.example.myapplication

import androidx.compose.runtime.Immutable

// A chess board is 8 x 8 squares
const val BOARD_SIZE = 8

// A chess piece
interface Piece {
    val set: Set
    val name: String
    val asset: Int

    fun getValidMovesPositions(position: Pair<Int, Int>,
                               enemyPositions: List<List<Int>>,
                               allyPositions: List<List<Int>>): List<List<Int>>
}

// Adds moves for a Piece with unlimited direction (Rook, Bishop, Queen)
private fun Piece.validateUnboundMove(
    direction: Pair<Int, Int>,
    position: Pair<Int, Int>,
    allyPositions: List<List<Int>>,
    enemyPositions: List<List<Int>>
): List<List<Int>> {
    val moves = mutableListOf<List<Int>>()
    var x = position.first + direction.first
    var y = position.second + direction.second

    // While the new (x, y) position is on the board,
    while (x in 0 until BOARD_SIZE && y in 0 until BOARD_SIZE) {
        val pos = listOf(x, y)

        if (allyPositions.contains(pos)) { // if we are bumping into an ally. stop. do not add.
            return moves
        }
        else {
            //if(!allyPositions.contains(pos))  // if we aren't crowding our friend. add. good move!
            moves.add(pos) // Add move, can add more
            if(enemyPositions.contains(pos)) { // if this move removes an enemy. Return the list. We got one!
                return moves
            }
        }
        // move in the direction and see if the next square is also good
        x += direction.first
        y += direction.second
    }

    return moves
}

// Add moves for a Piece with set positions to move to (Pawn, King, Knight)
private fun Piece.validateBoundMove(
    direction: Pair<Int, Int>,
    position: Pair<Int, Int>,
    allyPositions: List<List<Int>>
): List<List<Int>> {
    val moves = mutableListOf<List<Int>>()
    var x = position.first + direction.first
    var y = position.second + direction.second

    // If the position is on the board AND an ally is not in that position,
    if (x in 0 until BOARD_SIZE && y in 0 until BOARD_SIZE && !allyPositions.contains(listOf(x, y))) {
        moves.add(listOf(x, y))
    }

    return moves
}

// The color of a chess piece
enum class Set {
    WHITE, BLACK
}

// region Chess Piece Classes
@Immutable
class King(override val set: Set) : Piece {
    override val name = "King"
    override val asset: Int = when (set) {
        Set.WHITE -> R.drawable.king_light
        Set.BLACK -> R.drawable.king_dark
    }

    override fun getValidMovesPositions(
        position: Pair<Int, Int>,
        enemyPositions: List<List<Int>>,
        allyPositions: List<List<Int>>
    ): List<List<Int>> {
        val moves = mutableListOf<List<Int>>()
        val directions = listOf(
            Pair(1, 0), Pair(-1, 0), Pair(0, 1), Pair(0, -1),
            Pair(1, 1), Pair(1, -1), Pair(-1, 1), Pair(-1, -1)
        )
        for (direction in directions) {
            moves += validateBoundMove(
                direction = direction,
                position = position,
                allyPositions = allyPositions
            )
        }
        return moves
    }
}

@Immutable
class Bishop(override val set: Set) : Piece {
    override val name = "Bishop"
    override val asset: Int = when (set) {
        Set.WHITE -> R.drawable.bishop_light
        Set.BLACK -> R.drawable.bishop_dark
    }

    override fun getValidMovesPositions(
        position: Pair<Int, Int>,
        enemyPositions: List<List<Int>>,
        allyPositions: List<List<Int>>
    ): List<List<Int>> {
        val moves = mutableListOf<List<Int>>()
        val directions = listOf(Pair(1, 1), Pair(1, -1), Pair(-1, 1), Pair(-1, -1))
        for ((dx, dy) in directions) {
            moves += validateUnboundMove(
                direction = Pair(dx, dy),
                position = position,
                allyPositions = allyPositions,
                enemyPositions = enemyPositions
            )
        }
        return moves
    }

}
@Immutable
class Knight(override val set: Set) : Piece {
    override val name = "Knight"
    override val asset: Int = when (set) {
        Set.WHITE -> R.drawable.knight_light
        Set.BLACK -> R.drawable.knight_dark
    }

    override fun getValidMovesPositions(
        position: Pair<Int, Int>,
        enemyPositions: List<List<Int>>,
        allyPositions: List<List<Int>>
    ): List<List<Int>> {
        val moves = mutableListOf<List<Int>>()
        val deltas = listOf(
            Pair(2, 1), Pair(1, 2), Pair(-1, 2), Pair(-2, 1),
            Pair(-2, -1), Pair(-1, -2), Pair(1, -2), Pair(2, -1)
        )
        for (delta in deltas) {
            moves += validateBoundMove(
                direction = delta,
                position = position,
                allyPositions = allyPositions
            )
        }
        return moves
    }
}
@Immutable
class Pawn(override val set: Set) : Piece {
    override val name = "Pawn"
    override val asset: Int = when (set) {
        Set.WHITE -> R.drawable.pawn_light
        Set.BLACK -> R.drawable.pawn_dark
    }

    override fun getValidMovesPositions(
        position: Pair<Int, Int>,
        enemyPositions: List<List<Int>>,
        allyPositions: List<List<Int>>
    ): List<List<Int>> {
        val moves = mutableListOf<List<Int>>()
        val dir = if (set == Set.WHITE) 1 else -1
        val startRow = if (set == Set.WHITE) 1 else 6

        // Forward move
        val forward = listOf(position.first + dir, position.second)
        if (forward[0] in 0 until BOARD_SIZE && forward !in allyPositions && forward !in enemyPositions) {
            moves.add(forward)
            // Double move from start
            if (position.first == startRow) {
                val doubleForward = listOf(position.first + 2 * dir, position.second)
                if (doubleForward[0] in 0 until BOARD_SIZE && doubleForward !in allyPositions && doubleForward !in enemyPositions) {
                    moves.add(doubleForward)
                }
            }
        }
        // Captures
        for (dc in listOf(-1, 1)) {
            val capture = listOf(position.first + dir, position.second + dc)
            if (capture[0] in 0 until BOARD_SIZE && capture[1] in 0 until BOARD_SIZE && capture in enemyPositions) {
                moves.add(capture)
            }
        }
        return moves
    }

}
@Immutable
class Queen(override val set: Set) : Piece {
    override val name = "Queen"
    override val asset: Int = when (set) {
        Set.WHITE -> R.drawable.queen_light
        Set.BLACK -> R.drawable.queen_dark
    }

    override fun getValidMovesPositions(
        position: Pair<Int, Int>,
        enemyPositions: List<List<Int>>,
        allyPositions: List<List<Int>>
    ): List<List<Int>> {
        val moves = mutableListOf<List<Int>>()
        val directions = listOf(
            Pair(1, 0), Pair(-1, 0), // Vertical up/down
            Pair(0, 1), Pair(0, -1), // Horizontal left/right
            Pair(1, 1), Pair(-1, -1), // Diagonal top-right & bottom-left
            Pair(1, -1), Pair(-1, 1)  // Diagonal bottom-right & top-left
        )

        for ((dx, dy) in directions) {
            moves += validateUnboundMove(
                direction = Pair(dx, dy),
                position = position,
                allyPositions = allyPositions,
                enemyPositions = enemyPositions
            )
        }
        return moves
    }
}
@Immutable
class Rook(override val set: Set) : Piece {
    override val name = "Rook"
    override val asset: Int = when (set) {
        Set.WHITE -> R.drawable.rook_light
        Set.BLACK -> R.drawable.rook_dark
    }

    override fun getValidMovesPositions(
        position: Pair<Int, Int>,
        enemyPositions: List<List<Int>>,
        allyPositions: List<List<Int>>
    ): List<List<Int>> {
        val moves = mutableListOf<List<Int>>()
        val directions = listOf(Pair(1, 0), Pair(-1, 0), Pair(0, 1), Pair(0, -1))
        for ((dx, dy) in directions) {
            var x = position.first + dx
            var y = position.second + dy
            while (x in 0 until BOARD_SIZE && y in 0 until BOARD_SIZE) {
                val pos = listOf(x, y)
                if (!allyPositions.contains(pos)) {
                    moves.add(pos)
                    if (enemyPositions.contains(pos)) break
                } else {
                    break
                }
                x += dx
                y += dy
            }
        }
        return moves
    }
}
// endregion