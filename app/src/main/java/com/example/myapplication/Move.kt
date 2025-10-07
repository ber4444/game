package com.example.myapplication

// Used to represent an invalid position on the board
//  y and x values must always be between 0 and 8
val INVALID_POSITION = Pair(-1, -1)

// Return a randomly selected move
fun pickMoveRandom(
    enemyPositions: List<Pair<Int, Int>>,
    enemyPieces: List<Piece>,
    allyPositions: List<Pair<Int, Int>>,
    allyPieces: List<Piece>
): Pair<Pair<Int, Int>, Int> {
    // If no newPosition is assigned, returns an invalid position (not a possible move)
    var newPosition: Pair<Int, Int> = INVALID_POSITION
    var newPositionIndex = -1

    // Go through the Pieces in a random order
    val shuffledAllyIndexes = (0 until allyPieces.size).toList().shuffled()

    // Going through all the ally Pieces,
    for (i in 0 until shuffledAllyIndexes.size) {
        // Get all possible moves for the Piece
        val possibleMoves = allyPieces[shuffledAllyIndexes[i]].
            getValidMovesPositions(allyPositions[shuffledAllyIndexes[i]], enemyPositions, allyPositions)

        // If there are possible moves,
        if (possibleMoves.isNotEmpty()) {
            // Have the Piece take a random move
            newPosition = possibleMoves.random()
            newPositionIndex = shuffledAllyIndexes[i]
            break // Break to return the random move
        }
    }

    // Return the newPosition to update the PositionIndex with
    return Pair(newPosition, newPositionIndex)
}

// From the given list of moves, pick a move based on a CPU algorithm
fun pickMoveCPU(
    enemyPositions: List<Pair<Int, Int>>,
    enemyPieces: List<Piece>,
    allyPositions: List<Pair<Int, Int>>,
    allyPieces: List<Piece>
): Pair<Pair<Int, Int>, Int> {
    // Determine all possible moves given the state of the board
    val allPossibleMoves = getAllLegalMoves(
        enemyPositions = enemyPositions,
        enemyPieces = enemyPieces,
        allyPositions = allyPositions,
        allyPieces = allyPieces
    )
    if(allPossibleMoves.isEmpty()) return Pair(INVALID_POSITION, -1)


    // Focus on capturing enemy Pieces
    val captureMoves = allPossibleMoves.filter { it.first in enemyPositions }
    if(captureMoves.isNotEmpty()) {
        return captureMoves.random()
    }

    // Otherwise, return a random possible move
    return allPossibleMoves.random()
}

// Get the possible moves for all ally Pieces
fun getPossibleMoves(
    enemyPositions: List<Pair<Int, Int>>,
    //enemyPieces: List<Piece>, // [REMOVE]: Could pass to getValidMoves to determine if a taken move would put the Enemy King in Check (not efficient, only needed for CPU)
    allyPositions: List<Pair<Int, Int>>,
    allyPieces: List<Piece>) : List<Pair<Pair<Int, Int>, Int>> {
    // Determine what moves are possible for the given team, given the board information
    val possibleMoves : MutableList<Pair<Pair<Int, Int>, Int>> = mutableListOf() // List of (Position(y, x), PieceIndex) pairs

    // For every allied Piece,
    for(pieceIndex in 0 until allyPieces.size) {
        // Determine the current Piece's possible move locations
        val pieceType = allyPieces[pieceIndex]
        val allyPosition = allyPositions[pieceIndex]
        val pieceMoves = pieceType.getValidMovesPositions(allyPosition, enemyPositions, allyPositions)

        // Add each possible location, paired with the current pieceIndex
        for (move in pieceMoves) {
            possibleMoves += Pair(move, pieceIndex)
        }
    }
    return possibleMoves
}

