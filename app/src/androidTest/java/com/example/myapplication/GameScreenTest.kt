package com.example.myapplication

import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
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
            winner = "White"
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
            testGameState.winner
        )
        composeTestRule.onNodeWithText(winnerText).assertIsDisplayed()
    }

}