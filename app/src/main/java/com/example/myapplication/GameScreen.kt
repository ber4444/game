package com.example.myapplication

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import androidx.compose.material3.Card
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.window.Dialog

@Composable
fun GameScreen(
    windowSize: WindowWidthSizeClass,
    viewModel: GameViewModel
) {
    val gameState by viewModel.gameState.collectAsState()
    val animState by viewModel.animState.collectAsState()
    val viewState by viewModel.viewState.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.verticalScroll(scrollState)
    ) {

        // Show the gameOver window
        if(gameState.winState != WinState.NONE && !viewState.hideWindow) {
            // When the user dismisses the window, restart the game or hide the gameOver window (depending on button choice)
            val resetGame = { reset: Boolean ->
                if(reset) {
                    viewModel.resetGame()
                } else {
                    viewModel.hideWindow()
                }
            }
            PopupWindow(resetGame) {
                // Show the King of the winning Team, or indicate the game had no winner
                val winIcon: Int
                val gameEndMessageFormat: Int
                when (gameState.winState) {
                    WinState.NONE -> throw Exception("Invalid Game State")
                    WinState.WHITE -> {
                        winIcon = R.drawable.king_light
                        gameEndMessageFormat = R.string.game_end_message_winner
                    }

                    WinState.BLACK -> {
                        winIcon = R.drawable.king_dark
                        gameEndMessageFormat = R.string.game_end_message_winner
                    }

                    WinState.DRAW, WinState.STALEMATE -> {
                        winIcon = R.drawable.no_winner
                        gameEndMessageFormat = R.string.game_end_message_no_winner
                    }
                }
                Icon(
                    painter = painterResource(id = winIcon),
                    tint = Color.Unspecified,
                    modifier = Modifier.size(50.dp),
                    contentDescription = winIcon.toString()
                )
                Text(
                    modifier = Modifier.testTag("winnerText"),
                    text = stringResource(gameEndMessageFormat, gameState.winState),
                    color = Color.Red,
                    style = MaterialTheme.typography.titleLarge
                )

                // Prompt the user to reset or review their game
                Row(horizontalArrangement = Arrangement.SpaceBetween) {
                    Button(
                        modifier = Modifier.padding(5.dp),
                        onClick = { resetGame(true) }
                    ) {
                        Text(stringResource(R.string.play_again_button))
                    }
                    Button(
                        modifier = Modifier.padding(5.dp),
                        onClick = { resetGame(false) }
                    ) {
                        Text(stringResource(R.string.cancel_button))
                    }
                }
            }
        }

        // Display the Chess Board based on the current viewModel
        Board(gameState, animState, windowSize,
            { selectedPosition -> viewModel.updateSelected(selectedPosition) },
            { pieceIndex: Int, newPosition: Pair<Int, Int> ->  viewModel.playerMove(pieceIndex, newPosition) },
            { viewModel.animationEnd() })

        // Display a spacer
        Spacer(modifier = Modifier.padding(8.dp))

        // Display the AutoPlay mode toggle
        Text("Autoplay is ${if(gameState.autoPlay) "on" else "off"}")

        Row {
            Button(
                onClick = { viewModel.setAutoPlay(!gameState.autoPlay) },
                enabled = !viewState.buttonLock
            ) {
                Text(
                    text = stringResource(R.string.autoplay_button),
                    style = MaterialTheme.typography.titleLarge
                )
            }

            // Display the 'Move' Button
            Button(
                onClick = { viewModel.startUserTurn() },
                // Button is enabled only when game has not ended and it is White's turn
                enabled = gameState.winState == WinState.NONE && !viewState.moveButtonLock
            ) {
                Text(
                    text = stringResource(R.string.move_button),
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }

        Row {
            // Display the 'Reset' Button
            Button(
                onClick = { viewModel.resetGame() }
            ) {
                Text(stringResource(R.string.reset_button))
            }

            // Display the 'End' Button
            Button(
                onClick = { viewModel.setGameOver() },
                enabled = !viewState.buttonLock
            ) {
                Text(stringResource(R.string.end_button))
            }
        }
    }

    // If autoplay is on, the game hasn't ended, and there isn't a Piece being moved,
    if(gameState.autoPlay && gameState.winState == WinState.NONE && animState.pieceToAnimate == null) {
        // Move a White Piece
        viewModel.startUserTurn()
    }
}

// Identify what is going on in a displayed Square on the Board
enum class SquareType {
    Empty,  // No Pieces

    WhitePiece, // A White Piece is in the Square
    BlackPiece, // A Black Piece is in the Square

    // Selected Ally Piece
    CanMove,    // The selected Piece can make a valid move
    CannotMove, // The selected Piece cannot make a valid move

    // Possible moves
    PossibleMove,   // This is a possible move for the selected Piece
    PossibleCapture // This is a possible move which will capture an enemy Piece
}

// A Square on the Chess Board
@Composable
fun RowScope.Square(modifier: Modifier, isDarkSquare: Boolean,
    squareType : SquareType = SquareType.Empty, clickable: Boolean = false,
    onClick : (SquareType) -> Unit = {},
    content: @Composable ()-> Unit) {
    val borderWidth : Dp
    val borderColor : Color
    val shapeType : Shape

    // Different display based on SquareType
    when(squareType) {
        // Green to show the Piece can move
        SquareType.CanMove -> {
            borderWidth = 1.dp
            borderColor = Color.Green
            shapeType = RectangleShape
        }

        // Red to show the Piece cannot move
        SquareType.CannotMove -> {
            borderWidth = 1.dp
            borderColor = Color.Red
            shapeType = RectangleShape
        }

        // A possible move (doesn't capture an enemy)
        SquareType.PossibleMove -> {
            borderWidth = 5.dp
            borderColor = Color.Yellow
            shapeType = CircleShape
        }

        // A possible move that does capture an enemy
        SquareType.PossibleCapture -> {
            borderWidth = 5.dp
            borderColor = Color.Red
            shapeType = CircleShape
        }

        // Other Squares are not highlighted
        else -> {
            borderWidth = 0.dp
            borderColor = Color.Transparent
            shapeType = RectangleShape
        }
    }

    // Create a Box to hold the given content
    Box(modifier = modifier
        .weight(1f)
        .aspectRatio(1f)
        .background(
            color = if (isDarkSquare) MaterialTheme.colorScheme.secondary
            else Color.White
        )
        .border(borderWidth, borderColor, shapeType)
        .clickable(clickable, onClickLabel = null, role = null, onClick = { onClick(squareType) }),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
fun Board(
    gameState: GameUiState,
    animState: PieceAnimationState,
    windowSize: WindowWidthSizeClass,
    updateSelected: (Pair<Int, Int>) -> Unit,
    playerMove: (Int, Pair<Int, Int>) -> Unit,
    animationEnd: () -> Unit
) {
    // TODO [EFFICIENCY]: Calculate before draw?
    val squareSizePx = remember { mutableStateOf(IntSize.Zero) }
    val squareAvgSizePx = remember { mutableStateOf(IntSize.Zero) }

    // The possible moves of the selected square
    val selectedPossibleMoves = remember { mutableStateOf(emptyList<Pair<Int,Int>>()) }

    // If not in autoplay and not in an invalid position,
    if(!gameState.autoPlay) {
        if(gameState.selectedSquare != INVALID_POSITION) {
            // If the user has selected one of their Pieces,
            val pieceIndex = gameState.positionsWhite.indexOf(gameState.selectedSquare)
            if(pieceIndex != -1) {
                // Get the possible moves for the current position
                selectedPossibleMoves.value =
                    getLegalMovesForPiece(
                        pieceIndex = pieceIndex,
                        enemyPieces = gameState.piecesBlack,
                        enemyPositions = gameState.positionsBlack,
                        allyPositions = gameState.positionsWhite,
                        allyPieces = gameState.piecesWhite
                    )
            }
        }
    }
    else if(selectedPossibleMoves.value.isNotEmpty()) { selectedPossibleMoves.value = emptyList() }

    Box(
        modifier = Modifier
            .padding(
                when (windowSize){
                    WindowWidthSizeClass.Expanded -> 18.dp
                    WindowWidthSizeClass.Medium -> 12.dp
                    else -> 8.dp
                }
            )
    ) {
        Column {
            // A Column with 8 Rows
            repeat(8) { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    // Each Row has 8 Squares
                    repeat(8) { column ->
                        // The position of the current Square
                        val currentSquare = Pair(row, column)

                        // Type of Square to draw based on Piece type and possible moves
                        val squareType : SquareType =
                            if(currentSquare == gameState.selectedSquare) {
                                if(selectedPossibleMoves.value.isEmpty()) {
                                    SquareType.CannotMove
                                } else {
                                    SquareType.CanMove
                                }
                            }
                            else {
                                    if(currentSquare in selectedPossibleMoves.value) {
                                        if(currentSquare in gameState.positionsBlack) {
                                            SquareType.PossibleCapture
                                        } else {
                                            SquareType.PossibleMove
                                        }
                                    } else if(currentSquare in gameState.positionsWhite) {
                                        SquareType.WhitePiece
                                    } else if(currentSquare in gameState.positionsBlack) {
                                        SquareType.BlackPiece
                                    }
                                    else {
                                        SquareType.Empty
                                    }
                            }

                        // Can only click on White Pieces or possible moves
                        val clickable : Boolean = squareType == SquareType.PossibleMove ||  squareType == SquareType.PossibleCapture || squareType ==  SquareType.WhitePiece

                        Square(
                            modifier = Modifier.onGloballyPositioned {
                                // If this position is where an animation will start,
                                if (animState.animatePositionStart == currentSquare) {
                                    // Save the size of the red bounding square for the moving animation
                                    squareSizePx.value = it.size
                                }
                                if(squareAvgSizePx.value == IntSize.Zero) {
                                    squareAvgSizePx.value = it.size // Needs to be calculated once
                                }
                            },
                            (row + column) % 2 == 1, squareType, !gameState.autoPlay && clickable,
                            { squareType ->
                                when (squareType) {
                                    SquareType.PossibleMove, SquareType.PossibleCapture -> {
                                        val moveIndex = gameState.selectedSquare

                                        // reset selection
                                        updateSelected(INVALID_POSITION)
                                        selectedPossibleMoves.value = emptyList()

                                        // Move based on the Player's input
                                        playerMove(
                                            gameState.positionsWhite.indexOf(moveIndex),
                                            currentSquare
                                        )

                                    } // Have Player take move
                                    SquareType.WhitePiece -> if(gameState.turn == Set.WHITE) updateSelected(
                                        currentSquare
                                    ) // Update selectedPiece
                                    else -> {
                                        throw Exception("Should not be clickable")
                                    } // Do nothing when selected
                                }
                            }) {
                            // Only draw Pieces that are not being animated
                            if(!(animState.pieceToAnimate != null &&
                                ((animState.animatePositionStart == currentSquare) ||
                                (animState.animatePositionEnd == currentSquare)))) {
                                // Draw White Pieces
                                if(squareType == SquareType.WhitePiece || squareType == SquareType.CannotMove || squareType == SquareType.CanMove) {
                                    Piece(pieceModel = gameState.piecesWhite[gameState.positionsWhite.indexOf(currentSquare)])
                                }

                                // Draw Black Pieces
                                if(squareType == SquareType.BlackPiece || squareType == SquareType.PossibleCapture) {
                                    Piece(pieceModel = gameState.piecesBlack[gameState.positionsBlack.indexOf(currentSquare)])
                                }
                            }
                        }
                    }
                }
            }
        }

        // If there is a Piece to animate,
        if (animState.pieceToAnimate != null) {
            // DEBUG: Set to true to move to next turn without animating
            val skipAnim = false
            if(skipAnim) { animationEnd() }
            else {
                if(animState.moveIsValid()) {
                    AnimatedChessPiece(piece = animState.pieceToAnimate,
                        squareSizePx = squareSizePx.value,
                        from = animState.animatePositionStart,
                        to = animState.animatePositionEnd,
                        animationEnd = animationEnd)
                } else {
                    throw Exception("Invalid move")
                }
            }
        }
    }
}

// The visual representation of a chess Piece
@Composable
fun Piece(pieceModel: Piece) {
    Icon(
        painter = painterResource(id = pieceModel.asset),
        tint = Color.Unspecified,
        contentDescription = pieceModel.asset.toString()
    )
}

// A chess Piece that is being animated
@Composable
fun AnimatedChessPiece(
    piece: Piece,
    squareSizePx: IntSize,
    from: Pair<Int, Int>,
    to: Pair<Int, Int>,
    animationEnd: () -> Unit
) {
    val offsetY = remember(from) { Animatable(from.first.toFloat()) }
    val offsetX = remember(from) { Animatable(from.second.toFloat()) }

    val squareSizeDp = with(LocalDensity.current) {
        DpSize(
            width = squareSizePx.width.toDp(),
            height = squareSizePx.height.toDp()
        )
    }

    LaunchedEffect(from) {
        val yAnim = launch {
            offsetY.animateTo(to.first.toFloat(), animationSpec = tween(500))
        }
        val xAnim = launch {
            offsetX.animateTo(to.second.toFloat(), animationSpec = tween(500))
        }
        joinAll(yAnim, xAnim)
        animationEnd()
    }

    // Draw a box with a Piece inside it
    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    (offsetX.value * squareSizePx.width).roundToInt(),
                    (offsetY.value * squareSizePx.height).roundToInt()
                )
            }
            .size(squareSizeDp)
            .zIndex(1f)
            .border(width = 1.dp, color = Color.Red)
    ) {
        Piece(pieceModel = piece)
    }
}

// Creates a popup window with the specified dismiss action, and content
@Composable
fun PopupWindow(onDismiss: (Boolean) -> Unit, content: @Composable () -> Unit) {
    // Card height, Card corner roundness, and content padding values are static
    val height = 200.dp
    val cornerRoundness = 25.dp
    val contentPadding = 15.dp

    // Create a dialog, call onDismiss when the dialog is dismissed by the user
    Dialog(onDismissRequest = { onDismiss(false) }) {
        // Create a rounded Card
        Card(
            modifier = Modifier.fillMaxWidth().height(height),
            shape = RoundedCornerShape(cornerRoundness),
        ) {
            // Show the given content in a column
            Column(
                modifier = Modifier.fillMaxSize()
                    .padding(contentPadding)
                    .wrapContentHeight(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                content()
            }
        }
    }
}