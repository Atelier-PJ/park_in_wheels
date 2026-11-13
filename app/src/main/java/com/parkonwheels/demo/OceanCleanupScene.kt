package com.parkonwheels.demo

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext

/** Before a choice the paper remains on the sand; after a choice play cleanup once. */
@Composable
internal fun OceanCleanupScene(completed: Boolean, reducedMotion: Boolean, modifier: Modifier = Modifier) {
    if (completed && !reducedMotion) {
        DinoSafariGif(modifier, assetName = "ocean_cleanup.gif")
    } else {
        val context = LocalContext.current
        val bitmap = remember(context, completed) {
            context.assets.open(if(completed) "ocean_cleanup_after.png" else "ocean_cleanup_before.png").use {
                BitmapFactory.decodeStream(it).asImageBitmap()
            }
        }
        Image(bitmap, if(completed) "종이가 수거함에 담기고 바다가 깨끗해졌어요" else "모래 위의 종이를 발견했어요", modifier.fillMaxSize(), contentScale = ContentScale.Fit)
    }
}
