package com.example.myapplication

import androidx.compose.runtime.Immutable

interface Piece {
    val set: Set
    val symbol: String
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
}
@Immutable
class Bishop(override val set: Set) : Piece {
    override val symbol: String = when (set) {
        Set.WHITE -> "♗"
        Set.BLACK -> "♝"
    }
}
@Immutable
class Knight(override val set: Set) : Piece {
    override val symbol: String = when (set) {
        Set.WHITE -> "♘"
        Set.BLACK -> "♞"
    }
}
@Immutable
class Pawn(override val set: Set) : Piece {
    override val symbol: String = when (set) {
        Set.WHITE -> "♙"
        Set.BLACK -> "♟︎"
    }
}
@Immutable
class Queen(override val set: Set) : Piece {
    override val symbol: String = when (set) {
        Set.WHITE -> "♕"
        Set.BLACK -> "♛"
    }
}
@Immutable
class Rook(override val set: Set) : Piece {
    override val symbol: String = when (set) {
        Set.WHITE -> "♖"
        Set.BLACK -> "♜"
    }
}