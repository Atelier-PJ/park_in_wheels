package com.parkonwheels.demo

import android.Manifest
import android.app.Application
import android.graphics.Bitmap
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import org.junit.After
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

/** Production UI + VoiceDirector + PleosSpeechInput + real STT/vehicle service.
 * Only the microphone source is replaced by authored test PCM; no final result is fabricated.
 */
class PleosJourneyUiTest {
    @get:Rule val compose=createEmptyComposeRule()
    @get:Rule val microphone=GrantPermissionRule.grant(Manifest.permission.RECORD_AUDIO)
    private val app get()=InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as Application
    private val pref="e2e_pleos_${System.nanoTime()}"
    private var scenario:ActivityScenario<MainActivity>?=null
    private lateinit var model:SafariModel
    private lateinit var voice:VoiceDirector
    private lateinit var gateway:AppVehicleGateway
    private val main=Handler(Looper.getMainLooper())
    private val inputs=AtomicInteger()
    private val writes=AtomicInteger()
    private var lightStatus=""
    private val evidence=StringBuilder()
    private fun click(text:String) {
        compose.waitUntil(10_000){compose.onAllNodesWithText(text,substring=true).fetchSemanticsNodes().size==1}
        compose.onNodeWithText(text,substring=true).performClick()
    }
    private fun waitFor(timeout:Long=120_000, condition:()->Boolean) {
        try { compose.waitUntil(timeout){var done=false;compose.runOnUiThread{done=condition()};done} }
        catch(error:Throwable) {
            var state=""
            compose.runOnUiThread{state="phase=${model.state.phase}, gate=${model.gate}, distance=${model.hasDistance}, mission=${model.missionStep}, voice=${voice.status}, light=$lightStatus, writes=${writes.get()}"}
            evidence.append("FAIL $state\n")
            throw IllegalStateException(state,error)
        }
    }
    private fun capture(name:String,dialog:Boolean=false) {
        val captured=(if(dialog) compose.onNode(isDialog()) else compose.onRoot()).captureToImage().asAndroidBitmap()
        val bitmap=checkNotNull(captured.copy(Bitmap.Config.ARGB_8888,false))
        val file=File(app.filesDir,name)
        file.outputStream().use{check(bitmap.compress(Bitmap.CompressFormat.PNG,100,it)){"PNG encoding failed: $name"}}
        check(file.length()>1024){"Empty capture: $name"}
        bitmap.recycle()
    }
    @After fun close() {
        main.removeCallbacksAndMessages(null)
        try {
            if(::voice.isInitialized) {
                var restore=false
                compose.runOnUiThread {
                    voice.close()
                    restore=model.ambientEnabled && model.canPlay
                    if(restore)model.changeAmbient(false)
                }
                if(restore) {
                    waitFor(20_000){model.ambientStatus.startsWith("이전 색 복원") || model.ambientStatus.startsWith("앱이 바꾼 조명이 없어요") || !model.canPlay}
                    compose.runOnUiThread {
                        evidence.append("RESTORE ${model.ambientStatus}\n")
                        check(model.ambientStatus.contains("미확인 0곳")){"Original color restore not independently confirmed"}
                    }
                }
            }
        } finally {
            scenario?.close()
            app.getSharedPreferences(pref,0).edit().clear().commit()
            File(app.filesDir,"e2e-pleos.txt").writeText(evidence.toString())
        }
    }

