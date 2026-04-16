package com.example.myapplication

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class GameViewModel(
    gameState: GameUiState = GameUiState()
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _gameState = MutableStateFlow(gameState)
    val gameState: StateFlow<GameUiState> = _gameState

    private val _animState = MutableStateFlow(PieceAnimationState())
    val animState: StateFlow<PieceAnimationState> = _animState

    private val _viewState = MutableStateFlow(ViewState())
    val viewState: StateFlow<ViewState> = _viewState

    private val _stockfishEnabled = MutableStateFlow(false)
    val stockfishEnabled: StateFlow<Boolean> = _stockfishEnabled

    private var gameMoves: Job? = null
    private var chessEngine: ChessEngine? = null

    fun attachEngine(engine: ChessEngine?) {
        chessEngine?.close()
        chessEngine = engine
        _stockfishEnabled.value = engine != null
    }

    fun close() {
        gameMoves?.cancel()
        chessEngine?.close()
        chessEngine = null
        _stockfishEnabled.value = false
        scope.cancel()
    }

    fun setAutoPlay(newVal: Boolean) {
        _gameState.value = gameState.value.copy(autoPlay = newVal)
    }

    fun hideWindow() {
        _viewState.value = viewState.value.copy(buttonLock = true, hideWindow = true)
    }

    fun updateSelected(position: Pair<Int, Int>) {
        _gameState.value = gameState.value.copy(selectedSquare = position)
    }

    fun playerMoveCheck(): Boolean {
        return true
    }

    fun playerMove(selectedPieceIndex: Int, newPosition: Pair<Int, Int>) {
        if (
            gameState.value.turn == Set.WHITE &&
            _gameState.value.winState == WinState.NONE &&
            _gameState.value.piecesWhite.isNotEmpty()
        ) {
            if (selectedPieceIndex == -1) {
                throw IllegalStateException("Cannot identify selected Piece!")
            }

            val legalMoves = getAllLegalMoves(
                enemyPositions = gameState.value.positionsBlack,
                enemyPieces = gameState.value.piecesBlack,
                allyPositions = gameState.value.positionsWhite,
                allyPieces = gameState.value.piecesWhite
            )

            if (legalMoves.none { move -> move.first == newPosition && move.second == selectedPieceIndex }) {
                println("Cannot move into Check!")
                return
            }

            _gameState.value = deriveNewGameState(
                newPosition = newPosition,
                pieceIndex = selectedPieceIndex,
                turn = gameState.value.turn,
                enemyPieces = gameState.value.piecesBlack,
                enemyPositions = gameState.value.positionsBlack,
                allyPositions = gameState.value.positionsWhite,
                allyPieces = _gameState.value.piecesWhite
            )

            _animState.value = PieceAnimationState(
                pieceToAnimate = gameState.value.piecesWhite[selectedPieceIndex],
                animatePositionStart = gameState.value.positionsWhite[selectedPieceIndex],
                animatePositionEnd = newPosition
            )
        }
    }

    fun startUserTurn() {
        gameMoves?.cancel()
        gameMoves = scope.launch {
            delay(500)
            moveCPU { enemyPositions, enemyPieces, allyPositions, allyPieces ->
                pickMoveStockfish(
                    chessEngine,
                    _gameState.value,
                    enemyPositions,
                    enemyPieces,
                    allyPositions,
                    allyPieces
                )
            }
        }
    }

    fun animationEnd() {
        if (_animState.value.pieceToAnimate == null) return
        _animState.value = _animState.value.copy(pieceToAnimate = null)

        if (_gameState.value.turn == Set.BLACK) {
            moveCPU { enemyPositions, enemyPieces, allyPositions, allyPieces ->
                pickMoveStockfish(
                    chessEngine,
                    _gameState.value,
                    enemyPositions,
                    enemyPieces,
                    allyPositions,
                    allyPieces
                )
            }
        } else {
            _viewState.value = _viewState.value.copy(moveButtonLock = false)
        }
    }

    fun updateUI() {
        if (_animState.value.pieceToAnimate == null) return
        _animState.value = _animState.value.copy(pieceToAnimate = null)

        if (_gameState.value.turn == Set.WHITE) {
            _viewState.value = _viewState.value.copy(moveButtonLock = false)
        }
    }

    fun resetGame() {
        println("Game reset")
        _gameState.value = GameUiState()
        _viewState.value = ViewState()
        _animState.value = PieceAnimationState()
    }

    fun moveCPU(
        turn: Set = _gameState.value.turn,
        pickMove: (
            enemyPositions: List<Pair<Int, Int>>,
            enemyPieces: List<Piece>,
            allyPositions: List<Pair<Int, Int>>,
            allyPieces: List<Piece>
        ) -> Pair<Pair<Int, Int>, Int>
    ) {
        _gameState.value = _gameState.value.copy(turn = turn, selectedSquare = INVALID_POSITION)
        _viewState.value = _viewState.value.copy(moveButtonLock = true)

        val allyPositions: List<Pair<Int, Int>>
        val allyPieces: List<Piece>
        val enemyPositions: List<Pair<Int, Int>>
        val enemyPieces: List<Piece>
        when (turn) {
            Set.WHITE -> {
                allyPositions = _gameState.value.positionsWhite
                allyPieces = _gameState.value.piecesWhite
                enemyPositions = _gameState.value.positionsBlack
                enemyPieces = _gameState.value.piecesBlack
            }

            Set.BLACK -> {
                allyPositions = _gameState.value.positionsBlack
                allyPieces = _gameState.value.piecesBlack
                enemyPositions = _gameState.value.positionsWhite
                enemyPieces = _gameState.value.piecesWhite
            }
        }

        if (allyPieces.isEmpty() || _gameState.value.winState != WinState.NONE) {
            return
        }

        if ((_gameState.value.inCheckWhite && _gameState.value.turn != Set.WHITE) ||
            (_gameState.value.inCheckBlack && _gameState.value.turn != Set.BLACK)
        ) {
            _gameState.value = _gameState.value.copy(
                winState = if (_gameState.value.turn == Set.WHITE) WinState.WHITE else WinState.BLACK
            )
            return
        }

        if ((_gameState.value.inCheckWhite && _gameState.value.turn == Set.WHITE) ||
            (_gameState.value.inCheckBlack && _gameState.value.turn == Set.BLACK)
        ) {
            if (hasLegalMoves(enemyPositions, enemyPieces, allyPositions, allyPieces)) {
                println("Must escape check!")
            } else {
                println("No legal moves to escape check! You lose!")
                _gameState.value = _gameState.value.copy(
                    winState = if (_gameState.value.turn == Set.BLACK) WinState.WHITE else WinState.BLACK
                )
                return
            }
        } else if ((!_gameState.value.inCheckWhite && _gameState.value.turn == Set.WHITE) ||
            (!_gameState.value.inCheckBlack && _gameState.value.turn == Set.BLACK)
        ) {
            if (hasLegalMoves(enemyPositions, enemyPieces, allyPositions, allyPieces)) {
                println("Continue playing, legal moves available.")
            } else {
                println("No legal moves available, Stalemate!")
                _gameState.value = _gameState.value.copy(winState = WinState.STALEMATE)
                return
            }
        }

        val positionIndexPair = pickMove(enemyPositions, enemyPieces, allyPositions, allyPieces)
        val newPosition = positionIndexPair.first

        _gameState.value = deriveNewGameState(
            newPosition = newPosition,
            pieceIndex = positionIndexPair.second,
            turn = _gameState.value.turn,
            enemyPieces = enemyPieces,
            enemyPositions = enemyPositions,
            allyPositions = allyPositions,
            allyPieces = allyPieces
        )

        _animState.value = PieceAnimationState(
            pieceToAnimate = allyPieces[positionIndexPair.second],
            animatePositionStart = allyPositions[positionIndexPair.second],
            animatePositionEnd = positionIndexPair.first
        )
    }

    private fun deriveNewGameState(
        pieceIndex: Int,
        newPosition: Pair<Int, Int>,
        turn: Set,
        enemyPieces: List<Piece>,
        enemyPositions: List<Pair<Int, Int>>,
        allyPositions: List<Pair<Int, Int>>,
        allyPieces: List<Piece>
    ): GameUiState {
        val mutableEnemyPieces = enemyPieces.toMutableList()
        val mutableEnemyPositions = enemyPositions.toMutableList()
        val mutableAllyPositions = allyPositions.toMutableList()

        println("Moving $turn ${allyPieces[pieceIndex].name} from ${allyPositions[pieceIndex]} to $newPosition")

        if (newPosition in enemyPositions) {
            val index = enemyPositions.indexOf(newPosition)
            println("${when (turn) { Set.WHITE -> Set.BLACK.name; Set.BLACK -> Set.WHITE.name }} ${enemyPieces[index].name} was captured!")
            mutableEnemyPositions.removeAt(index)
            mutableEnemyPieces.removeAt(index)
        }

        mutableAllyPositions[pieceIndex] = newPosition

        val allyKingIndex = allyPieces.indexOfFirst { it::class == King::class }
        val allyInCheck = checkCheck(
            mutableAllyPositions[allyKingIndex],
            mutableEnemyPositions,
            mutableEnemyPieces,
            mutableAllyPositions
        )

        val enemyKingIndex = mutableEnemyPieces.indexOfFirst { it::class == King::class }
        val enemyInCheck = checkCheck(
            mutableEnemyPositions[enemyKingIndex],
            mutableAllyPositions,
            allyPieces,
            mutableEnemyPositions
        )

        val nextTurn = when (_gameState.value.turn) {
            Set.WHITE -> Set.BLACK
            Set.BLACK -> Set.WHITE
        }

        if (allyInCheck) {
            println("Ally $turn in Check!")
        } else if (enemyInCheck) {
            println("Enemy $nextTurn in Check!")
        }

        return when (turn) {
            Set.WHITE -> _gameState.value.copy(
                turn = nextTurn,
                piecesBlack = mutableEnemyPieces,
                positionsBlack = mutableEnemyPositions,
                positionsWhite = mutableAllyPositions,
                inCheckWhite = allyInCheck,
                inCheckBlack = enemyInCheck
            )

            Set.BLACK -> _gameState.value.copy(
                turn = nextTurn,
                piecesWhite = mutableEnemyPieces,
                positionsWhite = mutableEnemyPositions,
                positionsBlack = mutableAllyPositions,
                inCheckWhite = enemyInCheck,
                inCheckBlack = allyInCheck
            )
        }
    }
}
