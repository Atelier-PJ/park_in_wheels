package com.parkonwheels.demo

import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.widget.ImageView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/** Lifecycle-scoped offline animation; HD story scenes keep their full composition. */
@Composable
fun DinoSafariGif(modifier: Modifier = Modifier, themeId: String = "dino", stage: Int? = null, assetName: String? = null, crop: Boolean = false, animate: Boolean = true) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val drawable = remember(context, themeId, stage, assetName) {
        runCatching {
            ImageDecoder.decodeDrawable(
                ImageDecoder.createSource(context.assets, assetName ?: if (stage == null) "${themeId}_storybook.gif" else "hd/${themeId}_${stage.coerceIn(0,3)}.webp"),
            ) as? AnimatedImageDrawable
        }.getOrNull()
    }

    if (drawable == null) {
        Image(
            painter = painterResource(if(assetName=="park_entrance_animated.gif") R.drawable.park_entrance else when(themeId){"magic"->R.drawable.adventure_magic_storybook;"space"->R.drawable.adventure_space_storybook;"ocean"->R.drawable.adventure_ocean_storybook;else->R.drawable.adventure_dino_storybook}),
            contentDescription = null,
            modifier = modifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
        )
        return
    }

    DisposableEffect(drawable, lifecycleOwner, animate) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    if(assetName == "ocean_cleanup.gif") drawable.repeatCount = 0
                    if(animate) drawable.start() else drawable.stop()
                }
                Lifecycle.Event.ON_STOP -> drawable.stop()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if(assetName == "ocean_cleanup.gif") drawable.repeatCount = 0
        if(animate) drawable.start() else drawable.stop()
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            drawable.stop()
        }
    }

    AndroidView(
        factory = { viewContext -> ImageView(viewContext).apply { scaleType = ImageView.ScaleType.FIT_CENTER } },
        update = { it.scaleType=if(crop) ImageView.ScaleType.CENTER_CROP else ImageView.ScaleType.FIT_CENTER; it.setImageDrawable(drawable) },
        modifier = modifier.fillMaxSize(),
    )
}
