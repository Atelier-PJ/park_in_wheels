package com.parkonwheels.demo

import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.Odometer
import ai.pleos.playground.vehicle.api.Gear
import ai.pleos.playground.vehicle.constant.control.VehicleGear
import ai.pleos.playground.vehicle.listener.GearSelectionListener
import ai.pleos.playground.vehicle.api.model.capability.AccessType
import ai.pleos.playground.vehicle.listener.DistanceDrivenListener
import android.app.Application
import android.car.Car
import android.car.drivingstate.CarUxRestrictions
import android.car.drivingstate.CarUxRestrictionsManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.parkonwheels.core.VehicleGateway
import com.parkonwheels.core.VehicleGateway.Gate
import com.parkonwheels.core.VehicleReadiness

fun createVehicleGateway(application: Application, report: (String) -> Unit): AppVehicleGateway =
    PleosVehicleGateway(application, report)

/** Typed, read-only SDK 2.3.2 adapter. No mock state or vehicle control calls in this flavor. */
class PleosVehicleGateway(
    private val application: Application,
    private val report: (String) -> Unit
) : AppVehicleGateway {
    override val label = "PLEOS SDK TEST · Vehicle 2.3.2 · 에뮬레이터 검증용"
    private val handler = Handler(Looper.getMainLooper())
    private val readiness = VehicleReadiness()
    private var target: VehicleGateway.Listener? = null
    private var vehicle: Vehicle? = null
    private var odometer: Odometer? = null
    private var gear: Gear? = null
    private var gearRequestId = 0L
    private var gearEventVersion = 0L
    private var gearListener: GearSelectionListener? = null
    private var car: Car? = null
    private var uxManager: CarUxRestrictionsManager? = null
    private var distanceListener: DistanceDrivenListener? = null
    private var active = false
    private var epoch = 0L
    private var request = 0L
    private var inFlightAt: Long? = null
    private var readable = false
    private var eventVersion = 0L
    private var lastMessage = ""
    private val ambient = AmbientSceneController(application, { vehicle?.getLight() }, {
        active && readiness.gate(SystemClock.elapsedRealtime()) == Gate.AVAILABLE
    })
    override fun ambientColor(color: Int?, report: (String) -> Unit) { ambient.apply(color, report) }

    override fun start(listener: VehicleGateway.Listener) {
        target = listener
        listener.onGate(Gate.UNKNOWN)
        listener.onOdometer(null)
    }
    private fun note(message: String) {
        if (message != lastMessage) { lastMessage = message; report(message) }
    }
    private fun dispatch(token: Long, action: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            if (active && epoch == token) action()
        } else handler.post { if (active && epoch == token) action() }
    }
    private fun publish() {
        val gate = readiness.gate(SystemClock.elapsedRealtime())
        if (gate != Gate.AVAILABLE) ambient.suspendCommands()
        target?.onGate(gate)
    }
    private fun invalidDistance(message: String) {
        readiness.distance(null, SystemClock.elapsedRealtime())
        // Hide gameplay before mutating or persisting domain distance.
        publish()
        target?.onOdometer(null)
        note(message)
    }
    private fun acceptDistance(value: Float?, receivedAt: Long = SystemClock.elapsedRealtime()) {
        val now=SystemClock.elapsedRealtime()
        if(now < receivedAt || now-receivedAt > VehicleReadiness.MAX_AGE_MS) {
            invalidDistance("delayed odometer callback · awaiting fresh state")
            return
        }
        val km = readiness.distance(value, receivedAt)
        if (km == null) { invalidDistance("odometer unavailable"); return }
        target?.onOdometer(km)
        note("odometer_km=$km · capability=READ · actual SDK callback")
        publish()
    }
    override fun foreground() {
        if (active) return
        active = true
        epoch++
        readiness.reset()
        publish()
        val token = epoch
        try {
            vehicle = Vehicle(application).also { it.initialize() }
            odometer = vehicle!!.getOdometer()
            gear = vehicle!!.getGear()
            // SDK 2.3.2 Gear has no capability method. Typed reads require CAR_DRIVE;
            // denied, unknown and stale values fail closed rather than inferring P from UX.
            gearListener = object : GearSelectionListener {
                override fun onSelected(value: VehicleGear?) {
                    val receivedAt=SystemClock.elapsedRealtime()
                    dispatch(token) { gearEventVersion++; acceptGear(value,receivedAt) }
                }
                override fun onFailed(e: Exception) = dispatch(token) { acceptGear(null) }
            }.also { gear!!.registerGearSelection(it) }
            note("Vehicle.initialize returned; awaiting capability/read callbacks")
            car = Car.createCar(application, handler, Car.CAR_WAIT_TIMEOUT_DO_NOT_WAIT) { connectedCar, ready ->
                dispatch(token) {
                    if (!ready) {
                        uxManager = null
                        readiness.ux(Gate.UNKNOWN)
                        publish()
                        note("Car service disconnected · UX unknown")
                    } else try {
                        val manager = connectedCar.getCarManager(Car.CAR_UX_RESTRICTION_SERVICE) as CarUxRestrictionsManager
                        uxManager = manager
                        manager.registerListener { value -> dispatch(token) { acceptUx(value) } }
                        acceptUx(manager.currentCarUxRestrictions)
                    } catch (ex: Exception) {
                        readiness.ux(Gate.UNKNOWN)
                        publish()
                        note("UX service error: ${ex.javaClass.simpleName}: ${ex.message}")
                    }
                }
            }
            handler.post(poll)
        } catch (ex: Exception) {
            invalidDistance("SDK connection error: ${ex.javaClass.simpleName}: ${ex.message}")
        }
    }
    private fun acceptGear(value: VehicleGear?, receivedAt: Long = SystemClock.elapsedRealtime()) {
        readiness.parked(when (value) {
            null, VehicleGear.GEAR_UNKNOWN -> null
            VehicleGear.GEAR_PARK -> true
            else -> false
        }, receivedAt)
        publish()
        note("gear=$value · actual SDK callback")
    }
    private fun acceptUx(value: CarUxRestrictions?) {
        val gate = when {
            value == null -> Gate.UNKNOWN
            value.isRequiresDistractionOptimization || value.activeRestrictions != CarUxRestrictions.UX_RESTRICTIONS_BASELINE -> Gate.RESTRICTED
            else -> Gate.AVAILABLE
        }
        readiness.ux(gate)
        publish()
        note("UX=$gate · activeRestrictions=${value?.activeRestrictions}")
    }
    private val poll = object : Runnable {
        override fun run() {
            if (!active) return
            val now = SystemClock.elapsedRealtime()
            if (!readiness.fresh(now)) {
                publish()
                target?.onOdometer(null)
            }
            if (inFlightAt?.let { now - it > VehicleReadiness.MAX_AGE_MS } == true) {
                request++
                inFlightAt = null
                readable = false
                invalidDistance("SDK read timeout · awaiting reconnect")
            }
            if (inFlightAt == null) read()
            val token = epoch
            val gearId = ++gearRequestId
            val gearVersion = gearEventVersion
            try { gear?.getCurrentGearSelectionState(
                onComplete = { value -> dispatch(token) {
                    if (gearId == gearRequestId && gearVersion == gearEventVersion) acceptGear(value, now)
                } },
                onFailed = { dispatch(token) { if (gearId == gearRequestId) acceptGear(null) } }) }
            catch (ex: Exception) { acceptGear(null) }
            // Re-query UX as well as listening, so a missed callback is not retained forever.
            try { uxManager?.let { acceptUx(it.currentCarUxRestrictions) } }
            catch (ex: Exception) { readiness.ux(Gate.UNKNOWN); publish(); note("UX refresh failed") }
            handler.postDelayed(this, 2_000)
        }
    }
    private fun read() {
        val odo = odometer ?: return
        val token = epoch
        val id = ++request
        inFlightAt = SystemClock.elapsedRealtime()
        fun failed(ex: Exception) = dispatch(token) {
            if (request == id) {
                inFlightAt = null
                readable = false
                invalidDistance("odometer failed: ${ex.javaClass.simpleName}: ${ex.message}")
            }
        }
        fun query() {
            val before = eventVersion
            val startedAt = SystemClock.elapsedRealtime()
            odo.getCurrentDistanceDriven(onComplete = { value ->
                val receivedAt=SystemClock.elapsedRealtime()
                dispatch(token) {
                if (request == id) {
                    inFlightAt = null
                    if (eventVersion == before) {
                        if(SystemClock.elapsedRealtime()-startedAt > VehicleReadiness.MAX_AGE_MS) invalidDistance("SDK read expired · awaiting fresh state")
                        else acceptDistance(value,receivedAt)
                    }
                }
            } }, onFailed = ::failed)
        }
        try {
            if (readable) query()
            else odo.checkCurrentDistanceDrivenCapabilities(onComplete = { config -> dispatch(token) {
                if (request != id) return@dispatch
                readable = config.accessType == AccessType.READ || config.accessType == AccessType.READ_WRITE
                if (!readable) {
                    inFlightAt = null
                    invalidDistance("odometer capability=${config.accessType}")
                } else try {
                    if (distanceListener == null) {
                        val listener = object : DistanceDrivenListener {
                            override fun onDistanceDrivenUpdated(value: Float?) {
                                val receivedAt=SystemClock.elapsedRealtime()
                                dispatch(token) { eventVersion++;acceptDistance(value,receivedAt) }
                            }
                            override fun onFailed(e: Exception) = dispatch(token) {
                                invalidDistance("odometer listener failed: ${e.javaClass.simpleName}")
                            }
                        }
                        odo.registerDistanceDriven(listener)
                        distanceListener = listener
                    }
                    query()
                } catch (ex: Exception) { failed(ex) }
            } }, onFailed = ::failed)
        } catch (ex: Exception) { failed(ex) }
    }
    override fun background() {
        ambient.suspendCommands()
        active = false
        epoch++
        request++
        readiness.reset()
        target?.onGate(Gate.UNKNOWN)
        target?.onOdometer(null)
        handler.removeCallbacksAndMessages(null)
        try { uxManager?.unregisterListener() } catch (ex: Exception) { note("UX cleanup failed") }
        try { car?.disconnect() } catch (ex: Exception) { note("Car disconnect failed") }
        try { distanceListener?.let { odometer?.unregisterDistanceDriven(it) } } catch (ex: Exception) { note("Distance cleanup failed") }
        try { gearListener?.let { gear?.unregisterGearSelection(it) } } catch (ex: Exception) { note("Gear cleanup failed") }
        try { vehicle?.release() } catch (ex: Exception) { note("Vehicle release failed") }
        uxManager = null
        gear = null
        gearListener = null
        car = null
        distanceListener = null
        odometer = null
        vehicle = null
        readable = false
        inFlightAt = null
    }
    override fun close() { background(); target = null }
}
