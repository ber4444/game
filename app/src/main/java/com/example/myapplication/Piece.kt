package com.example.myapplication

import androidx.compose.runtime.Immutable

interface Piece {
    val set: Set
    val asset: Int
    fun getValidMovesPositions(position: Pair<Int, Int>,
                               enemyPositions: List<List<Int>>,
                               allyPositions: List<List<Int>>): List<List<Int>>

}

enum class Set {
    WHITE, BLACK
}

@Immutable
class King(override val set: Set) : Piece {
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
        for ((dx, dy) in directions) {
            val x = position.first + dx
            val y = position.second + dy
            if (x in 0..7 && y in 0..7 && !allyPositions.contains(listOf(x, y))) {
                moves.add(listOf(x, y))
            }
        }
        return moves
        // TODO should not be under attack by an enemy piece
    }
}
@Immutable
class Bishop(override val set: Set) : Piece {
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
            var x = position.first + dx
            var y = position.second + dy
            while (x in 0..7 && y in 0..7) {
                val pos = listOf(x, y)
                if (allyPositions.contains(pos)) break
                moves.add(pos)
                if (enemyPositions.contains(pos)) break
                x += dx
                y += dy
            }
        }
        return moves
    }

}
@Immutable
class Knight(override val set: Set) : Piece {
    override val asset: Int = when (set) {
        Set.WHITE -> R.drawable.knight_light
        Set.BLACK -> R.drawable.king_dark
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
        for ((dx, dy) in deltas) {
            val x = position.first + dx
            val y = position.second + dy
            val pos = listOf(x, y)
            if (x in 0..7 && y in 0..7 && !allyPositions.contains(pos)) {
                moves.add(pos)
            }
        }
        return moves
    }
}
@Immutable
class Pawn(override val set: Set) : Piece {
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
        if (forward[0] in 0..7 && forward !in allyPositions && forward !in enemyPositions) {
            moves.add(forward)
            // Double move from start
            if (position.first == startRow) {
                val doubleForward = listOf(position.first + 2 * dir, position.second)
                if (doubleForward[0] in 0..7 && doubleForward !in allyPositions && doubleForward !in enemyPositions) {
                    moves.add(doubleForward)
                }
            }
        }
        // Captures
        for (dc in listOf(-1, 1)) {
            val capture = listOf(position.first + dir, position.second + dc)
            if (capture[0] in 0..7 && capture[1] in 0..7 && capture in enemyPositions) {
                moves.add(capture)
            }
        }
        return moves
    }

}
@Immutable
class Queen(override val set: Set) : Piece {
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
            var x = position.first
            var y = position.second

            x += dx
            y += dy

            if (x !in 0..7 || y !in 0..7) break // Stop if out of bounds

            val pieceAtTarget = enemyPositions.find { it == listOf(x, y) }
            if (pieceAtTarget == null) {
                if (allyPositions.find { it == listOf(x, y) } == null)
                    moves.add(listOf(x, y)) // Empty square, valid move
            } else {
                moves.add(listOf(x, y)) // Capture enemy piece
                break // Stop moving in this direction
            }
        }

        return moves
    }


}
@Immutable
class Rook(override val set: Set) : Piece {
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
            while (x in 0..7 && y in 0..7) {
                val pos = listOf(x, y)
                if (allyPositions.contains(pos)) break
                moves.add(pos)
                if (enemyPositions.contains(pos)) break
                x += dx
                y += dy
            }
        }
        return moves
    }
}