package com.parkonwheels.demo

import android.app.Application
import android.app.Instrumentation
import android.os.Build
import android.os.Bundle
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Actual read-only optional service probe. No injected SDK results. */
class TravelServicesVerification:Instrumentation() {
    override fun onCreate(arguments:Bundle?){super.onCreate(arguments);start()}
    override fun onStart() {
        val evidence=StringBuilder();var gateway:TravelContextGateway?=null
        val charge=CountDownLatch(1);val nav=CountDownLatch(1)
        try {
            check(Build.HARDWARE.contains("ranchu")){"Emulator only"}
            runOnMainSync {
                gateway=createTravelContextGateway(targetContext.applicationContext as Application)
                gateway!!.start(true,true,object:TravelContextGateway.Listener {
                    override fun charging(value:Boolean?,observedAt:Long){synchronized(evidence){evidence.append("ACTUAL EV_CHARGE_STATE callback: $value\n")};charge.countDown()}
                    override fun arrived(opaqueId:String,observedAt:Long){synchronized(evidence){evidence.append("ACTUAL destination arrival received (no place data logged)\n")}}
                    override fun clearArrival(){}
                    override fun status(message:String){synchronized(evidence){evidence.append("ACTUAL SERVICE: $message\n")};nav.countDown()}
                })
            }
            charge.await(10,TimeUnit.SECONDS);nav.await(5,TimeUnit.SECONDS)
            synchronized(evidence){evidence.append("Probe completed; connection/read results are not proof of a charging transition or navigation arrival.\n")}
        } catch(t:Throwable){evidence.append("FAIL ${t.stackTraceToString()}\n")}
        finally {runOnMainSync{gateway?.close()}}
        finish(android.app.Activity.RESULT_OK,Bundle().apply{putString("stream",evidence.toString())})
    }
}
