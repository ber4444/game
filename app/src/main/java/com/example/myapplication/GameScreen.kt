package com.example.myapplication

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.Alignment
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

@Composable
fun GameScreen(
    windowSize: WindowWidthSizeClass,
    viewModel: GameViewModel
) {
    val gameState by viewModel.gameState.collectAsState()
    val animState by viewModel.animState.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.verticalScroll(scrollState)
    ) {
        // TODO [UI FEATURE]: Make into a floating window, more obvious to user and doesn't interrupt Column's flow
        //       Show surviving king of the winning color with the win message
        // Show which Set won the game
        if(gameState.gameEnded == true) {
            Text(
                modifier = Modifier.testTag("winnerText"),
                text = stringResource(R.string.game_end_message, gameState.winner),
                color = Color.Red,
                style = MaterialTheme.typography.titleLarge
            )
        }

        // TODO [UI FEATURE]: Display possible moves for a selected Piece
        // Display the Chess Board
        Board(gameState, animState, windowSize) { viewModel.animationEnd() }

        // Display a spacer
        Spacer(modifier = Modifier.padding(8.dp))

        // Display the AutoPlay mode toggle
        // TODO [UI FEATURE]: Change button color depending on gameState.autoPlay
        // TODO [CLEANUP]: Move "AutoPlay" string to stringResource
        Text("Autoplay is ${if(gameState.autoPlay) "on" else "off"}")
        Button(
            onClick = { viewModel.setAutoPlay(!gameState.autoPlay) },
        ) { Text(text = "AutoPlay", style = MaterialTheme.typography.titleLarge) }

        // Display the 'Move' Button
        Button(
            onClick = {
                viewModel.gameMover()
            },
            // Button is enabled only when game has not ended and it is White's turn
            enabled = !gameState.gameEnded && !gameState.buttonLock
        ) {
            Text(
                text = stringResource(R.string.move_button),
                style = MaterialTheme.typography.titleLarge
            )
        }

        // Display the 'Reset' Button
        Button(
            onClick = {
                viewModel.resetGame()
            }
        ) {
            Text("Reset")
        }
    }
    // If autoplay is on, the game hasn't ended, and there isn't a Piece being moved,
    if(gameState.autoPlay && !gameState.gameEnded && animState.pieceToAnimate == null) {
        // Move a White Piece
        viewModel.gameMover()
    }
}


@Composable
fun RowScope.Square(modifier: Modifier, isDarkSquare: Boolean, content: @Composable ()-> Unit) {
    Box(modifier = modifier
        .weight(1f)
        .aspectRatio(1f)
        .background(
            color = if (isDarkSquare) MaterialTheme.colorScheme.secondary
            else Color.White
        ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
fun Board(
    state: GameUiState,
    animState: PieceAnimationState,
    windowSize: WindowWidthSizeClass,
    animationEnd: () -> Unit
) {
    val squareSizePx = remember { mutableStateOf(IntSize.Zero) }

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
            // TODO [CLEANUP]: Use BOARD_SIZE const instead of 8
            // A Column with 8 Rows
            repeat(8) { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    // Each Row has 8 Squares
                    repeat(8) { column ->
                        Square(
                            modifier = Modifier.onGloballyPositioned {
                                // If this position is where an animation will start,
                                if (animState.animatePositionStart[0] == row &&
                                    animState.animatePositionStart[1] == column) {

                                    // Save the size of the red bounding square for the moving animation
                                    squareSizePx.value = it.size
                                }
                            },
                            (row + column) % 2 == 1) {
                            // Do not draw Pieces that are being animated
                            if(animState.pieceToAnimate != null &&
                                ((animState.animatePositionStart[0] == row &&
                                  animState.animatePositionStart[1] == column) ||
                                        (animState.animatePositionEnd[0] == row &&
                                         animState.animatePositionEnd[1] == column))) {
                                // TODO [EXTRA]: Change color of box?
                            }
                            else {
                                // display a piece on the board if it exists at the given row and column
                                // pair each white piece with its position
                                // find the first pair where the position matches the current row and column
                                // extract the piece from the pair if it exists
                                val pieceWhite = state.piecesWhite.zip(state.positionsWhite)
                                    .firstOrNull { it.second == listOf(row, column) }
                                    ?.first

                                val pieceBlack = state.piecesBlack.zip(state.positionsBlack)
                                    .firstOrNull { it.second == listOf(row, column) }
                                    ?.first

                                // Draw the icon of a White or Black Piece
                                pieceWhite?.let { Piece(pieceModel = it) } ?:
                                pieceBlack?.let { Piece(pieceModel = it) }
                            }
                        }
                    }
                }
            }
        }

        // If there is a Piece to animate,
        if (animState.pieceToAnimate != null) {
            // Animate the given Piece's movement, from -> to
            AnimatedChessPiece(
                piece = animState.pieceToAnimate,
                squareSizePx = squareSizePx.value,
                from = animState.animatePositionStart,
                to = animState.animatePositionEnd,
                animationEnd = animationEnd
            )
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
    from: List<Int>,
    to: List<Int>,
    animationEnd: () -> Unit
) {
    val offsetX = remember(from) { Animatable(from[1].toFloat()) } // pos is row, col
    val offsetY = remember(from) { Animatable(from[0].toFloat()) }

    val squareSizeDp = with(LocalDensity.current) {
        DpSize(
            width = squareSizePx.width.toDp(),
            height = squareSizePx.height.toDp()
        )
    }

    LaunchedEffect(to) {
        val yAnim = launch {
            offsetY.animateTo(to[0].toFloat(), animationSpec = tween(500))
        }
        val xAnim = launch {
            offsetX.animateTo(to[1].toFloat(), animationSpec = tween(500))
        }
        joinAll(yAnim, xAnim)
        animationEnd()
    }

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
