package com.parkonwheels.demo

import android.content.SharedPreferences
import android.app.Application
import com.parkonwheels.core.VehicleGateway

/** Explicit simulation controls. Never substitute this for a real vehicle safety adapter. */
fun createVehicleGateway(application: Application, report: (String) -> Unit): AppVehicleGateway =
    MockVehicleGateway(application.getSharedPreferences("safari_demo_v1", 0))

class MockVehicleGateway(private val prefs: SharedPreferences) : AppVehicleGateway, SimulationControls {
    override val label = "DEMO · 모의 차량 데이터 · 실제 API 미연결"
    override val simulation get() = this
    private var listener: VehicleGateway.Listener? = null
    var kilometres: Double = prefs.getString("mock_km", "1000.0")!!.toDoubleOrNull() ?: 1000.0
        private set
    override fun start(listener: VehicleGateway.Listener) {
        this.listener = listener
        listener.onGate(VehicleGateway.Gate.UNKNOWN)
        listener.onOdometer(kilometres)
    }
    override fun foreground() { listener?.onGate(VehicleGateway.Gate.UNKNOWN); listener?.onOdometer(kilometres) }
    override fun background() { listener?.onGate(VehicleGateway.Gate.UNKNOWN) }
    override fun gate(value: VehicleGateway.Gate) { listener?.onGate(value) }
    override fun addKilometres(value: Double) {
        kilometres += value
        check(prefs.edit().putString("mock_km", kilometres.toString()).commit()) { "모의 거리 저장 실패" }
        listener?.onOdometer(kilometres)
    }
    override fun missingDistance() { listener?.onOdometer(null) }
    override fun close() { listener = null }
}
