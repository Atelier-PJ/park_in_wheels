package com.parkonwheels.demo

import android.app.Application
import android.app.Instrumentation
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import ai.pleos.playground.stt.SpeechToText
import ai.pleos.playground.stt.constant.Mode
import ai.pleos.playground.stt.constant.ErrorType
import ai.pleos.playground.stt.listener.OnInitializeListener
import ai.pleos.playground.stt.listener.ResultListener
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/** Real emulator STT PCM + real vehicle light gateway + production Compose stamp screen. */
class PleosStarlightJourneyVerification:Instrumentation() {
    override fun onCreate(arguments:Bundle?){super.onCreate(arguments);start()}
    override fun onStart() {
        val evidence=StringBuilder();var success=false
        val accessibilityInfo=uiAutomation.serviceInfo
        accessibilityInfo.flags=accessibilityInfo.flags or android.accessibilityservice.AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        uiAutomation.serviceInfo=accessibilityInfo
        var model:SafariModel?=null;var voice:VoiceDirector?=null
        var gateway:AppVehicleGateway?=null;var sdk:SpeechToText?=null;var listener:ResultListener?=null
        var lifecycle:Application.ActivityLifecycleCallbacks?=null
        val pref="starlight_real_emulator_${System.nanoTime()}"
        try {
            check(Build.HARDWARE.contains("ranchu") || Build.MODEL.contains("emulator",true)){"Emulator only"}
            val app=targetContext.applicationContext as Application
            val shownActivity=AtomicReference<MainActivity?>(null)
            fun show(activity:MainActivity) {
                activity.setContent { ParkTheme { StoryJourneyScreen(model!!,voice!!,onPassport={},onCancel={}) } }
            }
            lifecycle=object:Application.ActivityLifecycleCallbacks {
                override fun onActivityResumed(a:android.app.Activity){if(a is MainActivity){shownActivity.set(a);if(model!=null && voice!=null)show(a)}}
                override fun onActivityCreated(a:android.app.Activity,b:Bundle?)=Unit
                override fun onActivityStarted(a:android.app.Activity)=Unit
                override fun onActivityPaused(a:android.app.Activity)=Unit
                override fun onActivityStopped(a:android.app.Activity)=Unit
                override fun onActivitySaveInstanceState(a:android.app.Activity,b:Bundle)=Unit
                override fun onActivityDestroyed(a:android.app.Activity)=Unit
            }
            app.registerActivityLifecycleCallbacks(lifecycle!!)
            val activity=startActivitySync(Intent(targetContext,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as MainActivity
            val lightStatus=AtomicReference("")
            val starRequested=CountDownLatch(1);val starReadback=CountDownLatch(1)
            runOnMainSync {
                val real=createVehicleGateway(app){evidence.append("VEHICLE $it\n")}
                gateway=object:AppVehicleGateway by real {
                    override fun ambientColor(color:Int?,report:(String)->Unit) {
                        if(color==SafariModel.STARLIGHT_RGB){evidence.append("STARLIGHT_REQUEST rgb=#FFE5A1\n");starRequested.countDown()}
                        real.ambientColor(color){status->
                            lightStatus.set(status);evidence.append("LIGHT $status\n");report(status)
                            if(color==SafariModel.STARLIGHT_RGB && status.startsWith("테마 앰비언트 색 적용 완료"))starReadback.countDown()
                        }
                    }
                }
                model=SafariModel(app,gateway,pref)
                voice=VoiceDirector(activity){model!!.canPlay}

                model!!.foreground()
            }
            val availableDeadline=System.currentTimeMillis()+20000
            var available=false
            while(!available && System.currentTimeMillis()<availableDeadline){runOnMainSync{available=model!!.canPlay};Thread.sleep(150)}
            check(available){"Fresh actual parked P/UX/odometer unavailable"}
            evidence.append("PASS fresh real vehicle P/UX/distance\n")
            runOnMainSync {
                model!!.chooseTheme("magic");model!!.changeAmbient(true);model!!.chooseMode(ExperienceMode.PARKED);model!!.start()
                repeat(3){model!!.answer(0);model!!.nextMission()}
                check(model!!.canFinish && model!!.stamps.isEmpty())
            }
            Thread.sleep(2000)
            runOnMainSync{show(shownActivity.get() ?: activity)}
            Thread.sleep(4500)
            fun capture(name:String) {
                val screenshot=uiAutomation.takeScreenshot()
                java.io.FileOutputStream(java.io.File(targetContext.filesDir,name)).use { screenshot.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) }
            }
            capture("starlight-before.png")
            val initialized=CountDownLatch(1);val finalResult=CountDownLatch(1);val failure=AtomicReference<String?>(null)
            runOnMainSync {
                sdk=SpeechToText(targetContext,Mode.ON_DEVICE)
                sdk!!.initialize(object:OnInitializeListener{
                    override fun onSuccess(){sdk!!.setLocale(Locale.KOREA);initialized.countDown()}
                    override fun onFailure(errorType:ErrorType){failure.set(errorType.name);initialized.countDown()}
                })
            }
            check(initialized.await(15,TimeUnit.SECONDS) && failure.get()==null){"STT initialization failed: ${failure.get()}"}
            listener=object:ResultListener {
                override fun onReady()=Unit
                override fun onStartedRecognition()=Unit
                override fun onUpdated(stt:String,completed:Boolean){
                    if(!completed)return
                    val matched=stt.replace(Regex("[^\\p{L}\\p{N}]"),"")=="별빛을밝혀라"
                    if(!matched){failure.set("Final phrase mismatch");finalResult.countDown();return}
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        if(model!!.canFinish){
                            evidence.append("PASS real STT final matched authored spell\n")
                            model!!.completeRecognizedFinale()
                            evidence.append("STAMP_COUNT ${model!!.stamps.size}\n")
                        }
                        finalResult.countDown()
                    }
                }
                override fun onUpdatedEpdData(on:Long,off:Long)=Unit
                override fun onUpdatedRms(rms:Double)=Unit
                override fun onEndedRecognition()=Unit
                override fun onError(errorType:ErrorType){failure.set(errorType.name);finalResult.countDown()}
            }
            runOnMainSync{sdk!!.addListener(listener!!)}
            val pcm=context.assets.open("spell_ko_16k.pcm").use{it.readBytes()}
            evidence.append("INPUT generated Korean test PCM, not live child microphone\n")
            pcm.asList().chunked(3200).forEach{chunk->runOnMainSync{sdk!!.sendAudio(chunk.toByteArray())};Thread.sleep(100)}
            runOnMainSync{sdk!!.completeAudioSend()}
            check(finalResult.await(20,TimeUnit.SECONDS) && failure.get()==null){"STT final failed: ${failure.get()}"}
            check(starRequested.await(1,TimeUnit.SECONDS)){"No real light request"}
            val changed=starReadback.await(7,TimeUnit.SECONDS)
            runOnMainSync{check(model!!.stamps.size==1){"Expected one stamp"}}
            evidence.append(if(changed)"PASS real Pleos ambient RGB write AND readback confirmed #FFE5A1\n" else "LIGHT_NOT_CONFIRMED ${lightStatus.get()}\n")
            capture("starlight-stamp.png")
            // AAOS accessibility may expose only the driving panel. Verify the real app window
            // and completion state, then retain the captured screenshot for visual review.
            runOnMainSync {
                val visibleActivity=shownActivity.get() ?: activity
                check(visibleActivity.window.decorView.isShown && visibleActivity.window.decorView.width>0)
                check(model!!.state.phase==com.parkonwheels.core.SafariEngine.Phase.COMPLETE)
            }
            evidence.append("PASS production Compose stamp ending captured in visible app window; isolated test passport\n")
            Thread.sleep(30000) // Allow recording and visual inspection of real console color before restoration.
            success=changed
        } catch(t:Throwable){evidence.append("FAIL ${t.stackTraceToString()}\n")}
        finally {
            runOnMainSync{
                listener?.let{runCatching{sdk?.removeListener(it)}};runCatching{sdk?.stop()};runCatching{sdk?.release()}
                lifecycle?.let{(targetContext.applicationContext as Application).unregisterActivityLifecycleCallbacks(it)}
                voice?.close()
                if(model?.canPlay==true)model?.changeAmbient(false)
            }
            Thread.sleep(1500)
            runOnMainSync{model?.background();gateway?.close();targetContext.getSharedPreferences(pref,0).edit().clear().commit()}
        }
        finish(if(success)-1 else 0,Bundle().apply{putString("stream",evidence.toString())})
    }
}
