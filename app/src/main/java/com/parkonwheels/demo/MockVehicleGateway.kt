package com.parkonwheels.demo

import android.content.SharedPreferences
import com.parkonwheels.core.VehicleGateway

/** Explicit simulation controls. Never substitute this for a real vehicle safety adapter. */
class MockVehicleGateway(private val prefs: SharedPreferences) : VehicleGateway {
    private var listener: VehicleGateway.Listener? = null
    var kilometres: Double = prefs.getString("mock_km", "1000.0")!!.toDoubleOrNull() ?: 1000.0
        private set
    override fun start(listener: VehicleGateway.Listener) {
        this.listener = listener
        listener.onGate(VehicleGateway.Gate.UNKNOWN)
        listener.onOdometer(kilometres)
    }
    fun gate(value: VehicleGateway.Gate) { listener?.onGate(value) }
    fun addKilometres(value: Double) {
        kilometres += value
        check(prefs.edit().putString("mock_km", kilometres.toString()).commit()) { "모의 거리 저장 실패" }
        listener?.onOdometer(kilometres)
    }
    fun missingDistance() { listener?.onOdometer(null) }
    override fun close() { listener = null }
}
