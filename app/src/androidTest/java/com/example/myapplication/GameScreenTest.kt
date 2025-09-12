package com.example.myapplication

import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import com.example.myapplication.ui.theme.MyApplicationTheme
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Rule

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
            gameEnded = true,
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
            R.string.game_end_message,
            testGameState.winState
        )
        composeTestRule.onNodeWithText(winnerText).assertIsDisplayed()
    }

    @Test
    fun testStalemate() {
        // we're just going to fill the board to force a no move scenario
        val sixtyFourWhitePieces = List(64) { King(Set.WHITE) }
        val positions = mutableListOf<List<Int>>()
        for (i in 0..7) {
            for (j in 0..7) {
                positions.add(listOf(i,j))
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
            R.string.game_end_message,
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

}