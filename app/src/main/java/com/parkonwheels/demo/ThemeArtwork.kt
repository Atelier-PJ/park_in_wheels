package com.parkonwheels.demo

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale

/** Bundled, offline artwork shared by theme selection, journey and passport. */
@Composable
fun ThemeArtwork(
    id: String,
    modifier: Modifier = Modifier,
    celebration: Boolean = false,
    contentScale: ContentScale = ContentScale.Fit,
    alignment: androidx.compose.ui.Alignment = androidx.compose.ui.Alignment.Center,
) {
    val image = when (id) {
        "magic" -> R.drawable.adventure_magic_storybook
        "space" -> R.drawable.adventure_space_storybook
        "ocean" -> R.drawable.adventure_ocean_storybook
        else -> R.drawable.adventure_dino_storybook
    }
    val context=LocalContext.current
    val bitmap by produceState<android.graphics.Bitmap?>(null,image) {
        value=withContext(Dispatchers.IO) {
            android.graphics.ImageDecoder.decodeBitmap(android.graphics.ImageDecoder.createSource(context.resources,image)) { decoder,info,_ ->
                val scale=minOf(1f,1280f/maxOf(info.size.width,info.size.height))
                decoder.setTargetSize((info.size.width*scale).toInt().coerceAtLeast(1),(info.size.height*scale).toInt().coerceAtLeast(1))
            }
        }
    }
    Box(modifier) {
        bitmap?.let { art -> Image(
            bitmap = art.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.fillMaxSize().testTag("theme-art-$id"),
            contentScale = contentScale,
            alignment = alignment,
        ) }
        if (celebration) CelebrationOverlay(Modifier.fillMaxSize())
    }
}

@Composable
private fun CelebrationOverlay(modifier: Modifier) {
    Canvas(modifier) {
        val scale = minOf(size.width / 600f, size.height / 440f)
        withTransform({
            translate((size.width - 600f * scale) / 2f, (size.height - 440f * scale) / 2f)
            scale(scale, scale, Offset.Zero)
        }) {
            val colors = listOf(Color(0xFFFFC857), Color(0xFFEEA3AF), Color(0xFF83C4B2))
            for (i in 0..12) {
                star(55f + (i * 47 % 500), 42f + (i * 59 % 320), 7f, colors[i % colors.size])
            }
        }
    }
}

private fun DrawScope.star(x: Float, y: Float, radius: Float, color: Color) {
    val path = Path()
    for (i in 0..9) {
        val angle = (i * 36 - 90) * Math.PI / 180
        val distance = if (i % 2 == 0) radius else radius * .46f
        val px = x + kotlin.math.cos(angle).toFloat() * distance
        val py = y + kotlin.math.sin(angle).toFloat() * distance
        if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
    }
    path.close()
    drawPath(path, color)
}
