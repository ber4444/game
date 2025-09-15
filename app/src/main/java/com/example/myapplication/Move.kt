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
    // TODO [LOGIC - EXTRA]: Prioritize moves that will progress the game
    //  such as capturing an Enemy Piece, putting the Enemy King in Check,
    //  moving the Ally King out of Check or attacking/blocking the
    //  Enemy Piece that is threatening the King

    // Determine all possible moves given the state of the board
    val allPossibleMoves = getPossibleMoves(enemyPositions, allyPositions, allyPieces)
    if(allPossibleMoves.isEmpty()) return Pair(INVALID_POSITION, -1)

    // TODO [LOGIC]: Make isInCheck a gameState variable
    //  (could also make Team a data class to hold Pieces, position, etc)
    //  When in Check, must make a move that will get King out of Check
    //  (otherwise Checkmate and the game is over)
    // If in Check, find a move to escape Check (otherwise stalemate)
    /*val escapeCheck = allPossibleMoves.filter { it }
    if(escapeCheck.isEmpty()) {
        return Pair(INVALID_POSITION, -1)
    }*/

    // [REMOVE]: King is never captured, just put in Check
    // Prioritize capturing enemy King
    val enemyKingIndex = enemyPieces.indexOfFirst { it is King }
    /*
    val kingKillMove = allPossibleMoves.find { it == enemyPositions[enemyKingIndex] }
    if(enemyKingIndex != -1 && kingKillMove != null) {
        println("${turn.name} ${allyPieces[kingKillMove.second].name} takes King at $kingKillMove!")
        return kingKillMove
    }*/

    // Focus on capturing enemy Pieces
    val captureMoves = allPossibleMoves.filter { it.first in enemyPositions }
    if(captureMoves.isNotEmpty()) {
        return captureMoves.random()
    }

    // Otherwise, return a random possible move
    return allPossibleMoves.random()
}

// TODO [LOGIC]: Use different functions to do board status check and movement
//  (hasTakenTurn = !Winner && pickedMove != null && pickedMove != InvalidMove)
//  when hasTakenTurn, swap current team and do next board status check and movement calls
// Allow the user to pick a move
fun pickMoveUser(
    enemyPositions: List<Pair<Int, Int>>,
    enemyPieces: List<Piece>,
    allyPositions: List<Pair<Int, Int>>,
    allyPieces: List<Piece>
): Pair<Pair<Int, Int>, Int> {
    // TEMP: Returns a randomly picked move
    return pickMoveRandom(enemyPositions, enemyPieces, allyPositions, allyPieces)

    val pickedMove : Pair<Pair<Int, Int>, Int> = Pair(INVALID_POSITION, -1)

    // TODO [UI - LOGIC]: Show user the possible moves for Pieces they click/tap on
    //  tracking: currentPiece, piecePossibleMoves, hasTakenTurn. Only return pick when hasTakenTurn

    return pickedMove
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

    // TODO [EXTRA]: Return list of Enemy Pieces that pose a risk to the King
    //  [UI] Could highlight/animate an arrow showing the possible move
    // Return if the Piece is at risk by one or more Enemy Pieces
    return kingPosition in enemyMoves.map { it.first }

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