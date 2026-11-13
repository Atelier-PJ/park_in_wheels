package com.parkonwheels.demo

import android.app.Instrumentation
import android.os.Build
import android.os.Bundle
import ai.pleos.playground.stt.SpeechToText
import ai.pleos.playground.stt.constant.Mode
import ai.pleos.playground.stt.constant.ErrorType
import ai.pleos.playground.stt.listener.OnInitializeListener
import ai.pleos.playground.stt.listener.ResultListener
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/** Emulator-only real SDK test using generated test PCM, never fabricated recognition results. */
class PleosSpeechVerification:Instrumentation() {
    override fun onCreate(arguments:Bundle?){super.onCreate(arguments);start()}
    override fun onStart() {
        val result=Bundle();val evidence=StringBuilder();var sdk:SpeechToText?=null;var listener:ResultListener?=null
        var success=false
        try {
            check(Build.FINGERPRINT.contains("generic") || Build.MODEL.contains("emulator",true) || Build.HARDWARE.contains("ranchu")){"Emulator-only verification"}
            val initialized=CountDownLatch(1);val failure=AtomicReference<String?>(null)
            runOnMainSync {
                sdk=SpeechToText(targetContext,Mode.ON_DEVICE)
                sdk!!.initialize(object:OnInitializeListener {
                    override fun onSuccess(){sdk!!.setLocale(Locale.KOREA);initialized.countDown()}
                    override fun onFailure(errorType:ErrorType){failure.set(errorType.name);initialized.countDown()}
                })
            }
            check(initialized.await(15,TimeUnit.SECONDS)){"Initialization timed out"};check(failure.get()==null){"Initialization: ${failure.get()}"}
            evidence.append("PASS initialize ON_DEVICE + ko-KR\n")
            val recognized=AtomicReference<String?>(null);val complete=CountDownLatch(1)
            listener=object:ResultListener {
                override fun onReady()=Unit
                override fun onStartedRecognition()=Unit
                override fun onUpdated(stt:String,completed:Boolean){if(completed){recognized.set(stt);complete.countDown()}}
                override fun onUpdatedEpdData(on:Long,off:Long)=Unit
                override fun onUpdatedRms(rms:Double)=Unit
                override fun onEndedRecognition()=Unit
                override fun onError(errorType:ErrorType){failure.set(errorType.name);complete.countDown()}
            }
            runOnMainSync{sdk!!.addListener(listener!!)}
            val pcm=context.assets.open("spell_ko_16k.pcm").use{it.readBytes()}
            pcm.asList().chunked(3200).forEach{chunk->runOnMainSync{sdk!!.sendAudio(chunk.toByteArray())};Thread.sleep(100)}
            runOnMainSync{sdk!!.completeAudioSend()}
            check(complete.await(20,TimeUnit.SECONDS)){"No final PCM result"};check(failure.get()==null){"Recognition: ${failure.get()}"}
            val match=recognized.get()?.replace(Regex("[^\\p{L}\\p{N}]"),"")=="별빛을밝혀라"
            evidence.append("finalResultReceived=${recognized.get()!=null}; expectedPhraseMatched=$match\n")
            check(match){"Generated PCM was not recognized as the expected phrase; raw transcript deliberately omitted"}
            runOnMainSync { StarlightVerification.verifyMatchedPhrase(targetContext.applicationContext as android.app.Application) }
            evidence.append("PASS real SDK matched PCM → model starlight request + one stamp (fake light gateway)\n")
            evidence.append("PASS real Pleos SDK PCM recognition; not a live microphone / child speech test\n");success=true
        } catch(t:Throwable){evidence.append("FAIL ${t.message}\n")}
        finally{runOnMainSync{listener?.let{runCatching{sdk?.removeListener(it)}};runCatching{sdk?.stop()};runCatching{sdk?.release()}}}
        result.putString("stream",evidence.toString());finish(if(success)-1 else 0,result)
    }
}
