package com.example.myapplication

// Return a randomly selected move
fun pickMoveRandom(
    turn: Set,
    enemyPositions: List<List<Int>>,
    enemyPieces: List<Piece>,
    allyPositions: List<List<Int>>,
    allyPieces: List<Piece>
): Pair<List<Int>, Int> {
    // If no newPosition is assigned, returns an empty list (no possible move)
    var newPosition: List<Int> = emptyList()
    var newPositionIndex = 0

    // Go through the Pieces in a random order
    val shuffledAllyIndexes = (0 until allyPieces.size).toList().shuffled()

    // Going through all the ally Pieces,
    for (i in 0 until shuffledAllyIndexes.size) {
        // Get a random move for the Piece to perform
        val position = randomNextPosition(
            allyPieces[shuffledAllyIndexes[i]],
            turn,
            allyPositions[shuffledAllyIndexes[i]],
            enemyPositions,
            enemyPieces,
            allyPositions
        )
        // If there is a possible move,
        if (position.isNotEmpty()) {
            newPosition = position
            newPositionIndex = shuffledAllyIndexes[i]
            break // Takes the first possible move
        }
    }

    // Return the newPosition to update the PositionIndex with
    return Pair(newPosition, newPositionIndex)
}

// From the given list of moves, pick a move based on a CPU algorithm
fun pickMoveCPU(
    turn: Set,
    enemyPositions: List<List<Int>>,
    enemyPieces: List<Piece>,
    allyPositions: List<List<Int>>,
    allyPieces: List<Piece>
): Pair<List<Int>, Int>  {
    // TODO [LOGIC - EXTRA]: Prioritize moves that will progress the game
    //  such as capturing an Enemy Piece, putting the Enemy King in Check,
    //  moving the Ally King out of Check or attacking/blocking the
    //  Enemy Piece that is threatening the King

    return pickMoveRandom(turn, enemyPositions, enemyPieces, allyPositions, allyPieces)
}

// TODO [CLEANUP]: Move logic to pickMoveCPU (filtering possible moves of Pieces to find best)
//  Or move filter to getPossibleMoves with a parameter to decide which moves to get (all, evadeCheck, etc)
fun randomNextPosition(
    piece: Piece,
    turn: Set,
    currentPosition: List<Int>,
    enemyPositions: List<List<Int>>,
    enemyPieces: List<Piece>,
    allyPositions: List<List<Int>>
): List<Int> {
    // For the given Piece, get its possible moves
    val possibleMoves = piece.getValidMovesPositions(
        Pair(currentPosition[0], currentPosition[1]), enemyPositions, allyPositions
    )
    if (possibleMoves.isEmpty()) return emptyList()

    // Check if the Piece can move (filter possible moves to moves it can actually take)
    val validMoves = possibleMoves.filter { move ->
        // TODO [CLEANUP]: Move Ally/Enemy collision logic here to reduce parameters passed in Piece.getValidMovesPositions
        //val teamPositions = allyPositions - currentPosition

        // TODO [LOGIC]: Make isInCheck a gameState variable (could also make Team a data class to hold Pieces, position, etc),
        //  When in Check, must make a move that will get King out of Check (otherwise Checkmate and the game is over)

        // TEMP
        true
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

// Allow the User to pick a move
fun pickMoveUser(
    turn: Set,
    enemyPositions: List<List<Int>>,
    enemyPieces: List<Piece>,
    allyPositions: List<List<Int>>,
    allyPieces: List<Piece>
): Pair<List<Int>, Int> {
    // TEMP: Returns a randomly picked move
    return pickMoveRandom(turn, enemyPositions, enemyPieces, allyPositions, allyPieces)

    val pickedMove : Pair<List<Int>, Int> = Pair(emptyList(), 0)

    // TODO [UI - LOGIC]: Show user the possible moves for Pieces they click/tap on
    //  tracking: currentPiece, piecePossibleMoves, hasTakenTurn. Only return pick when hasTakenTurn

    return pickedMove
}

// Get the possible moves for all Ally Pieces
fun getPossibleMoves(
    enemyPositions: List<List<Int>>,
    //enemyPieces: List<Piece>, // Could pass to getValidMoves to determine if a taken move would put the Enemy King in Check (not efficient, only needed for CPU)
    allyPositions: List<List<Int>>,
    allyPieces: List<Piece>) : List<Pair<List<Int>, Int>> {
    // Determine what moves are possible for the given team, given the board information
    val possibleMoves : MutableList<Pair<List<Int>, Int>> = mutableListOf() // List of (Position, PieceIndex) pairs

    // For every allied Piece,
    for(pieceIndex in 0 until allyPieces.size) {
        // Determine the current Piece's possible moves
        val pieceType = allyPieces[pieceIndex]
        val currentPosition = Pair(allyPositions[pieceIndex][0], allyPositions[pieceIndex][1])

        // TODO [CLEANUP]: Make Move a class? (set, pieceIndex : Int, startPosition : List<Int> or Pair<Int, Int>, EndPosition)
        //  Could also add booleans for capturesPiece and resultsInCheck, allowing the CPU to make smarter Moves
        val pieceMoves = pieceType.getValidMovesPositions(currentPosition, enemyPositions, allyPositions)

        // Add the moves
        for (move in pieceMoves) {
            possibleMoves += Pair(move, pieceIndex)
        }
    }
    return possibleMoves
}

// Tell if the King is in Check
fun checkCheck(
    kingPosition : Pair<Int, Int>,
    enemyPositions: List<List<Int>>,
    enemyPieces: List<Piece>,
    allyPositions: List<List<Int>>
): Boolean {
    // DEBUG: When is Check looked for, [BUG] sometimes movement happens after a move resulting in Check
    println("Checking ${kingPosition}..")

    // TODO [EFFICIENCY]: Rewrite King.amIDead logic to check if anyone can get to the King
    //  instead of checking all Enemy movement with getPossibleMoves
    //  Can use enemyPieces to determine what types of moves to look for
    //  (if no queens or bishops, don't need to check diagonals)
    // Using getPossibleMoves
    val enemyMoves = getPossibleMoves(allyPositions, enemyPositions, enemyPieces)
    return kingPosition in enemyMoves.map { Pair(it.first[0], it.first[1]) }

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