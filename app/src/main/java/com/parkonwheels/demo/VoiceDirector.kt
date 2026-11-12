package com.parkonwheels.demo

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.parkonwheels.core.VoiceChoiceParser
import java.util.Locale

/** Foreground, parked-only voice interaction. Never falls back to a network recognizer. */
class VoiceDirector(private val context: Context, private val allowed: () -> Boolean) {
    private val main = Handler(Looper.getMainLooper())
    private val audio = context.getSystemService(AudioManager::class.java)
    private var epoch = 0L
    private var closed = false
    private var focusHeld = false
    private var recognizer: SpeechRecognizer? = null
    private var callback: ((Int) -> Unit)? = null
    private var choices = emptyList<String>()
    private var narration = ""
    private var withMicrophone = false
    private var guard: Runnable? = null
    var enabled by mutableStateOf(false); private set
    var ready by mutableStateOf(false); private set
    var status by mutableStateOf("한국어 음성 안내 확인 중"); private set
    val onDeviceAvailable get() = Build.VERSION.SDK_INT >= 31 && SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
    private val focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
        .setOnAudioFocusChangeListener({ value -> if (value != AudioManager.AUDIOFOCUS_GAIN) stop("다른 안내가 먼저예요. 다시 시작하려면 음성 버튼을 눌러 주세요.") }, main)
        .setAcceptsDelayedFocusGain(false).build()
    private val tts = TextToSpeech(context) { code -> main.post {
        if (!closed && code == TextToSpeech.SUCCESS) {
            val voice = ttsVoices()
            ready = voice != null
            android.util.Log.i("ParkOnWheels", "voice offlineTts=$ready · onDeviceStt=$onDeviceAvailable")
            if (voice != null) {
                speech().voice = voice
                speech().setSpeechRate(.85f)
                status = if (onDeviceAvailable) "기기 안에서 듣고 대답할 수 있어요" else "선택지 읽기 가능 · 기기 내 음성 인식은 미지원"
            } else status = "기기에 한국어 오프라인 음성이 없어요. 터치로 계속할 수 있어요."
        } else if (!closed) status = "음성 안내를 지원하지 않는 기기예요. 터치로 계속해 주세요."
    } }
    // Avoid accessing the constructor property before the asynchronous TTS initializer returns.
    private fun speech(): TextToSpeech = tts
    private fun ttsVoices() = runCatching { speech().voices?.firstOrNull { it.locale.language == Locale.KOREAN.language && !it.isNetworkConnectionRequired } }.getOrNull()
    init {
        tts.setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) { }
            override fun onError(id: String?) { main.post { if (id == epoch.toString()) stop("안내를 읽지 못했어요. 터치로 골라 주세요.") } }
            override fun onDone(id: String?) { main.post {
                if (id == epoch.toString() && enabled && allowed()) {
                    if (withMicrophone) listen(epoch) else releaseFocus()
                }
            } }
        })
    }
    fun start(microphone: Boolean) {
        if (!allowed()) { status = "안전한 주차 상태에서만 음성을 사용할 수 있어요"; return }
        if (!ready) return
        if (microphone && (!onDeviceAvailable || context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)) {
            status = "기기 내 음성 인식과 마이크 허용이 필요해요. 선택지는 읽어 줄 수 있어요."; return
        }
        withMicrophone = microphone; enabled = true
    }
    fun present(prompt: String, options: List<String>, action: (Int) -> Unit) {
        if (!enabled || !allowed()) { if(enabled) stop(); return }
        epoch++; recognizer?.cancel(); guard?.let(main::removeCallbacks); callback = action; choices = options
        narration = prompt + if (options.isEmpty()) "" else options.mapIndexed { i, text -> " ${if(i == 0) "첫 번째" else "두 번째"}, $text." }.joinToString("") + " 첫 번째나 두 번째, 또는 선택지 이름으로 말해 주세요."
        speak()
    }
    private fun speak() {
        if (!enabled || !allowed()) { stop(); return }
        if (audio.mode != AudioManager.MODE_NORMAL) { stop("통화가 끝난 뒤 다시 만나요."); return }
        if (audio.requestAudioFocus(focus) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) { stop("다른 차량 안내가 끝난 뒤 음성을 시작해 주세요."); return }
        focusHeld = true
        status = "선택지를 읽어 주고 있어요"
        if (tts.speak(narration, TextToSpeech.QUEUE_FLUSH, null, epoch.toString()) != TextToSpeech.SUCCESS) stop("안내를 읽지 못했어요. 터치로 골라 주세요.")
    }
    private fun listen(token: Long) {
        if (!allowed() || !enabled || token != epoch) { stop(); return }
        if (Build.VERSION.SDK_INT < 31 || !onDeviceAvailable) { stop("기기 내 음성 인식이 없어요. 터치로 골라 주세요."); return }
        try {
            if (recognizer == null) recognizer = SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
            recognizer!!.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) { if(token == epoch) status = "듣고 있어요 · 그만이라고 말하면 멈춰요" }
                override fun onBeginningOfSpeech() { }
                override fun onRmsChanged(rmsdB: Float) { }
                override fun onBufferReceived(buffer: ByteArray?) { } // Deliberately never persist or transmit audio.
                override fun onEndOfSpeech() { }
                override fun onPartialResults(partialResults: Bundle?) { }
                override fun onEvent(eventType: Int, params: Bundle?) { }
                override fun onError(error: Int) { if (token == epoch) stop(when(error) {
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "마이크가 허용되지 않았어요. 터치로 계속할 수 있어요."
                    SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED, SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> "기기에 한국어 음성 인식이 준비되지 않았어요. 터치로 골라 주세요."
                    else -> "잘 듣지 못했어요. 다시 음성을 시작하거나 손으로 골라 주세요."
                }) }
                override fun onResults(results: Bundle?) {
                    if (token != epoch || !enabled || !allowed()) return
                    guard?.let(main::removeCallbacks); recognizer?.cancel(); releaseFocus()
                    val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                    val confidence = results?.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)?.firstOrNull()
                    val command = if (confidence != null && confidence >= 0 && confidence < .55f) VoiceChoiceParser.UNKNOWN else VoiceChoiceParser.parse(text, choices)
                    when (command) {
                        VoiceChoiceParser.STOP -> stop("음성 탐험을 멈췄어요")
                        VoiceChoiceParser.REPEAT -> { epoch++; speak() }
                        VoiceChoiceParser.UNKNOWN -> stop("어떤 선택인지 확실하지 않아요. 다시 말하거나 손으로 골라 주세요.")
                        else -> { status = "선택을 들었어요"; callback?.invoke(command) }
                    }
                }
            })
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR")
                .putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            recognizer!!.startListening(intent)
            guard = Runnable { if (token == epoch) stop("잠깐 쉬어 갈게요. 다시 음성을 시작할 수 있어요.") }.also { main.postDelayed(it, 12_000) }
        } catch (error: Exception) { stop("음성 인식을 시작하지 못했어요. 터치로 골라 주세요.") }
    }
    private fun releaseFocus() { if (focusHeld) { focusHeld = false; audio.abandonAudioFocusRequest(focus) } }
    fun stop(reason: String = "음성이 쉬고 있어요") {
        enabled = false; epoch++; guard?.let(main::removeCallbacks); guard = null
        recognizer?.cancel(); tts.stop(); callback = null; releaseFocus(); status = reason
    }
    fun close() { stop(); closed = true; recognizer?.destroy(); recognizer = null; tts.shutdown(); main.removeCallbacksAndMessages(null) }
}