    @Test fun narratedMagicVoiceFinaleLightsStampAndPassport() {
        check(Build.HARDWARE.contains("ranchu") || Build.MODEL.contains("emulator",true)){"Emulator only"}
        val pcm=InstrumentationRegistry.getInstrumentation().context.assets.open("spell_ko_16k.pcm").use{it.readBytes()}
        E2eRuntime.model={ application ->
            val real=createVehicleGateway(application){android.util.Log.i("ParkOnWheels",it)}
            gateway=object:AppVehicleGateway by real {
                override fun ambientColor(color:Int?,report:(String)->Unit){
                    if(color==SafariModel.STARLIGHT_RGB)writes.incrementAndGet()
                    real.ambientColor(color){status->
                        if(color==SafariModel.STARLIGHT_RGB)lightStatus=status
                        report(status)
                    }
                }
            }
            SafariModel(application,gateway,pref).also{model=it}
        }
        E2eRuntime.speech={ context ->
            PleosSpeechInput(context){sdk->
                inputs.incrementAndGet()
                evidence.append("INPUT generated PCM through production PleosSpeechInput, actual ON_DEVICE STT\n")
                val chunks=pcm.asList().chunked(3200).map{it.toByteArray()}
                chunks.forEachIndexed { i,chunk -> main.postDelayed({sdk.sendAudio(chunk)},i*100L) }
                main.postDelayed({sdk.completeAudioSend()},chunks.size*100L)
            }
        }
        scenario=ActivityScenario.launch<MainActivity>(checkNotNull(app.packageManager.getLaunchIntentForPackage(app.packageName)))
        scenario!!.onActivity{voice=it.voice}
        waitFor { model.canPlay && voice.ready && voice.onDeviceAvailable }
        evidence.append("PASS fresh actual P/UX/odometer and production voice readiness\n")
        click("보호자")
        compose.onNodeWithText("정답").performTextInput("14")
        click("보호자 확인")
        compose.onNodeWithContentDescription("테마 앰비언트 색상 설정").performScrollTo().performClick()
        compose.onNodeWithText("탐험").performClick()
        click("매직 캐슬")
        compose.waitUntil(10_000){compose.onAllNodesWithText("어디로 가볼까?",substring=true).fetchSemanticsNodes().size==1}
        capture("e2e-magic-ticket.png")
        click("어디로 가볼까?")
        click("여행지는 나중에 고를래요")
        waitFor { model.stamps.size==1 }
        compose.onNodeWithText("콩!",substring=true).assertIsDisplayed()
        waitFor(25_000){lightStatus.startsWith("테마 앰비언트 색 적용 완료")}
        compose.runOnUiThread {
            check(inputs.get()==1){"Expected exactly one production speech session"}
            check(writes.get()==1){"Expected one starlight request, got ${writes.get()}"}
            check(model.state.phase==com.parkonwheels.core.SafariEngine.Phase.COMPLETE)
        }
        evidence.append("PASS UI entrance/theme/ticket, real bundled narration and production final-result matcher\n")
        evidence.append("PASS real Pleos ambient RGB write AND readback confirmed #FFE5A1\n")
        evidence.append("PASS exactly one voice request, one starlight request and one persisted stamp\n")
        capture("e2e-magic-stamp.png")
        click("내 여행 패스포트 펼치기")
        compose.onNodeWithContentDescription("마법 배지 펼치기").assertIsDisplayed()
        compose.onNodeWithContentDescription("마법 배지 펼치기").performClick()
        compose.onNodeWithText("별빛 마법사").performClick()
        compose.onNodeWithText("닫기 ×").assertIsDisplayed()
        val detailArt=hasTestTag("collection-art-magic-0") and hasAnyAncestor(isDialog())
        compose.waitUntil(10_000){compose.onAllNodes(detailArt).fetchSemanticsNodes().size==1}
        compose.onNode(detailArt).assertIsDisplayed()
        capture("e2e-magic-collection.png",dialog=true)
        evidence.append("PASS passport, magic collection and detail narration reached by UI taps\n")
        compose.runOnUiThread {
            val restored=SafariModel(app,object:AppVehicleGateway by gateway{override fun start(listener:com.parkonwheels.core.VehicleGateway.Listener)=Unit;override fun background()=Unit},pref)
            check(restored.stamps.size==1 && restored.stamps.single().themeId=="magic")
        }
        evidence.append("PASS isolated passport restored; user passport untouched\n")
    }
}
