package com.parkonwheels.demo

import android.app.Instrumentation
import android.content.Intent
import android.graphics.Bitmap
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.io.File
import java.io.FileOutputStream

/** Renders real Compose surfaces at narrow/wide constraints; uses only isolated test-model data. */
fun verifyAdaptiveLayouts(runner: Instrumentation, model: SafariModel) {
    val activity = runner.startActivitySync(Intent(runner.targetContext, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as ComponentActivity
    for (width in listOf(420, 1180)) {
        runner.runOnMainSync {
            activity.setContent {
                ParkTheme { Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) { Box(Modifier.width(width.dp).height(800.dp)) { TicketScreen(model, {}, {}) } } }
            }
        }
        runner.waitForIdleSync(); SystemClock.sleep(350)
        val screenshot = runner.uiAutomation.takeScreenshot()
        check(screenshot != null) { "Adaptive screenshot unavailable" }
        FileOutputStream(File(runner.targetContext.filesDir,"qa-ticket-${width}.png")).use { screenshot.compress(Bitmap.CompressFormat.PNG,100,it) }
        screenshot.recycle()
    }
    runner.runOnMainSync { activity.finish() }
}
