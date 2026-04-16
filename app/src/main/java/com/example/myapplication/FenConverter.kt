package com.example.myapplication

/**
 * Converts between the app's internal board representation and FEN (Forsyth-Edwards Notation)
 * used by Stockfish and other UCI chess engines.
 *
 * Board coordinate mapping:
 *   App row 0 = rank 8 (top of board, black's back rank)
 *   App row 7 = rank 1 (bottom of board, white's back rank)
 *   App column 0 = file a (left side)
 *   App column 7 = file h (right side)
 */
object FenConverter {

    /** Standard starting position FEN. */
    const val STARTING_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w - - 0 1"

    /**
     * Convert a piece to its FEN character representation.
     * White pieces are uppercase, black pieces are lowercase.
     */
    fun pieceToFenChar(piece: Piece): Char {
        val base = when (piece) {
            is King -> 'k'
            is Queen -> 'q'
            is Rook -> 'r'
            is Bishop -> 'b'
            is Knight -> 'n'
            is Pawn -> 'p'
            else -> '?'
        }
        return if (piece.set == Set.WHITE) base.uppercaseChar() else base
    }

    /**
     * Convert the current game state to a FEN string.
     *
     * @param gameState The current game UI state
     * @return A FEN string representing the board position
     */
    fun gameStateToFen(gameState: GameUiState): String {
        // Build the board array (8x8, null = empty)
        val board = Array(BOARD_SIZE) { arrayOfNulls<Piece>(BOARD_SIZE) }

        // Place white pieces
        for (i in gameState.piecesWhite.indices) {
            val pos = gameState.positionsWhite[i]
            board[pos.first][pos.second] = gameState.piecesWhite[i]
        }

        // Place black pieces
        for (i in gameState.piecesBlack.indices) {
            val pos = gameState.positionsBlack[i]
            board[pos.first][pos.second] = gameState.piecesBlack[i]
        }

        // Build FEN piece placement string (rank 8 to rank 1, i.e., row 0 to row 7)
        val fenRows = mutableListOf<String>()
        for (row in 0 until BOARD_SIZE) {
            val sb = StringBuilder()
            var emptyCount = 0
            for (col in 0 until BOARD_SIZE) {
                val piece = board[row][col]
                if (piece == null) {
                    emptyCount++
                } else {
                    if (emptyCount > 0) {
                        sb.append(emptyCount)
                        emptyCount = 0
                    }
                    sb.append(pieceToFenChar(piece))
                }
            }
            if (emptyCount > 0) {
                sb.append(emptyCount)
            }
            fenRows.add(sb.toString())
        }

        val piecePlacement = fenRows.joinToString("/")

        // Active color
        val activeColor = if (gameState.turn == Set.WHITE) "w" else "b"

        // Castling, en passant, halfmove clock, and fullmove number
        // The app does not track these, so we use safe defaults
        val castling = "-"
        val enPassant = "-"
        val halfmoveClock = 0
        val fullmoveNumber = 1

        return "$piecePlacement $activeColor $castling $enPassant $halfmoveClock $fullmoveNumber"
    }
}
