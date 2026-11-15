package com.parkonwheels.demo
import android.app.Application
fun createTravelContextGateway(application: Application): TravelContextGateway = object : TravelContextGateway {
    override fun start(charging: Boolean, navigation: Boolean, listener: TravelContextGateway.Listener) {
        listener.status("데모 빌드 · 충전과 목적지 도착은 실제 차량에서 확인해요")
        listener.charging(null, android.os.SystemClock.elapsedRealtime())
    }
    override fun stop() { }
}
