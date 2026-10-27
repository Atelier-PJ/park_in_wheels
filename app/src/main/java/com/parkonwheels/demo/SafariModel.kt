package com.parkonwheels.demo

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.parkonwheels.core.SafariEngine
import com.parkonwheels.core.VehicleGateway
import org.json.JSONObject

class SafariModel(application: Application) : AndroidViewModel(application) {
    private val handler = Handler(Looper.getMainLooper())
    private var cleared = false
    private val prefs = application.getSharedPreferences("safari_demo_v1", 0)
    private fun load(): SafariEngine.Snapshot = runCatching {
        val json = JSONObject(prefs.getString("session", "")!!)
        SafariEngine.Snapshot(SafariEngine.Phase.valueOf(json.getString("phase")),
            json.getDouble("start"), json.getDouble("travelled"), json.getInt("cards"))
    }.getOrElse { SafariEngine.Snapshot.empty() }
    private val engine = SafariEngine(load()) { next ->
        val json = JSONObject().put("phase", next.phase.name).put("start", next.startKm)
            .put("travelled", next.travelledKm).put("cards", next.cards)
        check(prefs.edit().putString("session", json.toString()).commit()) { "진행 저장 실패" }
    }
    var state by mutableStateOf(engine.snapshot())
        private set
    var gate by mutableStateOf(VehicleGateway.Gate.UNKNOWN)
        private set
    var hasDistance by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var diagnostic by mutableStateOf("연결 확인 전")
        private set
    private val vehicle: AppVehicleGateway = createVehicleGateway(application) { message ->
        main { diagnostic = message; Log.i("ParkOnWheels", message) }
    }
    val modeLabel get() = vehicle.label
    val isSimulation get() = vehicle.simulation != null
    val canPlay get() = gate == VehicleGateway.Gate.AVAILABLE && error == null
    val readyToHatch get() = canPlay && engine.readyToHatch()

    init {
        vehicle.start(object : VehicleGateway.Listener {
            override fun onGate(value: VehicleGateway.Gate) = main { act { engine.onGate(value) } }
            override fun onOdometer(kilometres: Double?) = main { act { engine.onOdometer(kilometres) } }
        })
    }
    private fun main(action: () -> Unit) {
        if (cleared) return
        if (Looper.myLooper() == Looper.getMainLooper()) action()
        else handler.post { if (!cleared) action() }
    }
    private fun refresh() { state = engine.snapshot(); gate = engine.gate(); hasDistance = engine.hasDistance() }
    private fun act(action: () -> Unit) {
        try { action() } catch (ex: Exception) {
            error = "처리 오류: 앱을 다시 실행해 주세요."
            Log.e("ParkOnWheels", "session operation failed", ex)
        } finally { refresh() }
    }
    fun simulateGate(next: VehicleGateway.Gate) = act { vehicle.simulation?.gate(next) }
    fun addDistance() = act { vehicle.simulation?.addKilometres(1.0) }
    fun missingDistance() = act { vehicle.simulation?.missingDistance() }
    fun start() = act { if (canPlay) engine.start() }
    fun resume() = act { if (canPlay) engine.resume() }
    fun pause() = act { engine.pause() }
    fun hatch() = act { if (canPlay) engine.hatch() }
    fun cancel() = act { if (canPlay) engine.cancel() }
    fun foreground() = act { vehicle.foreground() }
    fun background() = act {
        engine.onGate(VehicleGateway.Gate.UNKNOWN)
        vehicle.background()
    }
    override fun onCleared() { cleared = true; vehicle.close(); handler.removeCallbacksAndMessages(null) }
}
