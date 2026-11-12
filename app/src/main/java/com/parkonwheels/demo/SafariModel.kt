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
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class PassportStamp(val id: String, val themeId: String, val episode: Int, val role: String, val earnedAt: Long)
enum class ExperienceMode { PARKED, DISTANCE }

class SafariModel(application: Application) : AndroidViewModel(application) {
    private val handler = Handler(Looper.getMainLooper())
    private var cleared = false
    private val prefs = application.getSharedPreferences("safari_demo_v1", 0)
    private val saved = runCatching { JSONObject(prefs.getString("session", "{}")!!) }.getOrElse { JSONObject() }
    var themeId by mutableStateOf(saved.optString("theme", "dino")); private set
    var role by mutableStateOf(saved.optString("role", "공룡 탐험대원")); private set
    var mode by mutableStateOf(runCatching { ExperienceMode.valueOf(saved.optString("mode", if (saved.has("phase")) "DISTANCE" else "PARKED")) }.getOrDefault(ExperienceMode.PARKED)); private set
    var missionStep by mutableStateOf(saved.optInt("mission", 0).coerceIn(0, 3)); private set
    var feedback by mutableStateOf<String?>(null); private set
    var ambientEnabled by mutableStateOf(saved.optBoolean("ambientEnabled", false)); private set
    var ambientStatus by mutableStateOf("테마 조명은 보호자 설정에서 켤 수 있어요"); private set
    private var effectKey: String? = null
    var reducedMotion by mutableStateOf(saved.optBoolean("reducedMotion", false)); private set
    var stamps by mutableStateOf(if (saved.has("stamps")) readStamps(saved.optJSONArray("stamps")) else List(saved.optInt("cards", 0).coerceIn(0, 10000)) { PassportStamp("legacy-$it", "dino", it + 1, "공룡 탐험대원", 0) }); private set
    private var savedCardCount = saved.optInt("cards", 0)
    private fun readStamps(array: JSONArray?): List<PassportStamp> = buildList {
        if (array != null) for (i in 0 until array.length()) runCatching {
            val item = array.getJSONObject(i)
            add(PassportStamp(item.getString("id"), item.getString("theme"), item.getInt("episode"), item.getString("role"), item.getLong("time")))
        }
    }
    private fun load(): SafariEngine.Snapshot = runCatching {
        SafariEngine.Snapshot(SafariEngine.Phase.valueOf(saved.getString("phase")), saved.getDouble("start"), saved.getDouble("travelled"), saved.getInt("cards"))
    }.getOrElse { SafariEngine.Snapshot.empty() }
    private fun persist(next: SafariEngine.Snapshot) {
        // Session, mission and earned stamp share one commit, so restart cannot duplicate a reward.
        val earned = if (next.cards > savedCardCount) stamps + PassportStamp(UUID.randomUUID().toString(), theme.id, episode, role, System.currentTimeMillis()) else stamps
        val array = JSONArray()
        earned.forEach { array.put(JSONObject().put("id", it.id).put("theme", it.themeId).put("episode", it.episode).put("role", it.role).put("time", it.earnedAt)) }
        val json = JSONObject().put("phase", next.phase.name).put("start", next.startKm).put("travelled", next.travelledKm).put("cards", next.cards)
            .put("theme", themeId).put("role", role).put("mode", mode.name).put("mission", missionStep)
            .put("reducedMotion", reducedMotion).put("ambientEnabled", ambientEnabled).put("stamps", array)
        check(prefs.edit().putString("session", json.toString()).commit()) { "진행 저장 실패" }
        savedCardCount = next.cards
        stamps = earned
    }
    private var engine = SafariEngine(load(), ::persist)
    var state by mutableStateOf(engine.snapshot()); private set
    var gate by mutableStateOf(VehicleGateway.Gate.UNKNOWN); private set
    var hasDistance by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set
    var diagnostic by mutableStateOf("연결 확인 전"); private set
    private val vehicle: AppVehicleGateway = createVehicleGateway(application) { message ->
        main { diagnostic = message; Log.i("ParkOnWheels", message) }
    }
    val theme get() = adventureThemes.firstOrNull { it.id == themeId } ?: adventureThemes.first()
    val episode get() = stamps.count { it.themeId == theme.id } + 1
    val missions get() = theme.chapterMissions(episode)
    val modeLabel get() = vehicle.label
    val isSimulation get() = vehicle.simulation != null
    val canPlay get() = gate == VehicleGateway.Gate.AVAILABLE && hasDistance && error == null
    val canResetJourney get() = gate == VehicleGateway.Gate.AVAILABLE && error == null
    val inJourney get() = state.phase == SafariEngine.Phase.ACTIVE || state.phase == SafariEngine.Phase.PAUSED
    val canFinish get() = canPlay && missionStep == 3 && state.phase == SafariEngine.Phase.ACTIVE && (mode == ExperienceMode.PARKED || engine.readyToHatch())
    val progress get() = if (mode == ExperienceMode.PARKED) missionStep / 3f else (state.travelledKm / 5.0).toFloat().coerceIn(0f, 1f)
    init {
        vehicle.start(object : VehicleGateway.Listener {
            override fun onGate(value: VehicleGateway.Gate) = main { act { if (value != VehicleGateway.Gate.AVAILABLE) feedback = null; engine.onGate(value) } }
            override fun onOdometer(kilometres: Double?) = main { act { engine.onOdometer(kilometres) } }
        })
    }
    private fun main(action: () -> Unit) {
        if (cleared) return
        if (Looper.myLooper() == Looper.getMainLooper()) action() else handler.post { if (!cleared) action() }
    }
    private fun refresh() {
        state = engine.snapshot(); gate = engine.gate(); hasDistance = engine.hasDistance()
        if (!canPlay) { effectKey = null; if(ambientEnabled) ambientStatus = "주행·연결 확인 중에는 조명 명령을 멈춰요" }
        else if(ambientEnabled) {
            val key = theme.id
            if(effectKey != key) {
                effectKey = key
                val color = when(theme.id) { "magic" -> 0xAC85DC; "space" -> 0x719EE9; "ocean" -> 0x55B9BF; else -> 0x74B37C }
                vehicle.ambientColor(color) { text -> main { ambientStatus = text; Log.i("ParkOnWheels", "ambient: $text") } }
            }
        }
    }
    private fun act(action: () -> Unit) {
        try { action() } catch (ex: Exception) {
            error = "기록을 저장하지 못했어요. 보호자와 저장 공간을 확인해 주세요."
            Log.e("ParkOnWheels", "session operation failed", ex)
        } finally { refresh() }
    }
    fun chooseTheme(id: String) = act {
        if (!canPlay || inJourney) return@act
        val nextTheme = adventureThemes.firstOrNull { it.id == id } ?: return@act
        if (engine.snapshot().phase == SafariEngine.Phase.COMPLETE) engine.cancel()
        themeId = nextTheme.id
        role = theme.role; missionStep = 0; feedback = null; persist(engine.snapshot())
    }
    fun chooseRole(value: String) = act { if (canPlay && !inJourney) { role = value; persist(engine.snapshot()) } }
    fun chooseMode(value: ExperienceMode) = act { if (canPlay && !inJourney) { mode = value; persist(engine.snapshot()) } }
    fun changeAmbient(value: Boolean) = act {
        if (!canPlay) return@act
        ambientEnabled = value; effectKey = null; persist(engine.snapshot())
        if(!value) vehicle.ambientColor(null) { text -> main { ambientStatus = text; Log.i("ParkOnWheels", "ambient: $text") } }
    }
    fun retryAmbient() { if(canPlay) { effectKey = null; refresh() } }
    fun changeReducedMotion(value: Boolean) = act { if (canPlay) { reducedMotion = value; persist(engine.snapshot()) } }
    fun start() = act {
        if (!canPlay || !hasDistance || inJourney) return@act
        missionStep = 0; feedback = null; engine.start()
    }
    fun answer(index: Int) = act {
        if (!canPlay || state.phase != SafariEngine.Phase.ACTIVE || feedback != null || missionStep >= 3) return@act
        val mission = missions[missionStep]
        if (index !in mission.choices.indices) return@act
        missionStep++; persist(engine.snapshot()); feedback = mission.response
    }
    fun nextMission() { if (canPlay) feedback = null }
    fun resume() = act { if (canPlay) { feedback = null; engine.resume() } }
    fun pause() = act { feedback = null; engine.pause() }
    fun hatch() = act {
        if (!canFinish) return@act
        if (mode == ExperienceMode.PARKED) engine.finishParkedExperience(true) else engine.hatch()
        feedback = null
    }
    fun cancel() = act {
        if (canResetJourney) {
            val reconnect = !engine.hasDistance()
            engine.cancel(); missionStep = 0; feedback = null; persist(engine.snapshot())
            if(reconnect) { vehicle.background(); vehicle.foreground() }
        }
    }
    fun erasePassport() = act {
        if (!canPlay) return@act
        val empty = SafariEngine.Snapshot.empty()
        val previous = stamps; val previousCount = savedCardCount
        try { stamps = emptyList(); savedCardCount = 0; missionStep = 0; persist(empty); engine = SafariEngine(empty, ::persist); vehicle.background(); vehicle.foreground() }
        catch (ex: Exception) { stamps = previous; savedCardCount = previousCount; throw ex }
    }
    fun simulateGate(next: VehicleGateway.Gate) = act { vehicle.simulation?.gate(next) }
    fun addDistance() = act { vehicle.simulation?.addKilometres(1.0) }
    fun missingDistance() = act { vehicle.simulation?.missingDistance() }
    fun foreground() = act { vehicle.foreground() }
    fun background() = act { feedback = null; engine.onGate(VehicleGateway.Gate.UNKNOWN); vehicle.background() }
    override fun onCleared() { cleared = true; vehicle.close(); handler.removeCallbacksAndMessages(null) }
}
