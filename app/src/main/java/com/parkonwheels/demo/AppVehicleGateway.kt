package com.parkonwheels.demo

import com.parkonwheels.core.VehicleGateway

interface AppVehicleGateway : VehicleGateway {
    val label: String
    val simulation: SimulationControls? get() = null
    fun ambientColor(color: Int?, report: (String) -> Unit) {
        report("테마 색상 미리보기 · 실제 조명 제어는 지원하지 않는 빌드예요")
    }
    fun foreground()
    fun background()
}

interface SimulationControls {
    fun gate(value: VehicleGateway.Gate)
    fun addKilometres(value: Double)
    fun missingDistance()
}
