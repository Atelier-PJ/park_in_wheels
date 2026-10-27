package com.parkonwheels.demo

import com.parkonwheels.core.VehicleGateway

interface AppVehicleGateway : VehicleGateway {
    val label: String
    val simulation: SimulationControls? get() = null
    fun foreground()
    fun background()
}

interface SimulationControls {
    fun gate(value: VehicleGateway.Gate)
    fun addKilometres(value: Double)
    fun missingDistance()
}
