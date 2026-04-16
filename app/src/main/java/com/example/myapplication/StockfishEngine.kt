package com.example.myapplication

import android.content.res.AssetManager
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
 *   1. Prefer packaging the compiled Stockfish binary under
 *      `app/src/main/jniLibs/<abi>/` using one of the supported file names.
 *      Asset-based executables are supported as a fallback, but many Android devices
 *      cannot execute binaries copied into writable app storage.
 *      If no packaged binary is available, the class can still run a built-in fallback engine.
 *   2. Create an instance with the app's native library directory path plus app assets/files.
 *   3. Call [start] to launch the packaged engine process or the fallback engine.
 *   4. Call [getBestMove] to get the best move for a given FEN position.
 *   5. Call [shutdown] to stop the engine when done.
 *
 * @param nativeLibraryDir The path to the app's native library directory
 *        (from applicationContext.applicationInfo.nativeLibraryDir)
 */
class StockfishEngine(
    private val nativeLibraryDir: String,
    private val filesDir: File,
    private val assetManager: AssetManager,
    private val supportedAbis: Array<String>
) {

    companion object {
        private const val TAG = "StockfishEngine"
        private const val DEFAULT_THINK_TIME_MS = 1000L
        private const val ASSET_DIRECTORY = "stockfish"
        private val ENGINE_FILE_NAMES = listOf(
            "stockfish",
            "libstockfish.so",
            "libpenguin.so"
        )
    }

    private var process: Process? = null
    private var writer: OutputStreamWriter? = null
    private var readerThread: Thread? = null
    private val lineQueue = LinkedBlockingQueue<String>()
    private var executableFile: File? = null
    private var useEmbeddedFallback = false

    @Volatile
    private var isReady = false

    /**
     * Check if a usable engine backend can be resolved.
     */
    fun isAvailable(): Boolean {
        return resolveEngineFile() != null || hasEmbeddedFallback()
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

        val engineFile = resolveEngineFile()
        if (engineFile == null) {
            if (!hasEmbeddedFallback()) {
                Log.e(TAG, "Stockfish executable could not be resolved")
                return false
            }

            useEmbeddedFallback = true
            isReady = true
            Log.w(TAG, "No packaged Stockfish executable found; using embedded fallback engine")
            return true
        }

        return try {
            useEmbeddedFallback = false
            process = ProcessBuilder(engineFile.absolutePath)
                .redirectErrorStream(true)
                .start()
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

    private fun hasEmbeddedFallback(): Boolean = true

    private fun resolveEngineFile(): File? {
        executableFile?.let { existing ->
            if (existing.exists() && existing.canExecute()) {
                return existing
            }
        }

        findEngineInNativeLibs()?.let { nativeLib ->
            executableFile = nativeLib
            Log.i(TAG, "Using Stockfish executable from native library dir: ${nativeLib.absolutePath}")
            return nativeLib
        }

        findEngineInAssets()?.let { extracted ->
            executableFile = extracted
            Log.i(TAG, "Using Stockfish executable from assets: ${extracted.absolutePath}")
            return extracted
        }

        Log.e(
            TAG,
            "No Stockfish executable found. Checked assets/$ASSET_DIRECTORY/<abi>/ and $nativeLibraryDir for ${ENGINE_FILE_NAMES.joinToString()}"
        )
        return null
    }

    private fun findEngineInNativeLibs(): File? {
        for (fileName in ENGINE_FILE_NAMES) {
            val candidate = File(nativeLibraryDir, fileName)
            if (candidate.exists()) {
                if (candidate.canExecute()) {
                    return candidate
                }
                Log.w(TAG, "Found Stockfish candidate but it is not executable: ${candidate.absolutePath}")
            }
        }
        return null
    }

    private fun findEngineInAssets(): File? {
        val abiSearchOrder = buildList {
            addAll(supportedAbis.asList())
            add("")
        }

        for (abi in abiSearchOrder) {
            for (fileName in ENGINE_FILE_NAMES) {
                val assetPath = if (abi.isBlank()) {
                    "$ASSET_DIRECTORY/$fileName"
                } else {
                    "$ASSET_DIRECTORY/$abi/$fileName"
                }

                if (assetExists(assetPath)) {
                    return extractAssetExecutable(assetPath, abi, fileName)
                }
            }
        }

        return null
    }

    private fun assetExists(path: String): Boolean {
        return try {
            assetManager.open(path).use { true }
        } catch (_: IOException) {
            false
        }
    }

    private fun extractAssetExecutable(assetPath: String, abi: String, fileName: String): File? {
        val outputDir = if (abi.isBlank()) {
            File(filesDir, ASSET_DIRECTORY)
        } else {
            File(filesDir, "$ASSET_DIRECTORY/$abi")
        }
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            Log.e(TAG, "Failed to create Stockfish extraction directory: ${outputDir.absolutePath}")
            return null
        }

        val outputName = if (fileName.endsWith(".so")) "stockfish" else fileName
        val outputFile = File(outputDir, outputName)

        return try {
            assetManager.open(assetPath).use { input ->
                outputFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            if (!outputFile.setReadable(true, true)) {
                Log.w(TAG, "Unable to explicitly mark Stockfish readable: ${outputFile.absolutePath}")
            }
            if (!outputFile.setExecutable(true, true)) {
                Log.e(TAG, "Failed to mark Stockfish executable: ${outputFile.absolutePath}")
                return null
            }

            outputFile
        } catch (e: IOException) {
            Log.e(TAG, "Failed to extract Stockfish asset: $assetPath", e)
            null
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
            if (useEmbeddedFallback && isReady) {
                return getEmbeddedBestMove(fen)
            }

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
                Log.w(TAG, "Embedded fallback engine could not find a legal move")
                null
            } else {
                val from = allyPositions[move.second]
                val uciMove = UciMoveConverter.appMoveToUci(from, move.first)
                Log.d(TAG, "Embedded fallback move: $uciMove")
                uciMove
            }
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Invalid FEN supplied to embedded fallback engine", e)
            null
        }
    }

    /**
     * Shut down the Stockfish engine process and release resources.
     */
    fun shutdown() {
        isReady = false
        useEmbeddedFallback = false
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