// Return if the King is in Check/Checkmate
fun checkCheck(
    kingPosition : Pair<Int, Int>,
    enemyPositions: List<Pair<Int, Int>>,
    enemyPieces: List<Piece>,
    allyPositions: List<Pair<Int, Int>>
): Boolean {
    // DEBUG: When is Check looked for, [BUG] sometimes movement happens after a move resulting in Check
    //println("Checking King at ${kingPosition}..")

    // TODO [EFFICIENCY]: Rewrite King.amIDead logic to check if anyone can get to the King
    //  instead of checking all Enemy movement with getPossibleMoves
    //  Can use enemyPieces to determine what types of moves to look for
    //  (if no queens or bishops, don't need to check diagonals)
    // Using getPossibleMoves,
    val enemyMoves = getPossibleMoves(allyPositions, enemyPositions, enemyPieces)

    // Determine if the King can be attacked by any possible Enemy move
    val checkMoveIndex = enemyMoves.map { it.first }.indexOf(kingPosition)

    // DEBUG: Show which Piece poses a threat]
    if(checkMoveIndex != -1) {
        val attackerIndex = enemyMoves[checkMoveIndex].second
        println("King at ${kingPosition} is at risk of attack from ${enemyPieces[attackerIndex].name} at ${enemyPositions[attackerIndex]}!")
    }

    // If an enemy can reach the King, they are in Check
    return checkMoveIndex != -1

    /*
    // Return if the Piece is at risk by one or more Enemy Pieces
    return kingPosition in enemyMoves.map {
        it.first
    }*/

    // Logic from previous King.amIDead
    // Ignores Pawns and the opposing King (explicitly checks Enemy Piece type)
    /*
    val bishopMovement = listOf(Pair(1, 1), Pair(1, -1), Pair(-1, 1), Pair(-1, -1))
    val rookMovement = listOf(Pair(1, 0), Pair(-1, 0), Pair(0, 1), Pair(0, -1))
    val knightMovement = listOf(
        Pair(2, 1), Pair(1, 2), Pair(-1, 2), Pair(-2, 1),
        Pair(-2, -1), Pair(-1, -2), Pair(1, -2), Pair(2, -1)
    )

    // Not sure what 4 stands for here
    for (i in 0 until 4) {
        var rookX = kingPosition.first + rookMovement[i].first
        var rookY = kingPosition.second + rookMovement[i].second
        var bishopX = kingPosition.first + bishopMovement[i].first
        var bishopY = kingPosition.second + bishopMovement[i].second

        // Checks if Pieces that move like a Rook to get to the King
        while (rookX in 0 until BOARD_SIZE && rookY in 0 until BOARD_SIZE) {
            val pos = listOf(rookX, rookY)
            if (enemyPositions.contains(pos)) { // if this space has a rook or queen, we're dead!
                val pieceIndex = enemyPositions.indexOfFirst { it == pos }
                when (enemyPieces[pieceIndex]) {
                    is Queen, is Rook -> return true
                }
            } else if (allyPositions.contains(pos)) { // friend is blocking!
                break
            }
            rookX += rookMovement[i].first
            rookY += rookMovement[i].second
        }

        // Checks if Pieces that move like a Bishop can get to the King
        while (bishopX in 0 until BOARD_SIZE && bishopY in 0 until BOARD_SIZE) {
            val pos = listOf(bishopX, bishopY)
            if (enemyPositions.contains(pos)) { // if this space has a bishop or queen, we're dead!
                val pieceIndex = enemyPositions.indexOfFirst { it == pos }
                when (enemyPieces[pieceIndex]) {
                    is Queen, is Bishop -> return true
                }
            } else if (allyPositions.contains(pos)) { // friend is blocking!
                break
            }
            // move in the direction and see if the next square is also good
            bishopX += bishopMovement[i].first
            bishopY += bishopMovement[i].second
        }
    }

    // Checks if Pieces that move like a Knight can get to the King
    for (direction in knightMovement) {
        var x = kingPosition.first + direction.first
        var y = kingPosition.second + direction.second
        if (x in 0 until BOARD_SIZE && y in 0 until BOARD_SIZE && enemyPositions.contains(listOf(x, y))) {
            val pieceIndex = enemyPositions.indexOfFirst { it == listOf(x, y) }
            when (enemyPieces[pieceIndex]) {
                is Knight -> return true
            }
        }
    }

    // Nobody can reach the King right now
    return false */
}

