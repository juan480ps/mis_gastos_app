package com.uaa.misgastosapp.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.uaa.misgastosapp.ui.theme.GastosTheme
import org.junit.Rule
import org.junit.Test

/**
 * Tests E2E para la pantalla principal.
 * Sin Hilt - usa DI manual.
 */
class HomeScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun homeScreen_displaysGreeting() {
        composeTestRule.setContent {
            GastosTheme {
                // HomeScreen would be tested here
            }
        }
    }

    @Test
    fun homeScreen_displaysBalance() {
        composeTestRule.setContent {
            GastosTheme {
                // HomeScreen would be tested here
            }
        }
    }
}
