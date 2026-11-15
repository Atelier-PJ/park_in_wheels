package com.parkonwheels.demo

import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.drawable.Animatable2
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** App-window boarding scene. Never draws into the system Driving view. */
@Composable
internal fun VehicleBoardingScreen(model: SafariModel, onBack: () -> Unit, onStart: () -> Unit) {
    BackHandler(onBack = onBack)
    val theme = model.theme
    val vehicle = when (theme.id) {
        "magic" -> "마법차"
        "space" -> "우주선"
        "ocean" -> "잠수함"
        else -> "탐험차"
    }
    var ready by remember(theme.id, model.reducedMotion) { mutableStateOf(model.reducedMotion) }
    Box(Modifier.fillMaxSize().background(theme.pale)) {
        Box(Modifier.fillMaxSize().semantics { contentDescription = "${theme.name}, 우리 차가 $vehicle 모습으로 변신하는 장면" }) {
            VehicleTransformation(theme.id, model.reducedMotion) { ready = true }
        }
        Box(Modifier.align(Alignment.TopCenter).fillMaxWidth().height(100.dp).background(
            androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color(0xFFFFFCF5).copy(alpha=.85f),Color.Transparent))))
        Column(Modifier.align(Alignment.TopCenter).fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(if (ready) "$vehicle 준비 완료!" else "우리 차가 변신해요!", fontSize = 25.sp, color = Color(0xFF24394A), modifier = Modifier.weight(1f))
                TextButton(onClick = onBack, modifier = Modifier.heightIn(min = 48.dp)) { Text("← 홈으로", fontSize = 17.sp, color = Color(0xFF24394A)) }
            }
            if(theme.id=="space" && model.chargingNow && model.chargingEnabled) Text("충전소 특별 모험 · 코코의 우주정거장",fontSize=17.sp,color=Color(0xFF24394A),modifier=Modifier.padding(horizontal=24.dp))
        }
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(110.dp).background(
            androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color.Transparent,Color(0xFF172C32).copy(alpha=.35f)))))
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().heightIn(min = 82.dp).padding(horizontal = 16.dp, vertical = 12.dp), contentAlignment = Alignment.Center) {
            if (ready) Action(
                if (model.inJourney) "이야기 이어서 듣기" else "어디로 가볼까?",
                enabled = model.hasDistance,
                modifier = Modifier.widthIn(min = 230.dp),
                onClick = onStart,
            ) else Text("$vehicle 준비 중…", fontSize = 18.sp, color = Color.White)
        }
    }
}

@Composable
private fun VehicleTransformation(themeId: String, reducedMotion: Boolean, onReady: () -> Unit) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val readyCallback by rememberUpdatedState(onReady)
    val still by produceState<android.graphics.Bitmap?>(null, themeId) {
        value = withContext(Dispatchers.IO) { context.assets.open("vehicles/${themeId}_ready.webp").use { BitmapFactory.decodeStream(it) } }
    }
    var animationFailed by remember(themeId) { mutableStateOf(false) }
    val animation by produceState<AnimatedImageDrawable?>(null, themeId, reducedMotion) {
        if (!reducedMotion) {
            val decoded = withContext(Dispatchers.IO) { runCatching { ImageDecoder.decodeDrawable(ImageDecoder.createSource(context.assets, "vehicles/${themeId}_transform.gif")) as AnimatedImageDrawable } }
            animationFailed = decoded.isFailure
            value = decoded.getOrNull()
        }
    }
    LaunchedEffect(reducedMotion, still, animationFailed) {
        if ((reducedMotion || animationFailed) && still != null) readyCallback()
    }
    val drawable = animation
    if (reducedMotion || animationFailed) {
        still?.let { Image(it.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
    } else if (drawable != null) {
        DisposableEffect(drawable, owner) {
            drawable.repeatCount = 0
            val callback = object : Animatable2.AnimationCallback() {
                override fun onAnimationEnd(drawable: Drawable?) { readyCallback() }
            }
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_STOP) drawable.stop()
                if (event == Lifecycle.Event.ON_START) drawable.start()
            }
            drawable.registerAnimationCallback(callback)
            owner.lifecycle.addObserver(observer)
            if (owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) drawable.start()
            onDispose {
                owner.lifecycle.removeObserver(observer)
                drawable.unregisterAnimationCallback(callback)
                drawable.stop()
            }
        }
        AndroidView(factory = { ImageView(it).apply { scaleType = ImageView.ScaleType.CENTER_CROP; setImageDrawable(drawable) } }, update = { it.setImageDrawable(drawable) }, modifier = Modifier.fillMaxSize())
    }
}
