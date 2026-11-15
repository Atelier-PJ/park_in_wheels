package com.parkonwheels.demo

import android.app.Application
import android.app.Instrumentation
import android.os.Bundle
import android.os.SystemClock
import com.parkonwheels.core.VehicleGateway

/** Injected context/model verification only, not actual navigation or charging evidence. */
class RecommendationVerification:Instrumentation() {
    override fun onCreate(arguments:Bundle?) {super.onCreate(arguments);start()}
    override fun onStart() {
        val evidence=StringBuilder();var success=false
        val app=targetContext.applicationContext as Application
        val pref="travel_verification_${System.nanoTime()}"
        var model:SafariModel?=null
        try {
            runOnMainSync {
                val vehicle=TestVehicle();val travel=TestTravel()
                var m=SafariModel(app,vehicle,pref,travel);model=m;m.foreground()
                check(m.canPlay && !m.recommendationsEnabled && m.recommendedTheme==null)
                m.changeRecommendations(true)
                val old=travel.listener!!
                for((name,id) in listOf("해운대해수욕장" to "ocean","서울숲" to "dino","경복궁" to "magic","국립과천과학관" to "space")) {
                    travel.listener!!.destination(name,SystemClock.elapsedRealtime());check(m.recommendedTheme==id)
                }
                travel.listener!!.destination("서울숲",SystemClock.elapsedRealtime()-46_000);check(m.recommendedTheme==null)
                travel.listener!!.destination("부산역",SystemClock.elapsedRealtime());check(m.recommendedTheme==null)
                m.setTripDestination("해운대해수욕장");check(m.recommendedTheme=="ocean" && m.recommendationSource=="직접 입력한 여행지")
                m.chooseTheme("magic");check(m.theme.id=="magic")
                m.changeRecommendations(false);old.destination("서울숲",SystemClock.elapsedRealtime());check(m.recommendedTheme==null)
                m.changeRecommendations(true);m.setTripDestination("경복궁");check(m.recommendedTheme=="magic")
                m.background();old.destination("서울숲",SystemClock.elapsedRealtime());check(m.recommendedTheme==null)
                m.foreground();check(m.recommendedTheme==null)
                val stored=app.getSharedPreferences(pref,0).all.toString()
                check(!stored.contains("경복궁") && !stored.contains("해운대"))
                evidence.append("PASS opt-in, four themes, stale/unknown fallback, manual source, free choice, opt-out, late callback, background clearing and no raw destination persistence\n")
            }
            success=true
        } catch(t:Throwable){evidence.append("FAIL ${t.stackTraceToString()}\n")}
        finally {runOnMainSync{model?.background();app.getSharedPreferences(pref,0).edit().clear().commit()}}
        finish(if(success)android.app.Activity.RESULT_OK else android.app.Activity.RESULT_CANCELED,Bundle().apply{putString("stream",evidence.toString())})
    }
    private class TestTravel:TravelContextGateway {
        var listener:TravelContextGateway.Listener?=null
        override fun start(charging:Boolean,navigation:Boolean,listener:TravelContextGateway.Listener){this.listener=listener}
        override fun stop(){listener=null}
    }
    private class TestVehicle:AppVehicleGateway {
        override val label="INJECTED TEST ONLY"
        var listener:VehicleGateway.Listener?=null
        override fun start(listener:VehicleGateway.Listener){this.listener=listener;listener.onGate(VehicleGateway.Gate.AVAILABLE);listener.onOdometer(1000.0)}
        fun gate(g:VehicleGateway.Gate){listener?.onGate(g)}
        override fun foreground(){listener?.onOdometer(1000.0);gate(VehicleGateway.Gate.AVAILABLE)}
        override fun background(){gate(VehicleGateway.Gate.UNKNOWN)}
        override fun close(){listener=null}
    }
}
