package com.parkonwheels.demo

import android.app.Application
import android.app.Instrumentation
import android.os.Bundle
import android.os.SystemClock
import com.parkonwheels.core.VehicleGateway

/** Injected context/model verification only, not actual navigation or charging evidence. */
class TravelExperienceVerification:Instrumentation() {
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
                check(m.canPlay);m.chooseTheme("space");m.changeCharging(true)
                travel.listener!!.charging(true,SystemClock.elapsedRealtime());check(m.chargingNow)
                m.start();check(m.chargingEpisode && m.missions==chargingStationMissions)
                travel.listener!!.charging(false,SystemClock.elapsedRealtime());check(!m.chargingNow && m.chargingEpisode)
                repeat(3){m.answer(0);m.nextMission()};m.hatch()
                check(m.stamps.size==1 && m.stamps.last().chargingSpecial && m.reward=="우주정비 배지")
                evidence.append("PASS injected actual-state contract selects and snapshots charging episode; special reward persisted\n")
                m.cancel();m.changeVisits(true)
                vehicle.gate(VehicleGateway.Gate.RESTRICTED)
                travel.listener!!.arrived("test-destination-day",SystemClock.elapsedRealtime())
                m.claimVisit();check(m.visits.isEmpty())
                vehicle.gate(VehicleGateway.Gate.AVAILABLE);check(m.arrivalAvailable)
                m.claimVisit();m.claimVisit();check(m.visits.size==1 && !m.arrivalAvailable)
                travel.listener!!.arrived("test-destination-day",SystemClock.elapsedRealtime());m.claimVisit();check(m.visits.size==1)
                evidence.append("PASS arrival cannot grant play; parked explicit claim gives exactly one visit\n")
                m.background()
                val t2=TestTravel();m=SafariModel(app,TestVehicle(),pref,t2);model=m;m.foreground()
                check(m.visits.size==1)
                t2.listener!!.arrived("test-destination-day",SystemClock.elapsedRealtime());m.claimVisit();check(m.visits.size==1)
                t2.listener!!.charging(null,SystemClock.elapsedRealtime());m.chooseTheme("space");m.start();check(!m.chargingEpisode)
                m.cancel();m.changeVisits(false);check(!m.arrivalAvailable)
                evidence.append("PASS restart deduplication, unknown charging normal fallback and opt-out\n")
                m.background();check(!m.chargingNow && !m.arrivalAvailable)
                evidence.append("PASS background clears optional context\n")
                for(theme in listOf("dino","magic","space","ocean")) for(kind in listOf("departure","ambience")) {
                    val bytes=app.assets.open("sound/${theme}_${kind}.wav").use{it.readBytes()}
                    check(String(bytes,0,4)=="RIFF" && bytes.size>24000)
                }
                evidence.append("PASS eight bundled WAV assets decoded as RIFF\n")
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
