package com.parkonwheels.demo

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer

/** Playback only; VoiceDirector owns the single audio-focus session. */
internal class ThemeSoundPlayer(private val context: Context) {
    private var player: MediaPlayer? = null
    private var theme: String? = null
    val playing get() = player?.isPlaying == true
    private fun open(path: String): MediaPlayer = MediaPlayer().apply {
        setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
        context.assets.openFd(path).use { setDataSource(it.fileDescriptor, it.startOffset, it.length) }
        prepare()
    }
    fun ambience(id: String) {
        if (theme == id && playing) { duck(true); return }
        stop()
        runCatching {
            player = open("sound/${id}_ambience.wav").apply { isLooping = true; setVolume(.15f,.15f); start() }
            theme = id
            android.util.Log.i("ParkOnWheels", "theme_sound ambience=$id playing=$playing ducked=true")
        }.onFailure { stop() }
    }
    fun departure(id: String, done: () -> Unit) {
        stop()
        runCatching {
            player = open("sound/${id}_departure.wav").apply {
                setVolume(.4f,.4f)
                setOnCompletionListener { stop(); done() }
                setOnErrorListener { _,_,_->stop();done();true }
                start()
                android.util.Log.i("ParkOnWheels", "theme_sound departure=$id playing=$isPlaying")
            }
        }.onFailure { stop();done() }
    }
    fun duck(narrating: Boolean) { player?.setVolume(if(narrating).15f else .4f, if(narrating).15f else .4f); if(playing) android.util.Log.i("ParkOnWheels","theme_sound ducked=$narrating") }
    fun stop() { player?.release();player=null;theme=null }
}
