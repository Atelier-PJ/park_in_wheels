package com.parkonwheels.demo

import android.app.Application
import android.Manifest
import android.os.Handler
import android.os.Looper
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.parkonwheels.core.VehicleGateway
import org.junit.After
import org.junit.Rule
import org.junit.Test

/** Full production navigation/narration. Fake input tests errors, never claims real recognition. */
class JourneyUiTest {
    @get:Rule val compose = createEmptyComposeRule()
    @get:Rule val microphone = GrantPermissionRule.grant(Manifest.permission.RECORD_AUDIO)
    private val app get() = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as Application
    private val pref = "ui_journey_${System.nanoTime()}"
    private var scenario:ActivityScenario<MainActivity>?=null
    private lateinit var model: SafariModel
    private lateinit var voice: VoiceDirector
    private val gateway = UiVehicle()
    private val speech = UiSpeech()
    private fun open() {
        E2eRuntime.model={ application -> SafariModel(application,gateway,pref).also{model=it} }
        E2eRuntime.speech={speech}
        scenario=ActivityScenario.launch<MainActivity>(checkNotNull(app.packageManager.getLaunchIntentForPackage(app.packageName)))
        scenario!!.onActivity{voice=it.voice;model.changeReducedMotion(true)}
    }

    private fun click(text: String) {
        compose.waitUntil(10_000){compose.onAllNodesWithText(text,substring=true).fetchSemanticsNodes().size==1}
        compose.onNodeWithText(text,substring=true).performClick()
    }
    private fun waitFor(timeout:Long=120_000, condition:()->Boolean) {
        compose.waitUntil(timeout) { var done=false;compose.runOnUiThread{done=condition()};done }
    }
    @After fun close() {
        scenario?.close()
        app.getSharedPreferences(pref,0).edit().clear().commit()
    }
    @Test fun fourThemesNarrateFinishAndPersistThroughUi() {
        open()
        click("입장하기")
        adventureThemes.forEachIndexed { i, theme ->
            click(theme.name)
            click("어디로 가볼까?")
            click("여행지는 나중에 고를래요")
            waitFor { model.missionStep>=1 }
            if(i==0) {
                // Reproduce the old dead-end: narration interruption before the finale.
                compose.runOnUiThread { voice.stop("테스트: 안내 중단") }
                compose.waitUntil(5000) { compose.onAllNodesWithText("이야기 듣기",substring=true).fetchSemanticsNodes().isNotEmpty() }
                click("이야기 듣기")
            }
            waitFor { model.missionStep==3 && speech.listening }
            compose.runOnUiThread { speech.fail("주문을 듣지 못했어요. 다시 말해 주세요") }
            waitFor { !voice.enabled }
            // A recognizer error must keep the active finale and touch fallback reachable.
            compose.waitUntil(10_000){compose.onAllNodesWithText("말하기 어려우면 손으로 열기").fetchSemanticsNodes().size==1}
            compose.onNodeWithText("말하기 어려우면 손으로 열기").assertIsEnabled().performClick()
            waitFor { model.stamps.size==i+1 }
            compose.onNodeWithText("콩!",substring=true).assertIsDisplayed()
            click("내 여행 패스포트 펼치기")
            compose.onNodeWithContentDescription("공룡 도감 펼치기").assertIsDisplayed()
            click("← 테마로")
        }
        compose.runOnUiThread {
            val restored=SafariModel(app,UiVehicle(),pref)
            check(restored.stamps.size==4 && restored.stamps.map{it.themeId}.toSet().size==4)
            restored.background()
        }
    }
    @Test fun restrictedFinalAndLateVoiceCannotAwardOrWriteLight() {
        open();click("입장하기");click("매직 캐슬");click("어디로 가볼까?");click("여행지는 나중에 고를래요")
        waitFor { model.missionStep==3 && speech.listening }
        val oldResult=speech.result
        compose.runOnUiThread { gateway.restrict();check(!speech.listening){"Safety change must synchronously stop microphone input"};oldResult?.invoke("별빛을 밝혀라") }
        waitFor { model.state.phase==com.parkonwheels.core.SafariEngine.Phase.PAUSED }
        compose.runOnUiThread { check(model.stamps.isEmpty());gateway.park() }
        // Fresh state does not automatically resume playback or accept a stale callback.
        compose.runOnUiThread { oldResult?.invoke("별빛을 밝혀라");check(model.stamps.isEmpty());check(model.state.phase==com.parkonwheels.core.SafariEngine.Phase.PAUSED) }
        click("이야기 이어서 듣기")
        waitFor { speech.listening }
        compose.runOnUiThread { speech.result?.invoke("다른 주문") }
        waitFor { !voice.enabled }
        compose.runOnUiThread { check(model.stamps.isEmpty()) }
        click("주문 다시 말하기")
        waitFor { speech.listening }
        compose.runOnUiThread { val final=speech.result;final?.invoke("별빛을 밝혀라");final?.invoke("별빛을 밝혀라") }
        waitFor { model.stamps.size==1 }
        compose.runOnUiThread { check(model.stamps.size==1) }
    }
    private class UiVehicle:AppVehicleGateway {
        override val label="explicit UI test simulation"
        lateinit var listener:VehicleGateway.Listener
        override fun start(listener:VehicleGateway.Listener){this.listener=listener;park()}
        fun park(){listener.onOdometer(100.0);listener.onGate(VehicleGateway.Gate.AVAILABLE)}
        fun restrict(){listener.onGate(VehicleGateway.Gate.RESTRICTED)}
        override fun foreground()=park()
        override fun background(){listener.onGate(VehicleGateway.Gate.UNKNOWN)}
        override fun close()=Unit
    }
    private class UiSpeech:SpeechInput {
        override val ready=true
        override val status="explicit test input"
        var listening=false
        var result:((String)->Unit)?=null
        private var error:((String)->Unit)?=null
        override fun listen(onListening:()->Unit,onResult:(String)->Unit,onError:(String)->Unit){result=onResult;error=onError;listening=true;onListening()}
        fun fail(reason:String){listening=false;error?.invoke(reason)}
        override fun cancel(){listening=false;result=null;error=null}
        override fun close()=cancel()
    }
}
