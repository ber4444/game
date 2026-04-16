package com.example.myapplication

import android.util.Log
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

/**
 * Manages the Stockfish chess engine process and communicates with it via the
 * UCI (Universal Chess Interface) protocol.
 *
 * Usage:
 *   1. Place the compiled Stockfish binary as `libstockfish.so` in the appropriate
 *      architecture folders under `app/src/main/jniLibs/` (arm64-v8a, armeabi-v7a,
 *      x86, x86_64).
 *   2. Create an instance with the app's native library directory path.
 *   3. Call [start] to launch the engine process.
 *   4. Call [getBestMove] to get the best move for a given FEN position.
 *   5. Call [shutdown] to stop the engine when done.
 *
 * @param nativeLibraryDir The path to the app's native library directory
 *        (from applicationContext.applicationInfo.nativeLibraryDir)
 */
class StockfishEngine(private val nativeLibraryDir: String) {

    companion object {
        private const val TAG = "StockfishEngine"
        private const val LIBRARY_NAME = "libstockfish.so"
        private const val DEFAULT_THINK_TIME_MS = 1000L
    }

    private var process: Process? = null
    private var writer: OutputStreamWriter? = null
    private var readerThread: Thread? = null
    private val lineQueue = LinkedBlockingQueue<String>()

    @Volatile
    private var isReady = false

    /**
     * Check if the Stockfish binary exists in the native library directory.
     */
    fun isAvailable(): Boolean {
        val file = File(nativeLibraryDir, LIBRARY_NAME)
        return file.exists() && file.canExecute()
    }

    /**
     * Start the Stockfish engine process and initialize UCI mode.
     *
     * @return true if the engine started successfully, false otherwise
     */
    fun start(): Boolean {
        if (process != null) {
            Log.w(TAG, "Engine already running")
            return isReady
        }

        val engineFile = File(nativeLibraryDir, LIBRARY_NAME)
        if (!engineFile.exists()) {
            Log.e(TAG, "Stockfish binary not found at: ${engineFile.absolutePath}")
            return false
        }

        return try {
            process = Runtime.getRuntime().exec(engineFile.absolutePath)
            val reader = BufferedReader(InputStreamReader(process!!.inputStream))
            writer = OutputStreamWriter(process!!.outputStream)

            // Start a background thread to read engine output into a blocking queue
            readerThread = Thread({
                try {
                    var line = reader.readLine()
                    while (line != null) {
                        Log.d(TAG, "Received: $line")
                        lineQueue.put(line)
                        line = reader.readLine()
                    }
                } catch (_: IOException) {
                    // Expected when process is destroyed
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                }
            }, "StockfishReader")
            readerThread?.isDaemon = true
            readerThread?.start()

            // Initialize UCI protocol
            sendCommand("uci")
            val uciOk = waitForLine("uciok", timeoutMs = 5000)
            if (!uciOk) {
                Log.e(TAG, "Engine did not respond with 'uciok'")
                shutdown()
                return false
            }

            // Send initial configuration
            sendCommand("isready")
            val ready = waitForLine("readyok", timeoutMs = 5000)
            if (!ready) {
                Log.e(TAG, "Engine did not respond with 'readyok'")
                shutdown()
                return false
            }

            isReady = true
            Log.i(TAG, "Stockfish engine started successfully")
            true
        } catch (e: IOException) {
            Log.e(TAG, "Failed to start Stockfish engine", e)
            shutdown()
            false
        }
    }

    /**
     * Get the best move for the given board position.
     *
     * @param fen The FEN string representing the current board position
     * @param thinkTimeMs How long (in ms) the engine should think (default 1000ms)
     * @return The best move in UCI notation (e.g., "e2e4"), or null if unavailable
     */
    fun getBestMove(fen: String, thinkTimeMs: Long = DEFAULT_THINK_TIME_MS): String? {
        if (!isReady || process == null) {
            Log.w(TAG, "Engine not ready")
            return null
        }

        return try {
            // Set up the position
            sendCommand("position fen $fen")

            // Ask the engine to search
            sendCommand("go movetime $thinkTimeMs")

            // Read until we get "bestmove"
            val bestMoveLine = waitForBestMove(timeoutMs = thinkTimeMs + 5000)
            if (bestMoveLine != null) {
                // Parse "bestmove e2e4 ponder e7e5" -> "e2e4"
                val parts = bestMoveLine.split(" ")
                val bestMoveIndex = parts.indexOf("bestmove")
                if (bestMoveIndex != -1 && bestMoveIndex + 1 < parts.size) {
                    val move = parts[bestMoveIndex + 1]
                    Log.d(TAG, "Best move: $move")
                    move
                } else {
                    Log.e(TAG, "Could not parse bestmove from: $bestMoveLine")
                    null
                }
            } else {
                Log.e(TAG, "Timed out waiting for bestmove")
                null
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error communicating with engine", e)
            null
        }
    }

    /**
     * Shut down the Stockfish engine process and release resources.
     */
    fun shutdown() {
        isReady = false
        try {
            if (writer != null) {
                sendCommand("quit")
            }
        } catch (_: IOException) {
            // Ignore errors when sending quit
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
        Log.i(TAG, "Stockfish engine shut down")
    }

    /**
     * Send a UCI command to the engine.
     */
    private fun sendCommand(command: String) {
        writer?.let {
            it.write("$command\n")
            it.flush()
            Log.d(TAG, "Sent: $command")
        }
    }

    /**
     * Wait for a line starting with the expected prefix, using the blocking queue.
     *
     * @param expected The prefix to wait for
     * @param timeoutMs Maximum time to wait in milliseconds
     * @return true if the expected line was received, false on timeout
     */
    private fun waitForLine(expected: String, timeoutMs: Long): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (true) {
            val remaining = deadline - System.currentTimeMillis()
            if (remaining <= 0) return false
            val line = lineQueue.poll(remaining, TimeUnit.MILLISECONDS) ?: return false
            if (line.startsWith(expected)) return true
        }
    }

    /**
     * Wait for the "bestmove" response from the engine, using the blocking queue.
     *
     * @param timeoutMs Maximum time to wait in milliseconds
     * @return The full bestmove line, or null on timeout
     */
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
