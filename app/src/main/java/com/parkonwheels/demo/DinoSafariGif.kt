package com.parkonwheels.demo

import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Decode bundled art away from the UI/vehicle callback thread, bounded to display size. */
@Composable
fun DinoSafariGif(modifier: Modifier = Modifier, themeId: String = "dino", stage: Int? = null, assetName: String? = null, crop: Boolean = false, animate: Boolean = true) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val path = assetName ?: if (stage == null) "${themeId}_storybook.gif" else "hd/${themeId}_${stage.coerceIn(0,3)}.webp"
    val maxEdge = minOf(context.resources.displayMetrics.widthPixels, 1280).coerceAtLeast(320)
    val drawable by produceState<Drawable?>(null, context, path, maxEdge) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                ImageDecoder.decodeDrawable(ImageDecoder.createSource(context.assets, path)) { decoder, info, _ ->
                    val scale = minOf(1f, maxEdge.toFloat() / maxOf(info.size.width, info.size.height))
                    decoder.setTargetSize((info.size.width * scale).toInt().coerceAtLeast(1), (info.size.height * scale).toInt().coerceAtLeast(1))
                }
            }.getOrNull()
        }
    }
    val animation = drawable as? AnimatedImageDrawable
    DisposableEffect(animation, lifecycleOwner, animate) {
        fun update() {
            if(assetName == "ocean_cleanup.gif") animation?.repeatCount = 0
            if(animate && lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) animation?.start() else animation?.stop()
        }
        val observer = LifecycleEventObserver { _, _ -> update() }
        lifecycleOwner.lifecycle.addObserver(observer)
        update()
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer); animation?.stop() }
    }
    Box(modifier.fillMaxSize()) {
        AndroidView(
            factory = { ImageView(it) },
            update = {
                it.scaleType = if(crop) ImageView.ScaleType.CENTER_CROP else ImageView.ScaleType.FIT_CENTER
                if(it.drawable !== drawable) it.setImageDrawable(drawable)
            },
            modifier = Modifier.fillMaxSize(),
            onRelease = { animation?.stop(); it.setImageDrawable(null) }
        )
    }
}