// Return if the given team has any valid moves
fun hasLegalMoves(
    enemyPositions: List<Pair<Int, Int>>,
    enemyPieces: List<Piece>,
    allyPositions: List<Pair<Int, Int>>,
    allyPieces: List<Piece>
): Boolean {
    // Using getPossibleMoves,
    val possibleMoves = getPossibleMoves(enemyPositions, allyPositions, allyPieces)
    val kingIndex = allyPieces.indexOfFirst { it is King }
    val updatedAllyPositions = allyPositions.toMutableList()
    for (move in possibleMoves) {
        // move = Pair(Pair(y,x), pieceIndex)
        // If there is at least one valid move, return true
        val kingPosition = if (move.second == kingIndex) move.first else allyPositions[kingIndex]
        updatedAllyPositions[move.second] = move.first
        var tempEnemyPositions = enemyPositions
        var tempEnemyPieces = enemyPieces
        val capturedEnemyIndex = enemyPositions.indexOf(move.first)
        if (capturedEnemyIndex != -1) {
            // If a capture happened, create new lists WITHOUT the captured piece.
            tempEnemyPositions = enemyPositions.filterIndexed { index, _ -> index != capturedEnemyIndex }
            tempEnemyPieces = enemyPieces.filterIndexed { index, _ -> index != capturedEnemyIndex }
        }
        val isKingSafe = !checkCheck(
            kingPosition = kingPosition,
            enemyPositions = tempEnemyPositions,
            enemyPieces = tempEnemyPieces,
            allyPositions = updatedAllyPositions
        )
        if (isKingSafe) {
            return true
        }
    }
    return false
}

fun getLegalMovesForPiece(
    pieceIndex: Int,
    enemyPositions: List<Pair<Int, Int>>,
    enemyPieces: List<Piece>,
    allyPositions: List<Pair<Int, Int>>,
    allyPieces: List<Piece>
) : List<Pair<Int, Int>> {
    val legalMoves : MutableList<Pair<Int, Int>> = mutableListOf()
    // Using getPossibleMoves,
    val possibleMoves = allyPieces[pieceIndex].getValidMovesPositions(
        allyPositions[pieceIndex],
        enemyPositions,
        allyPositions
    )
    val kingIndex = allyPieces.indexOfFirst { it is King }
    val updatedAllyPositions = allyPositions.toMutableList()
    for (move in possibleMoves) {
        val kingPositionAfter = if (pieceIndex == kingIndex) move else allyPositions[kingIndex]
        val allyPositionsAfter = updatedAllyPositions.toMutableList().also {
            it[pieceIndex] = move
        }
        var tempEnemyPositions = enemyPositions
        var tempEnemyPieces = enemyPieces
        val capturedEnemyIndex = enemyPositions.indexOf(move)
        if (capturedEnemyIndex != -1) {
            // If a capture happened, create new lists WITHOUT the captured piece.
            tempEnemyPositions = enemyPositions.filterIndexed { index, _ -> index != capturedEnemyIndex }
            tempEnemyPieces = enemyPieces.filterIndexed { index, _ -> index != capturedEnemyIndex }
        }

        val isKingSafe = !checkCheck(
            kingPosition = kingPositionAfter,
            enemyPositions = tempEnemyPositions,
            enemyPieces = tempEnemyPieces,
            allyPositions = allyPositionsAfter
        )

        // 5. If the king is safe, this is a legal move.
        if (isKingSafe) {
            legalMoves.add(move)
        }

    }
    return legalMoves
}

fun getAllLegalMoves(
    enemyPositions: List<Pair<Int, Int>>,
    enemyPieces: List<Piece>,
    allyPositions: List<Pair<Int, Int>>,
    allyPieces: List<Piece>
) : List<Pair<Pair<Int, Int>, Int>> {
    val legalMoves : MutableList<Pair<Pair<Int, Int>, Int>> = mutableListOf()
    // Using getPossibleMoves,
    val possibleMoves = getPossibleMoves(enemyPositions, allyPositions, allyPieces)
    val kingIndex = allyPieces.indexOfFirst { it is King }
    val updatedAllyPositions = allyPositions.toMutableList()
    for (move in possibleMoves) {
        // move = Pair(Pair(y,x), pieceIndex)
        val kingPosition = if (move.second == kingIndex) move.first else allyPositions[kingIndex]
        updatedAllyPositions[move.second] = move.first
        var tempEnemyPositions = enemyPositions
        var tempEnemyPieces = enemyPieces
        val capturedEnemyIndex = enemyPositions.indexOf(move.first)
        if (capturedEnemyIndex != -1) {
            // If a capture happened, create new lists WITHOUT the captured piece.
            tempEnemyPositions = enemyPositions.filterIndexed { index, _ -> index != capturedEnemyIndex }
            tempEnemyPieces = enemyPieces.filterIndexed { index, _ -> index != capturedEnemyIndex }
        }
        val isKingSafe = !checkCheck(
            kingPosition = kingPosition,
            enemyPositions = tempEnemyPositions,
            enemyPieces = tempEnemyPieces,
            allyPositions = updatedAllyPositions
        )
        if (isKingSafe) {
            legalMoves.add(move)
        }
    }
    return legalMoves
}