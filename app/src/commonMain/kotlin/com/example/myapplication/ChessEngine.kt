package com.example.myapplication

interface ChessEngine {
    fun getBestMove(fen: String): String?
    fun close()
}
