package com.parkonwheels.demo

import android.app.Application
import android.car.Car
import android.car.VehiclePropertyIds
import android.car.hardware.CarPropertyValue
import android.car.hardware.property.CarPropertyManager
import android.car.hardware.property.EvChargeState
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import ai.pleos.playground.navi.data.DestinationInfo
import ai.pleos.playground.navi.data.DestinationArrivedInfo
import ai.pleos.playground.navi.constants.NaviErrorCode
import ai.pleos.playground.navi.constants.NavigationStatus
import ai.pleos.playground.navi.data.RouteStartInfo
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.Executors

fun createTravelContextGateway(application: Application): TravelContextGateway = PleosTravelContextGateway(application)

/** Reads actual charging enum and explicit destination-arrived events. Never writes to a vehicle or route. */
class PleosTravelContextGateway(private val app: Application) : TravelContextGateway {
    private val main = Handler(Looper.getMainLooper())
    private val worker = Executors.newSingleThreadExecutor()
    private var epoch = 0L
    private var active = false
    private var car: Car? = null
    private var manager: CarPropertyManager? = null
    private var navigation: NaviHelper? = null
    private var destinationPoll: Runnable? = null
    private var destinationRequestedAt = -1L
    private var navListener: NaviHelperEventListener? = null
    private var target: TravelContextGateway.Listener? = null
    private var reading = false
    private val prefs = app.getSharedPreferences("travel_privacy", 0)
    private val salt: String = prefs.getString("salt", null) ?: UUID.randomUUID().toString().also { prefs.edit().putString("salt", it).apply() }
    private fun dispatch(token: Long, action: () -> Unit) = main.post { if(active && token == epoch) action() }
    override fun start(charging: Boolean, navigation: Boolean, listener: TravelContextGateway.Listener) {
        stop(); active = true; target = listener
        val token = epoch
        if(charging) {
            if(app.checkSelfPermission(Car.PERMISSION_ENERGY) != PackageManager.PERMISSION_GRANTED) listener.status("충전 상태 읽기 권한이 필요해요")
            else runCatching {
                car = Car.createCar(app, main, Car.CAR_WAIT_TIMEOUT_DO_NOT_WAIT) { connected, ready ->
                    dispatch(token) {
                        manager = if(ready) runCatching { connected.getCarManager(Car.PROPERTY_SERVICE) as CarPropertyManager }.getOrNull() else null
                        if(manager == null) listener.charging(null,SystemClock.elapsedRealtime())
                    }
                }
                main.post(poll)
            }.onFailure { listener.charging(null,SystemClock.elapsedRealtime());listener.status("충전 상태 연결을 지원하지 않아요") }
        }
        if(navigation) {
            if(app.checkSelfPermission("pleos.car.permission.NAVI_ROUTE") != PackageManager.PERMISSION_GRANTED) listener.status("내비게이션 정보 읽기 권한을 확인해 주세요")
            else runCatching {
                val helper=NaviHelper(app);this.navigation=helper
                var observedRoute=false
                val events=object : NaviHelperEventListener {
                    override fun onRouteStarted(routeStartInfo:RouteStartInfo) { dispatch(token) { observedRoute=true;listener.clearArrival();requestDestination() } }
                    override fun onNavigationStatus(status:NavigationStatus) {
                        dispatch(token) { if(status==NavigationStatus.ROUTE_DRIVE) observedRoute=true }
                    }
                    override fun onDestinationArrived(destinationArrivedInfo: DestinationArrivedInfo) {
                        // Salted event digest deduplicates callbacks without retaining a place name/address.
                        val v=destinationArrivedInfo
                        val day=System.currentTimeMillis()/86_400_000
                        if(v.name.isBlank() && v.address.isBlank()) return
                        val raw="$salt|$day|${v.name}|${v.address}"
                        val id=MessageDigest.getInstance("SHA-256").digest(raw.toByteArray()).joinToString(""){"%02x".format(it)}
                        val at=SystemClock.elapsedRealtime()
                        dispatch(token) { if(observedRoute) listener.arrived(id,at) }
                    }
                    override fun onDestinationInfo(destinationInfo: DestinationInfo) {
                        val receivedAt=SystemClock.elapsedRealtime()
                        dispatch(token) {
                            if(destinationRequestedAt>=0 && receivedAt-destinationRequestedAt in 0..10_000) {
                                destinationRequestedAt=-1
                                listener.destination(destinationInfo.name.takeIf{it.isNotBlank() && it.length<=160},receivedAt)
                            }
                        }
                    }
                    override fun onDestinationChanged() { dispatch(token) { listener.clearArrival();listener.destination(null,SystemClock.elapsedRealtime());requestDestination() } }
                    override fun onRouteCancelled() { dispatch(token) { observedRoute=false;listener.clearArrival();listener.destination(null,SystemClock.elapsedRealtime()) } }
                    override fun onError(errorCode: NaviErrorCode) { dispatch(token) { observedRoute=false;listener.clearArrival();listener.destination(null,SystemClock.elapsedRealtime());listener.status("내비 연결이 준비되면 방문 스탬프를 받을 수 있어요") } }
                }
                navListener=events;helper.addListener(events)
                helper.initialize(onConnected={dispatch(token){
                    listener.status("내비 연결 완료 · 여행 정보를 확인해요")
                    destinationPoll=object:Runnable {
                        override fun run() { if(active && token==epoch){requestDestination();main.postDelayed(this,15_000)} }
                    }.also{main.post(it)}
                }},onFailed={dispatch(token){listener.clearArrival();listener.status("내비 서비스 연결을 지원하지 않아요")}})
            }.onFailure { listener.clearArrival();listener.status("내비 서비스 연결을 지원하지 않아요") }
        }
    }
    private fun requestDestination() {
        destinationRequestedAt=SystemClock.elapsedRealtime()
        runCatching{navigation?.getDestinationInfo()}.onFailure{
            destinationRequestedAt=-1;target?.destination(null,SystemClock.elapsedRealtime())
        }
    }
    private val poll=object:Runnable {
        override fun run() {
            if(!active)return
            val service=manager
            if(service != null && !reading) {
                reading=true
                val token=epoch;val at=SystemClock.elapsedRealtime()
                worker.execute {
                    val state=runCatching {
                        val config=service.getCarPropertyConfig(VehiclePropertyIds.EV_CHARGE_STATE)
                        check(config != null && config.access and 1 != 0)
                        val value=service.getProperty<Int>(VehiclePropertyIds.EV_CHARGE_STATE,0)
                        if(value.status != CarPropertyValue.STATUS_AVAILABLE) null else when(value.value) {
                            EvChargeState.STATE_CHARGING -> true
                            EvChargeState.STATE_NOT_CHARGING,EvChargeState.STATE_FULLY_CHARGED -> false
                            else -> null
                        }
                    }.getOrNull()
                    dispatch(token) {
                        reading=false
                        target?.charging(if(SystemClock.elapsedRealtime()-at<=5000)state else null,at)
                    }
                }
            }
            main.postDelayed(this,2000)
        }
    }
    override fun stop() {
        active=false;epoch++;main.removeCallbacks(poll);destinationPoll?.let(main::removeCallbacks);destinationPoll=null;destinationRequestedAt=-1;reading=false
        manager=null;car?.disconnect();car=null
        navigation?.let { helper -> navListener?.let { runCatching { helper.removeListener(it) } };runCatching { helper.release() } }
        navigation=null;navListener=null;target=null
    }
    override fun close() { stop();worker.shutdownNow() }
}
