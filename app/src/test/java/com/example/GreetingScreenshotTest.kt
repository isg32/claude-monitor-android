package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_screenshot() {
    composeTestRule.setContent {
      MyApplicationTheme {
        androidx.compose.material3.Surface(
          modifier = androidx.compose.ui.Modifier.fillMaxSize(),
          color = androidx.compose.material3.MaterialTheme.colorScheme.background
        ) {
          androidx.compose.foundation.layout.Column(
            modifier = androidx.compose.ui.Modifier
              .fillMaxSize()
              .padding(24.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp),
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
          ) {
            // Header
            androidx.compose.foundation.layout.Row(
              verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
              horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
            ) {
              androidx.compose.material3.Icon(
                imageVector = androidx.compose.material3. someIcon(), // wait, we can just use Icons.Default.Terminal
                contentDescription = "Terminal",
                tint = androidx.compose.material3.MaterialTheme.colorScheme.primary
              )
              androidx.compose.material3.Text(
                text = "Claude Monitor",
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                fontSize = 24.sp
              )
            }
            
            // Stats Card
            androidx.compose.material3.Card(
              modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
              shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
            ) {
              androidx.compose.foundation.layout.Column(
                modifier = androidx.compose.ui.Modifier.padding(16.dp)
              ) {
                androidx.compose.material3.Text(
                  text = "ESTIMATED MONTHLY COST",
                  fontSize = 10.sp,
                  fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                  color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                androidx.compose.material3.Text(
                  text = "$124.50",
                  fontSize = 32.sp,
                  fontWeight = androidx.compose.ui.text.font.FontWeight.Black,
                  color = androidx.compose.material3.MaterialTheme.colorScheme.primary
                )
              }
            }
          }
        }
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}

// Simple helper to avoid import complexity
@androidx.compose.runtime.Composable
private fun someIcon() = androidx.compose.material.icons.Icons.Default.Terminal
