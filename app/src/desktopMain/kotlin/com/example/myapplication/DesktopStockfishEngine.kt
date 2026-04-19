package com.example.myapplication

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

class DesktopStockfishEngine : ChessEngine {

    private var process: Process? = null
    private var writer: OutputStreamWriter? = null
    private var readerThread: Thread? = null
    private val lineQueue = LinkedBlockingQueue<String>()

    @Volatile
    private var isReady = false

    fun start(): Boolean {
        if (process != null) {
            println("Stockfish engine already running")
            return isReady
        }

        return try {
            process = ProcessBuilder("stockfish")
                .redirectErrorStream(true)
                .start()
            val reader = BufferedReader(InputStreamReader(process!!.inputStream))
            writer = OutputStreamWriter(process!!.outputStream)

            readerThread = Thread({
                try {
                    var line = reader.readLine()
                    while (line != null) {
                        lineQueue.put(line)
                        line = reader.readLine()
                    }
                } catch (_: IOException) {
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                }
            }, "StockfishReader").apply {
                isDaemon = true
                start()
            }

            sendCommand("uci")
            val uciOk = waitForLine("uciok", timeoutMs = 5000)
            if (!uciOk) {
                println("Engine did not respond with 'uciok'")
                shutdown()
                return false
            }

            sendCommand("isready")
            val ready = waitForLine("readyok", timeoutMs = 5000)
            if (!ready) {
                println("Engine did not respond with 'readyok'")
                shutdown()
                return false
            }

            isReady = true
            println("Stockfish engine started successfully")
            true
        } catch (e: IOException) {
            println("Failed to start Stockfish engine. Is it installed and in PATH? ${e.message}")
            shutdown()
            false
        }
    }

    override fun getBestMove(fen: String): String? {
        return getBestMove(fen, 1000L)
    }

    fun getBestMove(fen: String, thinkTimeMs: Long): String? {
        if (!isReady || process == null) {
            return getEmbeddedBestMove(fen)
        }

        return try {
            sendCommand("position fen $fen")
            sendCommand("go movetime $thinkTimeMs")
            val bestMoveLine = waitForBestMove(timeoutMs = thinkTimeMs + 5000)
            if (bestMoveLine != null) {
                val parts = bestMoveLine.split(" ")
                val bestMoveIndex = parts.indexOf("bestmove")
                if (bestMoveIndex != -1 && bestMoveIndex + 1 < parts.size) {
                    parts[bestMoveIndex + 1]
                } else {
                    println("Could not parse bestmove from: $bestMoveLine")
                    null
                }
            } else {
                println("Timed out waiting for bestmove")
                null
            }
        } catch (e: IOException) {
            println("Error communicating with engine: ${e.message}")
            null
        }
    }

    private fun getEmbeddedBestMove(fen: String): String? {
        return try {
            val gameState = FenConverter.fenToGameState(fen)
            val isWhiteTurn = gameState.turn == Set.WHITE
            val allyPositions = if (isWhiteTurn) gameState.positionsWhite else gameState.positionsBlack
            val allyPieces = if (isWhiteTurn) gameState.piecesWhite else gameState.piecesBlack
            val enemyPositions = if (isWhiteTurn) gameState.positionsBlack else gameState.positionsWhite
            val enemyPieces = if (isWhiteTurn) gameState.piecesBlack else gameState.piecesWhite

            val move = pickMoveCPU(
                enemyPositions = enemyPositions,
                enemyPieces = enemyPieces,
                allyPositions = allyPositions,
                allyPieces = allyPieces
            )
            if (move.second == -1 || move.first == INVALID_POSITION) {
                println("Embedded fallback engine could not find a legal move")
                null
            } else {
                val from = allyPositions[move.second]
                UciMoveConverter.appMoveToUci(from, move.first)
            }
        } catch (e: IllegalArgumentException) {
            println("Invalid FEN supplied to embedded fallback engine: ${e.message}")
            null
        }
    }

    override fun close() {
        shutdown()
    }

    fun shutdown() {
        isReady = false
        try {
            if (writer != null) {
                sendCommand("quit")
            }
        } catch (_: IOException) {
        }

        try {
            writer?.close()
        } catch (_: IOException) {
        }

        readerThread?.interrupt()
        process?.destroy()
        process = null
        writer = null
        readerThread = null
        lineQueue.clear()
        println("Stockfish engine shut down")
    }

    private fun sendCommand(command: String) {
        writer?.let {
            it.write("$command\n")
            it.flush()
        }
    }

    private fun waitForLine(expected: String, timeoutMs: Long): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (true) {
            val remaining = deadline - System.currentTimeMillis()
            if (remaining <= 0) return false
            val line = lineQueue.poll(remaining, TimeUnit.MILLISECONDS) ?: return false
            if (line.startsWith(expected)) return true
        }
    }

    private fun waitForBestMove(timeoutMs: Long): String? {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (true) {
            val remaining = deadline - System.currentTimeMillis()
            if (remaining <= 0) return null
            val line = lineQueue.poll(remaining, TimeUnit.MILLISECONDS) ?: return null
            if (line.startsWith("bestmove")) return line
        }
    }
}

