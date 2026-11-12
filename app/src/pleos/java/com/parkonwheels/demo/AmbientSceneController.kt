package com.parkonwheels.demo

import ai.pleos.playground.vehicle.api.Light
import ai.pleos.playground.vehicle.api.model.area.SeatArea
import ai.pleos.playground.vehicle.api.model.capability.AccessType
import android.app.Application
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock

/** Only ambient RGB; no headlamps, power, brightness, flashing or automatic drive-mode changes. */
class AmbientSceneController(
    private val application: Application,
    private val light: () -> Light?,
    private val allowed: () -> Boolean
) {
    private val main = Handler(Looper.getMainLooper())
    private var generation = 0L
    private var deadline = 0L
    private var timeout: Runnable? = null
    private val originals = linkedMapOf<SeatArea, Int>()
    private val applied = linkedMapOf<SeatArea, Int>()
    private fun dispatch(token: Long, action: () -> Unit) {
        main.post { if(token == generation && allowed() && SystemClock.elapsedRealtime() <= deadline) action() }
    }
    fun suspendCommands() { generation++; timeout?.let(main::removeCallbacks); timeout = null }
    fun apply(color: Int?, report: (String) -> Unit) {
        suspendCommands()
        val token = generation
        deadline = SystemClock.elapsedRealtime() + 5_000
        fun complete(message: String) {
            if(token != generation) return
            suspendCommands(); report(message)
        }
        if(!allowed()) { complete("주행·연결 확인 중에는 조명 명령을 보내지 않아요. 기존 색은 유지될 수 있어요."); return }
        if(application.checkSelfPermission("pleos.car.permission.CAR_LIGHTS") != PackageManager.PERMISSION_GRANTED ||
            application.checkSelfPermission("pleos.car.permission.CONTROL_CAR_LIGHTS") != PackageManager.PERMISSION_GRANTED) {
            complete("차량 조명 권한이 없어요. 테마 그림으로 체험할 수 있어요."); return
        }
        val api = light() ?: run { complete("차량 조명 연결을 확인해 주세요"); return }
        timeout = Runnable { if(token == generation) complete("조명 응답을 확인하지 못했어요. 적용 여부는 차량에서 확인해 주세요.") }.also { main.postDelayed(it, 5_000) }
        if(color == null) {
            if(originals.isEmpty()) { complete("앱이 바꾼 조명이 없어요"); return }
            val targets = originals.toMap(); var remaining = targets.size; var restored = 0; var respected = 0
            fun finishOne(success: Boolean, manual: Boolean = false) {
                if(success) { if(manual) respected++ else restored++ }
                if(--remaining == 0) complete("이전 색 복원 ${restored}곳 · 직접 바꾼 색 유지 ${respected}곳 · 미확인 ${targets.size - restored - respected}곳")
            }
            for((area, original) in targets) try {
                api.getAmbientLightBrightnessColor(area, onComplete={ current -> dispatch(token) {
                    if(current == null) { finishOne(false) }
                    else if((current and 0xFFFFFF) != applied[area]?.and(0xFFFFFF)) {
                        originals.remove(area); applied.remove(area); finishOne(true, true)
                    } else try { api.setAmbientLightBrightnessColor(area, original,
                        onComplete={ _, actual -> dispatch(token) {
                            val ok = (actual and 0xFFFFFF) == (original and 0xFFFFFF)
                            if(ok) { originals.remove(area); applied.remove(area) }
                            finishOne(ok)
                        } }, onFailed={ dispatch(token) { finishOne(false) } }) }
                    catch(error: Exception) { finishOne(false) }
                } }, onFailed={ dispatch(token) { finishOne(false) } })
            } catch(error: Exception) { finishOne(false) }
            return
        }
        // Official SDK sources specify 32-bit ARGB with alpha fixed to 0xFF.
        val desired = color or 0xFF000000.toInt()
        report("차량의 조명 지원 범위를 확인하고 있어요")
        try { api.checkAmbientLightBrightnessColorCapabilities(onComplete={ config, areas -> dispatch(token) {
            if(config.accessType != AccessType.READ_WRITE || areas.isNullOrEmpty()) { complete("이 차량은 앱의 앰비언트 색 변경을 지원하지 않아요"); return@dispatch }
            val targets = areas.distinct(); var remaining = targets.size; var successes = 0
            fun finishOne(success: Boolean) {
                if(success) successes++
                if(--remaining == 0) complete(if(successes == targets.size) "테마 앰비언트 색 적용 완료 · ${successes}곳 · 주행 중 새 명령 없음" else "테마 색 적용 ${successes}/${targets.size}곳 · 일부 조명은 적용을 확인하지 못했어요")
            }
            for(area in targets) try {
                api.getAmbientLightBrightnessColor(area, onComplete={ previous -> dispatch(token) read@{
                    android.util.Log.i("ParkOnWheels", "ambient original=${previous?.toUInt()?.toString(16)}")
                    if(previous == null) { finishOne(false); return@read }
                    if(!originals.containsKey(area)) originals[area] = previous or 0xFF000000.toInt()
                    try { api.setAmbientLightBrightnessColor(area, desired,
                        onComplete={ _, actual -> dispatch(token) {
                            android.util.Log.i("ParkOnWheels", "ambient acknowledgement desired=${desired.toUInt().toString(16)} actual=${actual.toUInt().toString(16)}")
                            if((actual and 0xFFFFFF) != (desired and 0xFFFFFF)) { finishOne(false) }
                            else {
                                applied[area] = desired
                                // A write acknowledgement can precede the next vehicle-state event.
                                // Retry reads only (never resend writes), within the same parked epoch/deadline.
                                fun verify(attempt: Int) {
                                    main.postDelayed({ dispatch(token) {
                                        try { api.getAmbientLightBrightnessColor(area,
                                            onComplete={ verified -> dispatch(token) {
                                                android.util.Log.i("ParkOnWheels", "ambient readback desired=${desired.toUInt().toString(16)} actual=${verified?.toUInt()?.toString(16)} attempt=$attempt")
                                                if(verified != null && (verified and 0xFFFFFF) == (desired and 0xFFFFFF)) finishOne(true)
                                                else if(attempt < 4) verify(attempt + 1) else finishOne(false)
                                            } }, onFailed={ dispatch(token) { finishOne(false) } }) }
                                        catch(error: Exception) { finishOne(false) }
                                    } }, attempt * 400L)
                                }
                                verify(1)
                            }
                        } }, onFailed={ error -> dispatch(token) { android.util.Log.w("ParkOnWheels", "ambient write failure ${error.javaClass.simpleName}: ${error.message}"); finishOne(false) } }) }
                    catch(error: Exception) { finishOne(false) }
                } }, onFailed={ dispatch(token) { finishOne(false) } })
            } catch(error: Exception) { finishOne(false) }
        } }, onFailed={ dispatch(token) { complete("조명 지원 확인에 실패했어요. 차량 기본 설정을 유지해 주세요.") } }) }
        catch(error: Exception) { complete("조명을 연결하지 못했어요. 차량 기본 설정을 유지해 주세요.") }
    }
}
