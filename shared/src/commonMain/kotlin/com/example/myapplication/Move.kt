package com.example.myapplication

fun randomMove(
    turn: Set,
    enemyPositions: List<List<Int>>,
    enemyPieces: List<Piece>,
    allyPositions: List<List<Int>>,
    allyPieces: List<Piece>,
    shuffledAllyIndexes: List<Int> // to make this testable we need to provide the indexes
): Pair<List<Int>, Int> {
    var newPosition: List<Int> = emptyList()
    var newPositionIndex = 0
    for (i in 0 until shuffledAllyIndexes.size) {
        val position = randomNextPosition(
            allyPieces[shuffledAllyIndexes[i]],
            turn,
            allyPositions[shuffledAllyIndexes[i]],
            enemyPositions,
            enemyPieces,
            allyPositions
        )
        if (position.isNotEmpty()) {
            newPosition = position
            newPositionIndex = shuffledAllyIndexes[i]
            break
        } else if (i == shuffledAllyIndexes.size - 1) {
            emptyList<Int>()
        }
    }

    return Pair(newPosition, newPositionIndex)
}

fun randomNextPosition(
    piece: Piece,
    turn: Set,
    currentPosition: List<Int>,
    enemyPositions: List<List<Int>>,
    enemyPieces: List<Piece>,
    allyPositions: List<List<Int>>
): List<Int> {
    val possibleMoves = piece.getValidMovesPositions(
        Pair(currentPosition[0], currentPosition[1]), enemyPositions, allyPositions
    )
    if (possibleMoves.isEmpty()) return emptyList()

    val teamPositions = allyPositions - currentPosition

    val validMoves = possibleMoves.filter { move ->
        val newPosition = listOf(move[0], move[1])
        val validPosition = newPosition[0] in 0..7 &&
                newPosition[1] in 0..7 &&
                newPosition !in teamPositions
        val kingDead = if (piece is King) {
            piece.amIDead(
                position = Pair(move[0], move[1]),
                enemyPositions = enemyPositions,
                enemyPieces = enemyPieces,
                allyPositions = allyPositions,
            )
        } else { false }

        validPosition && !kingDead
    }

    if (validMoves.isEmpty()) return emptyList()

    // Prioritize capturing enemy King
    val enemyKingIndex = enemyPieces.indexOfFirst { it is King }
    val kingKillMove = validMoves.find { it == enemyPositions[enemyKingIndex] }
    return if(enemyKingIndex != -1 && kingKillMove != null) {
        println("${turn.name} ${piece.name} takes King at $kingKillMove!")
        kingKillMove
    } else {
        validMoves.random()
    }
}