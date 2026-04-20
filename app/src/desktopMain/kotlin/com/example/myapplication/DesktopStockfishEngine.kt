package com.example.myapplication

class DesktopStockfishEngine : BaseStockfishEngine() {
    /** Returns "stockfish" — relies on the system PATH. Falls back to embedded CPU engine if not found. */
    override fun resolveExecutablePath(): String = "stockfish"
}
