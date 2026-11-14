package com.parkonwheels.demo

/** Only final results are delivered. Implementations never retain speech or transcripts. */
interface SpeechInput {
    val ready: Boolean
    val status: String
    fun listen(onListening: () -> Unit, onResult: (String) -> Unit, onError: (String) -> Unit)
    fun cancel()
    fun close()
}
