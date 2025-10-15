package com.example.myapplication

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.device.DeviceInteraction.Companion.setScreenOrientation
import androidx.test.espresso.device.EspressoDevice.Companion.onDevice
import androidx.test.espresso.device.action.ScreenOrientation
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import com.example.myapplication.ui.theme.MyApplicationTheme
import junit.framework.TestCase.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@RunWith(AndroidJUnit4::class)
class GameScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testButtonClickAndPieceDisplayed() {
        composeTestRule.setContent {
            MyApplicationTheme {
                GameScreen(WindowWidthSizeClass.Medium, GameViewModel())
            }
        }

        composeTestRule.onNodeWithText("Move").performClick()

        composeTestRule.onNodeWithContentDescription(King(Set.WHITE).asset.toString()).assertIsDisplayed()
    }

    @Test
    fun testGameOver() {
        val testGameState = GameUiState(
            winState = WinState.WHITE
        )

        composeTestRule.setContent {
            MyApplicationTheme {
                GameScreen(
                    WindowWidthSizeClass.Medium,
                    GameViewModel(
                        testGameState
                    )
                )
            }
        }

        val winnerText = getInstrumentation().targetContext.getString(
            R.string.game_end_message_winner,
            testGameState.winState
        )
        composeTestRule.onNodeWithText(winnerText).assertIsDisplayed()
    }

    @Test
    fun testStalemate() {
        // we're just going to fill the board to force a no move scenario
        val sixtyFourWhitePieces = List(64) { King(Set.WHITE) }
        val positions = mutableListOf<Pair<Int, Int>>()
        for (i in 0 until BOARD_SIZE) {
            for (j in 0 until BOARD_SIZE) {
                positions.add(Pair(i, j))
            }
        }

        val testGameState = GameUiState(
            piecesWhite = sixtyFourWhitePieces,
            piecesBlack = listOf(King(Set.BLACK)),
            positionsWhite = positions,
            positionsBlack = listOf()
        )

        composeTestRule.setContent {
            MyApplicationTheme {
                GameScreen(
                    WindowWidthSizeClass.Medium,
                    GameViewModel(
                        testGameState
                    )
                )
            }
        }

        composeTestRule.onNodeWithText("Move").performClick()

        val winnerText = getInstrumentation().targetContext.getString(
            R.string.game_end_message_no_winner,
            WinState.STALEMATE
        )

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule
                .onAllNodesWithTag("winnerText", useUnmergedTree = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        composeTestRule.waitUntil(timeoutMillis = 5000) {
            val node = composeTestRule
                .onNodeWithTag("winnerText", useUnmergedTree = true)
                .fetchSemanticsNode()

            val isVisible = node.layoutInfo.width > 0f && node.layoutInfo.height > 0f
            val text = node.config.getOrNull(SemanticsProperties.Text)?.firstOrNull()?.text

            isVisible && !text.isNullOrBlank()
        }

        composeTestRule.onNodeWithText(winnerText).assertIsDisplayed()
    }

    @Test
    fun chessBoardInPortrait_shouldBeVisible() {
        assertChessboardVisibleIn(ScreenOrientation.PORTRAIT)
    }

    @Test
    fun chessBoardInLandscape_shouldBeVisible() {
        assertChessboardVisibleIn(ScreenOrientation.LANDSCAPE)
    }

    private fun assertChessboardVisibleIn(orientation: ScreenOrientation) {
        onDevice().setScreenOrientation(orientation)
        composeTestRule.waitForIdle()

        // Get status bar height from system resources. Used as reference for layout visibility test.
        val context = getInstrumentation().targetContext
        val statusBarHeightPx = context.resources.getIdentifier("status_bar_height", "dimen", "android")
            .takeIf { it > 0 }
            ?.let { context.resources.getDimensionPixelSize(it) }
            ?: 0

        composeTestRule.setContent {
            MyApplicationTheme {
                // Without Surface(fillMaxSize), layout measurements will be incorrect in tests.
                // This matches the real app structure and ensures chessboard Y-position is accurate.
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    GameScreen(
                        windowSize = WindowWidthSizeClass.Medium,
                        viewModel = GameViewModel(
                            gameState = GameUiState(
                                piecesWhite = List(64) { King(Set.WHITE) },
                                piecesBlack = listOf(King(Set.BLACK)),
                                positionsWhite = mutableListOf(),
                                positionsBlack = listOf()
                            )
                        )
                    )
                }
            }
        }

        composeTestRule.waitForIdle()

        // Get the top Y-position of the chessboard relative to the root layout
        val chessboardTop = composeTestRule
            .onNodeWithTag("chess_board")
            .fetchSemanticsNode()
            .boundsInRoot.top

        assertTrue(
            "Expected chessboard to be below status bar (≥ $statusBarHeightPx px), but was at $chessboardTop px",
            chessboardTop >= statusBarHeightPx
        )
    }
}
