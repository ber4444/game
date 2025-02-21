package com.example.myapplication

import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
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

        composeTestRule.onNodeWithText(King(Set.WHITE).symbol).assertIsDisplayed()
    }

    @Test
    fun testGameOver() {
        // TODO
        // move_button disabled
        // game_end_message displayed
    }

}