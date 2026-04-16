package com.example.myapplication

import android.content.res.AssetManager
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

class StockfishEngine(
    private val nativeLibraryDir: String,
    private val filesDir: File,
    private val assetManager: AssetManager,
    private val supportedAbis: Array<String>
) : ChessEngine {

    companion object {
        private const val DEFAULT_THINK_TIME_MS = 1000L
        private const val ASSET_DIRECTORY = "stockfish"
        private val ENGINE_FILE_NAMES = listOf("stockfish", "libstockfish.so", "libpenguin.so")
    }

    private var process: Process? = null
    private var writer: OutputStreamWriter? = null
    private var readerThread: Thread? = null
    private val lineQueue = LinkedBlockingQueue<String>()
    private var executableFile: File? = null
    private var useEmbeddedFallback = false

    @Volatile
    private var isReady = false

    fun isAvailable(): Boolean {
        return resolveEngineFile() != null || hasEmbeddedFallback()
    }

    fun start(): Boolean {
        if (process != null) {
            println("Stockfish engine already running")
            return isReady
        }

        val engineFile = resolveEngineFile()
        if (engineFile == null) {
            if (!hasEmbeddedFallback()) {
                println("Stockfish executable could not be resolved")
                return false
            }

            useEmbeddedFallback = true
            isReady = true
            println("No packaged Stockfish executable found; using embedded fallback engine")
            return true
        }

        return try {
            useEmbeddedFallback = false
            process = ProcessBuilder(engineFile.absolutePath)
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
            println("Failed to start Stockfish engine: ${e.message}")
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
            println("Using Stockfish executable from native library dir: ${nativeLib.absolutePath}")
            return nativeLib
        }

        findEngineInAssets()?.let { extracted ->
            executableFile = extracted
            println("Using Stockfish executable from assets: ${extracted.absolutePath}")
            return extracted
        }

        println(
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
                println("Found Stockfish candidate but it is not executable: ${candidate.absolutePath}")
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
            println("Failed to create Stockfish extraction directory: ${outputDir.absolutePath}")
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
                println("Unable to explicitly mark Stockfish readable: ${outputFile.absolutePath}")
            }
            if (!outputFile.setExecutable(true, true)) {
                println("Failed to mark Stockfish executable: ${outputFile.absolutePath}")
                return null
            }

            outputFile
        } catch (e: IOException) {
            println("Failed to extract Stockfish asset: $assetPath (${e.message})")
            null
        }
    }

    override fun getBestMove(fen: String): String? {
        return getBestMove(fen, DEFAULT_THINK_TIME_MS)
    }

    fun getBestMove(fen: String, thinkTimeMs: Long): String? {
        if (!isReady || process == null) {
            if (useEmbeddedFallback && isReady) {
                return getEmbeddedBestMove(fen)
            }

            println("Stockfish engine not ready")
            return null
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
        useEmbeddedFallback = false
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
