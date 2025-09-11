package com.example.myapplication

// Randomly move a Piece on the given team
fun randomMove(
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
            // TODO [LOGIC FEATURE]: Instead of picking first available move, prioritize moves that will
            //  progress the game (capture Enemy Pieces, put Enemy King in Check, get Ally King out of Check)
        }
    }

    // TODO [UI BUG]: Sometimes there is an issues with Black Rooks that causes the game to pause (but resetting still works)
    // TODO [UI BUG]: PieceIcon have different size ratio compared to board square depending on screen size
    // DEBUG: Track movements
    println("Moving ${turn.name} ${allyPieces[newPositionIndex].name} from ${allyPositions[newPositionIndex]} to ${newPosition}")

    // Return the newPosition to update the PositionIndex with
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
    // For the given Piece, get its possible moves
    val possibleMoves = piece.getValidMovesPositions(
        Pair(currentPosition[0], currentPosition[1]), enemyPositions, allyPositions
    )
    if (possibleMoves.isEmpty()) return emptyList()

    // Check if the Piece can move (filter possible moves to moves it can actually take)
    val validMoves = possibleMoves.filter { move ->
        // TODO [CLEANUP]: Move Ally/Enemy collision logic here to reduce parameters passed in Piece.getValidMovesPositions
        //val teamPositions = allyPositions - currentPosition

        // TODO [CLEANUP]: Move logic to before randomNextPosition is called
        //  When in Check, must make a move that will get King out of Check (otherwise Checkmate and the game is over)
        // If this Piece is the King, check if it is in Check
        val kingDead = if(piece is King) {
            piece.amIDead(
                position = Pair(move[0], move[1]),
                enemyPositions = enemyPositions,
                enemyPieces = enemyPieces,
                allyPositions = allyPositions,
            )

            // TODO [CLEANUP]: Move and rewrite Check logic from King.amIDead
            // For each Piece on the Enemy team,
            /*for(enemyIndex in 0 until enemyPieces.size) {
                // Can any Enemy Piece reach the King with their possible moves?
                val enemyPossibleMove = enemyPieces[enemyIndex].getValidMovesPositions(Pair(enemyPositions[enemyIndex][0], enemyPositions[enemyIndex][1]), allyPositions, enemyPositions)
                if(currentPosition in enemyPossibleMove) {
                    true
                    }
                }
                false
            */
        } else { false }

        // Cannot move if the King is dead
        !kingDead
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