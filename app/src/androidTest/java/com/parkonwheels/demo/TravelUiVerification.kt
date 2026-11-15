package com.parkonwheels.demo

import android.app.Application
import android.app.Instrumentation
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.parkonwheels.core.VehicleGateway

/** Production UI under explicitly injected telemetry. Never presented as real SDK arrival/charging. */
class TravelUiVerification:Instrumentation() {
    override fun onCreate(arguments:Bundle?){super.onCreate(arguments);start()}
    private fun find(node:AccessibilityNodeInfo?,text:String):AccessibilityNodeInfo? {
        if(node==null)return null
        if(node.text?.toString()?.replace(" ","")==text.replace(" ","") || node.contentDescription?.toString()==text)return node
        for(i in 0 until node.childCount)find(node.getChild(i),text)?.let{return it}
        return null
    }
    private fun click(text:String) {
        val until=SystemClock.elapsedRealtime()+60000
        while(SystemClock.elapsedRealtime()<until) {
            if(android.os.Build.VERSION.SDK_INT>=33) uiAutomation.clearCache()
            val found=uiAutomation.windows.firstNotNullOfOrNull{find(it.root,text)}
            if(found!=null) {
                var n:AccessibilityNodeInfo?=found
                while(n!=null && !n.isClickable)n=n.parent
                if(n?.performAction(AccessibilityNodeInfo.ACTION_CLICK)==true)return
                val bounds=android.graphics.Rect();found.getBoundsInScreen(bounds)
                if(!bounds.isEmpty) {
                    val at=SystemClock.uptimeMillis()
                    uiAutomation.injectInputEvent(android.view.MotionEvent.obtain(at,at,android.view.MotionEvent.ACTION_DOWN,bounds.exactCenterX(),bounds.exactCenterY(),0),true)
                    uiAutomation.injectInputEvent(android.view.MotionEvent.obtain(at,at+50,android.view.MotionEvent.ACTION_UP,bounds.exactCenterX(),bounds.exactCenterY(),0),true)
                    return
                }
            }
            Thread.sleep(200)
        }
        error("UI control missing: $text")
    }
    override fun onStart() {
        val info=uiAutomation.serviceInfo;info.flags=info.flags or android.accessibilityservice.AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;uiAutomation.serviceInfo=info
        val pref="travel_ui_${System.nanoTime()}";val app=targetContext.applicationContext as Application
        var m:SafariModel?=null;var voice:VoiceDirector?=null;var activity:MainActivity?=null
        val evidence=StringBuilder();var success=false
        try {
            val vehicle=object:AppVehicleGateway {
                override val label="INJECTED UI TEST"
                var listener:VehicleGateway.Listener?=null
                override fun start(listener:VehicleGateway.Listener){this.listener=listener;listener.onGate(VehicleGateway.Gate.AVAILABLE);listener.onOdometer(1000.0)}
                override fun foreground(){listener?.onGate(VehicleGateway.Gate.AVAILABLE);listener?.onOdometer(1000.0)}
                override fun background(){listener?.onGate(VehicleGateway.Gate.UNKNOWN)}
                override fun close(){}
            }
            val travel=object:TravelContextGateway {
                var listener:TravelContextGateway.Listener?=null
                override fun start(charging:Boolean,navigation:Boolean,listener:TravelContextGateway.Listener){this.listener=listener}
                override fun stop(){listener=null}
            }
            activity=startActivitySync(Intent(targetContext,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as MainActivity
            runOnMainSync {
                m=SafariModel(app,vehicle,pref,travel);m!!.foreground();m!!.chooseTheme("space");m!!.changeCharging(true)
                travel.listener!!.charging(true,SystemClock.elapsedRealtime());m!!.start();m!!.answer(0);m!!.nextMission()
                voice=VoiceDirector(activity!!){m!!.canPlay};voice!!.configureThemeSound("space",true)
            }
            val readyUntil=SystemClock.elapsedRealtime()+6000
            while(SystemClock.elapsedRealtime()<readyUntil && voice?.ready!=true)Thread.sleep(100)
            runOnMainSync { activity!!.setContent { ParkTheme { Column { Text("테스트 신호로 충전·도착 화면 확인",color=androidx.compose.ui.graphics.Color.White,modifier=Modifier.fillMaxWidth().background(androidx.compose.ui.graphics.Color(0xFF24394A)).padding(8.dp));Box(Modifier.weight(1f)){StoryJourneyScreen(m!!,voice!!,onPassport={},onCancel={},autoStart=true)} } } } }
            Thread.sleep(1600);capture("travel-charging.png")
            click("에너지 조각 1");click("에너지 조각 2");click("에너지 조각 3")
            click("마지막 장면 열기")
            Thread.sleep(1000);runOnMainSync{check(m!!.stamps.single().chargingSpecial)};capture("travel-special-stamp.png")
            evidence.append("PASS production charging UI: three actual button taps → existing finale touch alternative → special badge\n")
            runOnMainSync {
                voice!!.stop();m!!.cancel();m!!.changeVisits(true);travel.listener!!.arrived("ui-test-visit",SystemClock.elapsedRealtime())
                activity!!.setContent { ParkTheme { Column { Text("테스트 신호로 충전·도착 화면 확인",color=androidx.compose.ui.graphics.Color.White,modifier=Modifier.fillMaxWidth().background(androidx.compose.ui.graphics.Color(0xFF24394A)).padding(8.dp));Box(Modifier.weight(1f)){ParkApp(m!!,voice!!)} } } }
            }
            click("방문 스탬프 받기");Thread.sleep(700);capture("travel-visit-stamp.png");click("패스포트 보기")
            Thread.sleep(700);runOnMainSync{check(m!!.visits.size==1)};capture("travel-passport.png")
            evidence.append("PASS explicit parked visit claim, reward dialog and passport visit count\n")
            success=true
        } catch(t:Throwable){capture("travel-ui-failure.png");evidence.append("FAIL phase=${m?.state?.phase} mission=${m?.missionStep} voice=${voice?.status} ${t.stackTraceToString()}\n")}
        finally {runOnMainSync{voice?.close();m?.background();app.getSharedPreferences(pref,0).edit().clear().commit();activity?.finish()}}
        finish(if(success)android.app.Activity.RESULT_OK else android.app.Activity.RESULT_CANCELED,Bundle().apply{putString("stream",evidence.toString())})
    }
    private fun capture(name:String) {
        uiAutomation.takeScreenshot()?.let{bitmap->java.io.FileOutputStream(java.io.File(targetContext.filesDir,name)).use{bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)}}
    }
}
