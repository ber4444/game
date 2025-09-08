package com.example.myapplication

interface Piece {
    val set: Set
    val name: String
    val asset: String

    fun getValidMovesPositions(position: Pair<Int, Int>,
                               enemyPositions: List<List<Int>>,
                               allyPositions: List<List<Int>>): List<List<Int>>
}

private fun Piece.validateUnboundMove(
    direction: Pair<Int, Int>,
    position: Pair<Int, Int>,
    allyPositions: List<List<Int>>,
    enemyPositions: List<List<Int>>
): List<List<Int>> {
    val moves = mutableListOf<List<Int>>()
    var x = position.first + direction.first
    var y = position.second + direction.second
    while (x in 0..7 && y in 0..7) {
        val pos = listOf(x, y)
        if (allyPositions.contains(pos)) { // if we are bumping into an ally. stop. do not add.
            return moves
        }
        if (!allyPositions.contains(pos)) { // if we aren't crowding our friend. add. good move!
            moves.add(pos)
        }
        if (enemyPositions.contains(pos)) { // if this move removes an enemy. Return the list. We got one!
            return moves
        }
        // move in the direction and see if the next square is also good
        x += direction.first
        y += direction.second
    }

    return moves
}

private fun Piece.validateBoundMove(
    direction: Pair<Int, Int>,
    position: Pair<Int, Int>,
    allyPositions: List<List<Int>>
): List<List<Int>> {
    val moves = mutableListOf<List<Int>>()
    var x = position.first + direction.first
    var y = position.second + direction.second
    if (x in 0..7 && y in 0..7 && !allyPositions.contains(listOf(x, y))) {
        moves.add(listOf(x, y))
    }

    return moves
}

enum class Set {
    WHITE, BLACK
}

class King(override val set: Set) : Piece {
    override val name = "King"
    override val asset: String = when (set) {
        Set.WHITE -> "king_light.xml"
        Set.BLACK -> "king_dark.xml"
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

class Bishop(override val set: Set) : Piece {
    override val name = "Bishop"
    override val asset: String = when (set) {
        Set.WHITE -> "bishop_light.xml"
        Set.BLACK -> "bishop_dark.xml"
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
class Knight(override val set: Set) : Piece {
    override val name = "Knight"
    override val asset: String = when (set) {
        Set.WHITE -> "knight_light.xml"
        Set.BLACK -> "knight_dark.xml"
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
class Pawn(override val set: Set) : Piece {
    override val name = "Pawn"
    override val asset: String = when (set) {
        Set.WHITE -> "pawn_light.xml"
        Set.BLACK -> "pawn_dark.xml"
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
class Queen(override val set: Set) : Piece {
    override val name = "Queen"
    override val asset: String = when (set) {
        Set.WHITE -> "queen_light.xml"
        Set.BLACK -> "queen_dark.xml"
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
class Rook(override val set: Set) : Piece {
    override val name = "Rook"
    override val asset: String = when (set) {
        Set.WHITE -> "rook_light.xml"
        Set.BLACK -> "rook_dark.xml"
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