package com.parkonwheels.demo

import android.app.Application
import android.app.Instrumentation
import android.content.SharedPreferences
import android.os.Bundle
import com.parkonwheels.core.SafariEngine.Phase
import com.parkonwheels.core.VehicleGateway.Gate

/** Native on-device integration checks with isolated preferences and explicit demo inputs. */
class AdventureVerification : Instrumentation() {
    override fun onCreate(arguments: Bundle?) { super.onCreate(arguments); start() }
    override fun onStart() {
        val result = Bundle()
        try {
            check(targetContext.packageName == "com.parkonwheels.demo") { "Verification must run against demo flavor" }
            var assertions = 0
            fun expect(value: Boolean, message: String) { assertions++; check(value) { message } }
            val testApp = object : Application() {
                override fun getSharedPreferences(name: String, mode: Int): SharedPreferences = targetContext.getSharedPreferences("verification_$name", mode)
            }
            runOnMainSync {
                testApp.getSharedPreferences("safari_demo_v1", 0).edit().clear().commit()
                var model = SafariModel(testApp)
                model.foreground(); model.simulateGate(Gate.AVAILABLE)
                expect(model.canPlay, "Explicit demo parked state opens content")
                for ((i, theme) in adventureThemes.withIndex()) {
                    model.chooseTheme(theme.id)
                    expect(model.state.phase == Phase.IDLE, "${theme.id}: selecting a new theme cannot inherit another theme's completed ending")
                    model.chooseMode(ExperienceMode.PARKED); model.chooseRole("꼬마 길잡이")
                    model.start(); expect(model.state.phase == Phase.ACTIVE, "${theme.id}: start")
                    model.hatch(); expect(model.stamps.size == i, "${theme.id}: premature ending blocked")
                    model.answer(0)
                    model.answer(1); expect(model.missionStep == 1, "${theme.id}: repeated tap cannot skip feedback")
                    model.nextMission(); model.answer(1)
                    model.background()
                    val restored = SafariModel(testApp)
                    restored.foreground(); restored.simulateGate(Gate.AVAILABLE)
                    expect(restored.missionStep == 2 && restored.state.phase == Phase.PAUSED, "${theme.id}: persisted chapter and pause")
                    restored.resume(); restored.answer(0); restored.nextMission()
                    expect(restored.canFinish, "${theme.id}: three missions open parked ending")
                    restored.hatch(); restored.hatch()
                    expect(restored.stamps.size == i + 1, "${theme.id}: exactly one persisted stamp")
                    expect(restored.stamps.last().themeId == theme.id && restored.stamps.last().role == "꼬마 길잡이", "${theme.id}: stamp metadata")
                    expect(restored.state.travelledKm == 0.0, "${theme.id}: no fabricated travel")
                    restored.background(); model = SafariModel(testApp); model.foreground(); model.simulateGate(Gate.AVAILABLE)
                    expect(model.stamps.size == i + 1 && model.episode == 2, "${theme.id}: passport restore and next episode")
                    expect(model.missions != theme.missions, "${theme.id}: next episode has different authored content")
                }
                model.chooseTheme("dino"); model.chooseMode(ExperienceMode.DISTANCE); model.start()
                repeat(3) { model.answer(0); model.nextMission() }
                expect(!model.canFinish, "Distance ending cannot use parked mission progress alone")
                model.simulateGate(Gate.RESTRICTED)
                model.addDistance(); repeat(4) { model.addDistance() }
                model.hatch(); expect(model.stamps.size == 4 && !model.canPlay, "Restricted state blocks ending at threshold")
                model.simulateGate(Gate.AVAILABLE)
                expect(model.state.phase == Phase.PAUSED && !model.canFinish, "Permission release requires manual resume")
                model.resume(); expect(model.canFinish, "Distance plus missions plus explicit resume allows ending")
                model.hatch(); expect(model.stamps.size == 5, "Distance route persists exactly one extra stamp")
                model.start(); model.missingDistance()
                expect(!model.canPlay, "Missing odometer blocks even if demo UX remains available")
                expect(model.canResetJourney, "A safe parked state can recover a distance error without opening gameplay")
                model.cancel(); model.simulateGate(Gate.AVAILABLE)
                expect(model.state.phase == Phase.IDLE && model.canPlay, "Distance-error recovery cancels safely and rechecks fresh data")
                model.background()
            }
            lateinit var layoutModel: SafariModel
            runOnMainSync {
                layoutModel = SafariModel(testApp)
                layoutModel.foreground(); layoutModel.simulateGate(Gate.AVAILABLE); layoutModel.cancel()
                layoutModel.chooseTheme("ocean"); layoutModel.chooseMode(ExperienceMode.PARKED)
            }
            verifyAdaptiveLayouts(this, layoutModel)
            runOnMainSync { layoutModel.background() }
            result.putString("stream", "PASS: $assertions on-device model assertions; four themes, mission persistence, passports, next chapters, safety and reward idempotency; adaptive ticket layouts rendered at 420dp and 1180dp\n")
            finish(-1, result)
        } catch (error: Throwable) {
            result.putString("stream", "FAIL: ${error.stackTraceToString()}\n")
            finish(0, result)
        }
    }
}
