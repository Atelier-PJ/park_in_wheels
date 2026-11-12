package com.parkonwheels.demo

import android.app.Application
import android.app.Instrumentation
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.speech.SpeechRecognizer
import com.parkonwheels.core.VehicleGateway
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Explicit emulator-only capability probe; restores any ambient colors it successfully changed. */
class PleosCapabilityVerification : Instrumentation() {
    override fun onCreate(arguments: Bundle?) { super.onCreate(arguments); start() }
    override fun onStart() {
        val result = Bundle(); val evidence = StringBuilder()
        var vehicle: AppVehicleGateway? = null
        var voice: VoiceDirector? = null
        try {
            check(targetContext.packageName == "com.parkonwheels.pleos")
            check(Build.MODEL.contains("emulator", true)) { "This verification is emulator-only" }
            val app = targetContext.applicationContext as Application
            val available = CountDownLatch(1)
            runOnMainSync {
                vehicle = createVehicleGateway(app) { evidence.append("SDK: $it\n") }
                vehicle!!.start(object : VehicleGateway.Listener {
                    override fun onGate(gate: VehicleGateway.Gate) { if(gate == VehicleGateway.Gate.AVAILABLE) available.countDown() }
                    override fun onOdometer(kilometres: Double?) { }
                })
                vehicle!!.foreground()
            }
            check(available.await(15, TimeUnit.SECONDS)) { "Fresh actual P/UX/distance not available" }
            val applied = CountDownLatch(1)
            runOnMainSync {
                vehicle!!.ambientColor(0x74B37C) { status ->
                    evidence.append("AMBIENT: $status\n")
                    if(!status.contains("확인하고 있어요")) applied.countDown()
                }
            }
            if(!applied.await(7, TimeUnit.SECONDS)) evidence.append("AMBIENT: no completion callback; unverified\n")
            val restored = CountDownLatch(1)
            runOnMainSync { vehicle!!.ambientColor(null) { status -> evidence.append("RESTORE: $status\n"); restored.countDown() } }
            restored.await(7, TimeUnit.SECONDS)
            runOnMainSync { voice = VoiceDirector(app) { false } }
            val timeout = SystemClock.elapsedRealtime() + 8_000
            var initialized = false
            while(!initialized && SystemClock.elapsedRealtime() < timeout) {
                runOnMainSync { initialized = voice!!.status != "한국어 음성 안내 확인 중" }
                if(!initialized) SystemClock.sleep(100)
            }
            runOnMainSync {
                evidence.append("VOICE: offlineTts=${voice!!.ready}; onDeviceStt=${Build.VERSION.SDK_INT >= 31 && SpeechRecognizer.isOnDeviceRecognitionAvailable(app)}; ${voice!!.status}\n")
            }
            evidence.append("PASS: emulator actual parked/UX/odometer established; voice and ambient capability results above are separate from end-to-end success\n")
            result.putString("stream", evidence.toString()); finish(-1,result)
        } catch(error: Throwable) { result.putString("stream", evidence.toString()+"FAIL: ${error.stackTraceToString()}\n");finish(0,result) }
        finally { runOnMainSync { voice?.close(); vehicle?.close() } }
    }
}
