package com.parkonwheels.demo

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import ai.pleos.playground.stt.SpeechToText
import ai.pleos.playground.stt.constant.Mode
import ai.pleos.playground.stt.constant.ErrorType
import ai.pleos.playground.stt.listener.OnInitializeListener
import ai.pleos.playground.stt.listener.ResultListener
import java.util.Locale

internal fun createPleosSpeechInput(context: Context): SpeechInput? = PleosSpeechInput(context)

/** Pleos ON_DEVICE only. No server authentication, network fallback, or raw audio logging. */
internal class PleosSpeechInput(
    context: Context,
    private val requestInput: (SpeechToText) -> Unit = { it.request() }
) : SpeechInput {
    private val main=Handler(Looper.getMainLooper())
    private var closed=false
    private var generation=0L
    private var listener: ResultListener?=null
    private var sdk: SpeechToText?=null
    override var ready by mutableStateOf(false); private set
    override var status by mutableStateOf("차량 음성 인식을 연결하고 있어요"); private set
    init {
        try {
            val service=SpeechToText(context.applicationContext,Mode.ON_DEVICE)
            sdk=service
            service.initialize(object:OnInitializeListener {
                override fun onSuccess() { main.post {
                    if(closed)return@post
                    try {
                        service.setLocale(Locale.KOREA)
                        ready=true;status="차량 마이크로 주문을 말할 수 있어요"
                        Log.i("ParkOnWheels","Pleos STT initialized · mode=ON_DEVICE · locale=ko-KR")
                    } catch(e:Exception){ready=false;status="차량 한국어 음성 설정을 확인해 주세요";Log.w("ParkOnWheels","Pleos STT locale setup failed")}
                } }
                override fun onFailure(errorType:ErrorType) { main.post {
                    if(!closed){ready=false;status=message(errorType);Log.w("ParkOnWheels","Pleos STT initialization failed: ${errorType.name}")}
                } }
            })
        } catch(e:Exception){status="차량 음성 서비스를 연결하지 못했어요";Log.w("ParkOnWheels","Pleos STT service unavailable")}
    }
    override fun listen(onListening:()->Unit,onResult:(String)->Unit,onError:(String)->Unit) {
        cancel()
        if(closed || !ready){onError(status);return}
        val token=generation
        var finished=false
        val result=object:ResultListener {
            override fun onReady() { main.post {
                if(!closed && token==generation && !finished){status="주문을 말해 주세요";Log.i("ParkOnWheels","Pleos STT recognition ready");onListening()}
            } }
            override fun onStartedRecognition() { main.post {
                if(!closed && token==generation && !finished){status="듣고 있어요";Log.i("ParkOnWheels","Pleos STT recognition started");onListening()}
            } }
            override fun onUpdated(stt:String,completed:Boolean) { main.post {
                if(closed || token!=generation || finished || !completed)return@post
                finished=true
                Log.i("ParkOnWheels","Pleos STT final result received · nonempty=${stt.isNotBlank()}")
                if(stt.isBlank())onError("주문을 듣지 못했어요. 다시 말해 주세요") else onResult(stt)
            } }
            override fun onUpdatedEpdData(on:Long,off:Long) = Unit
            override fun onUpdatedRms(rms:Double) = Unit
            // End-of-input is not a final result. VoiceDirector owns the bounded timeout;
            // a final result may legitimately arrive after the SDK's end notification.
            override fun onEndedRecognition() = Unit
            override fun onError(errorType:ErrorType) { main.post {
                if(!closed && token==generation && !finished){finished=true;status=message(errorType);Log.w("ParkOnWheels","Pleos STT recognition failed: ${errorType.name}");onError(status)}
            } }
        }
        try {listener=result;sdk!!.addListener(result);requestInput(sdk!!);Log.i("ParkOnWheels","Pleos STT request sent")}
        catch(e:Exception){cancel();onError("차량 마이크를 시작하지 못했어요. 다시 시작해 주세요")}
    }
    override fun cancel() {
        generation++
        val old=listener;listener=null
        if(old!=null){runCatching{sdk?.removeListener(old)};runCatching{sdk?.stop()}}
    }
    override fun close() {if(closed)return;closed=true;cancel();ready=false;runCatching{sdk?.release()};sdk=null}
    private fun message(error:ErrorType):String = when(error.name){
        "PERMISSION_DENIED"->"차량 음성 인식 권한을 허용해 주세요"
        "MIC_ERROR"->"차량 마이크와 마이크 차단 설정을 확인해 주세요"
        "UNAVAILABLE_LANGUAGE"->"차량의 한국어 음성 인식을 확인해 주세요"
        "ON_DEVICE_ENGINE_ERROR","NOT_SUPPORTED_API"->"이 차량의 온디바이스 음성 인식을 확인해 주세요"
        "TIMEOUT"->"주문을 듣지 못했어요. 다시 말해 주세요"
        else->"차량 음성 연결을 확인한 뒤 다시 시작해 주세요"
    }
}
