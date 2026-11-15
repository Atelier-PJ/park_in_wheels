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
import com.parkonwheels.core.TravelRewardPolicy
import com.parkonwheels.core.ThemeRecommendation
import android.os.SystemClock

data class PassportStamp(val id: String, val themeId: String, val episode: Int, val role: String, val earnedAt: Long, val chargingSpecial: Boolean = false)
data class VisitStamp(val id: String, val themeId: String, val earnedAt: Long)
enum class ExperienceMode { PARKED, DISTANCE }

class SafariModel(application: Application, gatewayOverride: AppVehicleGateway?, preferenceName: String, travelOverride: TravelContextGateway? = null) : AndroidViewModel(application) {
    constructor(application: Application) : this(application, null, "safari_demo_v1")
    private val handler = Handler(Looper.getMainLooper())
    private var cleared = false
    private val playbackStops=mutableSetOf<()->Unit>()
    internal fun onPlaybackBlocked(stop:()->Unit):()->Unit {
        playbackStops.add(stop)
        return { playbackStops.remove(stop) }
    }
    private val prefs = application.getSharedPreferences(preferenceName, 0)
    private val saved = runCatching { JSONObject(prefs.getString("session", "{}")!!) }.getOrElse { JSONObject() }
    var themeId by mutableStateOf(saved.optString("theme", "dino")); private set
    var role by mutableStateOf(saved.optString("role", "공룡 탐험대원")); private set
    var mode by mutableStateOf(runCatching { ExperienceMode.valueOf(saved.optString("mode", if (saved.has("phase")) "DISTANCE" else "PARKED")) }.getOrDefault(ExperienceMode.PARKED)); private set
    var missionStep by mutableStateOf(saved.optInt("mission", 0).coerceIn(0, 3)); private set
    var feedback by mutableStateOf<String?>(null); private set
    var ambientEnabled by mutableStateOf(saved.optBoolean("ambientEnabled", false)); private set
    var ambientStatus by mutableStateOf("테마 조명은 보호자 설정에서 켤 수 있어요"); private set
    private var effectKey: String? = null
    private var starlightScene = false
    private var ambientEpoch = 0L
    companion object { const val STARLIGHT_RGB = 0xFFE5A1 }
    var soundsEnabled by mutableStateOf(saved.optBoolean("soundsEnabled", true)); private set
    var reducedMotion by mutableStateOf(saved.optBoolean("reducedMotion", false)); private set
    var stamps by mutableStateOf(if (saved.has("stamps")) readStamps(saved.optJSONArray("stamps")) else List(saved.optInt("cards", 0).coerceIn(0, 10000)) { PassportStamp("legacy-$it", "dino", it + 1, "공룡 탐험대원", 0) }); private set
    private val travelPolicy = TravelRewardPolicy()
    private val travel = travelOverride ?: createTravelContextGateway(application)
    private var foregroundActive = false
    var chargingEnabled by mutableStateOf(saved.optBoolean("chargingEnabled", false)); private set
    var visitsEnabled by mutableStateOf(saved.optBoolean("visitsEnabled", false)); private set
    var recommendationsEnabled by mutableStateOf(saved.optBoolean("recommendationsEnabled",false)); private set
    var recommendedTheme by mutableStateOf<String?>(null); private set
    var recommendationSource by mutableStateOf(""); private set
    private var manualDestination: String? = null
    private var destinationName: String? = null
    private var destinationAt = -1L
    private var travelEpoch = 0L
    private var recommendationExpiry: Runnable? = null
    private fun updateRecommendation() {
        val fresh=destinationAt>=0 && SystemClock.elapsedRealtime()-destinationAt in 0..45_000
        val name=manualDestination ?: destinationName.takeIf{fresh}
        recommendedTheme=if(recommendationsEnabled && foregroundActive && canPlay)ThemeRecommendation.forDestination(name) else null
        recommendationSource=if(manualDestination!=null)"직접 입력한 여행지" else "내비게이션 목적지"
    }
    fun changeRecommendations(value:Boolean)=act {
        if(canPlay){recommendationsEnabled=value;manualDestination=null;persist(engine.snapshot());connectTravel()}
    }
    fun setTripDestination(name:String)=act {
        if(canPlay && recommendationsEnabled){manualDestination=name.trim().take(160).takeIf{it.isNotBlank()};updateRecommendation()}
    }
    var chargingNow by mutableStateOf(false); private set
    var chargingEpisode by mutableStateOf(saved.optBoolean("chargingEpisode", false)); private set
    var arrivalAvailable by mutableStateOf(false); private set
    var travelStatus by mutableStateOf("보호자가 연결하면 충전 이야기와 방문 스탬프를 사용할 수 있어요"); private set
    var visits by mutableStateOf(buildList {
        val array=saved.optJSONArray("visits")
        if(array!=null) for(i in 0 until array.length()) runCatching {
            val v=array.getJSONObject(i);add(VisitStamp(v.getString("id"),v.getString("theme"),v.getLong("time")))
        }
    }); private set
    private fun refreshTravel() {
        updateRecommendation()
        val now=SystemClock.elapsedRealtime()
        chargingNow=chargingEnabled && travelPolicy.isCharging(now)
        arrivalAvailable=visitsEnabled && travelPolicy.claimable(canPlay,now,visits.map{it.id}.toSet())!=null
    }
    private fun connectTravel() {
        travelEpoch++;travel.stop();travelPolicy.reset();destinationName=null;destinationAt=-1
        recommendationExpiry?.let(handler::removeCallbacks);recommendationExpiry=null;refreshTravel()
        if(!foregroundActive || (!chargingEnabled && !visitsEnabled && !recommendationsEnabled))return
        val token=travelEpoch
        travel.start(chargingEnabled,visitsEnabled || recommendationsEnabled,object:TravelContextGateway.Listener {
            override fun charging(value:Boolean?,observedAt:Long)=main { if(token==travelEpoch && foregroundActive){travelPolicy.charging(value,observedAt);refreshTravel()} }
            override fun arrived(opaqueId:String,observedAt:Long)=main { if(token==travelEpoch && foregroundActive){travelPolicy.arrived(opaqueId,observedAt);refreshTravel()} }
            override fun clearArrival()=main { if(token==travelEpoch && foregroundActive){travelPolicy.clearArrival();refreshTravel()} }
            override fun destination(name:String?,observedAt:Long)=main {
                if(token==travelEpoch && foregroundActive && recommendationsEnabled) {
                    destinationName=name?.takeIf{it.length<=160};destinationAt=observedAt
                    recommendationExpiry?.let(handler::removeCallbacks)
                    recommendationExpiry=Runnable{updateRecommendation()}.also{handler.postDelayed(it,45_001)}
                    updateRecommendation()
                }
            }
            override fun status(message:String)=main { if(token==travelEpoch)travelStatus=message }
        })
    }
    fun reconnectTravel() { connectTravel() }
    fun changeCharging(value:Boolean)=act { if(canPlay) { chargingEnabled=value;persist(engine.snapshot());connectTravel() } }
    fun changeVisits(value:Boolean)=act { if(canPlay) { visitsEnabled=value;persist(engine.snapshot());connectTravel() } }
    fun claimVisit()=act {
        val key=travelPolicy.claimable(canPlay,SystemClock.elapsedRealtime(),visits.map{it.id}.toSet())
        if(!visitsEnabled || key==null)return@act
        val previous=visits
        try {
            visits=visits+VisitStamp(key,theme.id,System.currentTimeMillis())
            persist(engine.snapshot());travelPolicy.clearArrival()
        } catch(e:Exception) { visits=previous;throw e }
        refreshTravel()
    }
    private var savedCardCount = saved.optInt("cards", 0)
    private fun readStamps(array: JSONArray?): List<PassportStamp> = buildList {
        if (array != null) for (i in 0 until array.length()) runCatching {
            val item = array.getJSONObject(i)
            add(PassportStamp(item.getString("id"), item.getString("theme"), item.getInt("episode"), item.getString("role"), item.getLong("time"),item.optBoolean("chargingSpecial",false)))
        }
    }
    private fun load(): SafariEngine.Snapshot = runCatching {
        SafariEngine.Snapshot(SafariEngine.Phase.valueOf(saved.getString("phase")), saved.getDouble("start"), saved.getDouble("travelled"), saved.getInt("cards"))
    }.getOrElse { SafariEngine.Snapshot.empty() }
    private fun persist(next: SafariEngine.Snapshot) {
        // Session, mission and earned stamp share one commit, so restart cannot duplicate a reward.
        val earned = if (next.cards > savedCardCount) stamps + PassportStamp(UUID.randomUUID().toString(), theme.id, episode, role, System.currentTimeMillis(),chargingEpisode) else stamps
        val array = JSONArray()
        earned.forEach { array.put(JSONObject().put("id", it.id).put("theme", it.themeId).put("episode", it.episode).put("role", it.role).put("time", it.earnedAt).put("chargingSpecial",it.chargingSpecial)) }
        val visitArray=JSONArray();visits.forEach { visitArray.put(JSONObject().put("id",it.id).put("theme",it.themeId).put("time",it.earnedAt)) }
        val json = JSONObject().put("recommendationsEnabled",recommendationsEnabled).put("visits",visitArray).put("chargingEnabled",chargingEnabled).put("visitsEnabled",visitsEnabled).put("chargingEpisode",chargingEpisode).put("phase", next.phase.name).put("start", next.startKm).put("travelled", next.travelledKm).put("cards", next.cards)
            .put("theme", themeId).put("role", role).put("mode", mode.name).put("mission", missionStep)
            .put("soundsEnabled", soundsEnabled).put("reducedMotion", reducedMotion).put("ambientEnabled", ambientEnabled).put("stamps", array)
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
    private val vehicle: AppVehicleGateway = gatewayOverride ?: createVehicleGateway(application) { message ->
        main { diagnostic = message; Log.i("ParkOnWheels", message) }
    }
    val theme get() = adventureThemes.firstOrNull { it.id == themeId } ?: adventureThemes.first()
    val reward get() = if(themeId=="space" && chargingEpisode) "우주정비 배지" else theme.reward
    val episode get() = stamps.count { it.themeId == theme.id } + 1
    val missions get() = if(themeId=="space" && chargingEpisode) chargingStationMissions else theme.chapterMissions(episode)
    val modeLabel get() = vehicle.label
    val isSimulation get() = vehicle.simulation != null
    val canPlay get() = gate == VehicleGateway.Gate.AVAILABLE && hasDistance && error == null
    val canResetJourney get() = gate == VehicleGateway.Gate.AVAILABLE && error == null
    val inJourney get() = state.phase == SafariEngine.Phase.ACTIVE || state.phase == SafariEngine.Phase.PAUSED
    val canFinish get() = canPlay && missionStep == 3 && state.phase == SafariEngine.Phase.ACTIVE && (mode == ExperienceMode.PARKED || engine.readyToHatch())
    val progress get() = if (mode == ExperienceMode.PARKED) missionStep / 3f else (state.travelledKm / 5.0).toFloat().coerceIn(0f, 1f)
    init {
        vehicle.start(object : VehicleGateway.Listener {
            override fun onGate(value: VehicleGateway.Gate) = main { act {
                if(value != engine.gate()) Log.i("ParkOnWheels", "journey gate=${engine.gate()}->$value phase=${engine.snapshot().phase} mission=$missionStep")
                if (value != VehicleGateway.Gate.AVAILABLE) feedback = null
                engine.onGate(value)
            } }
            override fun onOdometer(kilometres: Double?) = main { act { engine.onOdometer(kilometres) } }
        })
    }
    private fun main(action: () -> Unit) {
        if (cleared) return
        if (Looper.myLooper() == Looper.getMainLooper()) action() else handler.post { if (!cleared) action() }
    }
    private fun refresh() {
        state = engine.snapshot(); gate = engine.gate(); hasDistance = engine.hasDistance()
        refreshTravel()
        if (!canPlay) { playbackStops.toList().forEach{it()}; effectKey = null; ambientEpoch++; starlightScene = false; if(ambientEnabled) ambientStatus = "주행·연결 확인 중에는 조명 명령을 멈춰요" }
        else if(ambientEnabled) {
            val key = if(theme.id=="magic" && starlightScene) "magic:starlight" else theme.id
            if(effectKey != key) {
                effectKey = key
                val color = if(starlightScene && theme.id=="magic") STARLIGHT_RGB else when(theme.id) { "magic" -> 0xAC85DC; "space" -> 0x719EE9; "ocean" -> 0x55B9BF; else -> 0x74B37C }
                requestAmbient(color)
            }
        }
    }
    private fun requestAmbient(color: Int?) {
        val token=++ambientEpoch
        try {
            vehicle.ambientColor(color) { text -> main {
                if(token==ambientEpoch && canPlay) {
                    ambientStatus=text
                    Log.i("ParkOnWheels", "ambient: $text")
                }
            } }
        } catch(error: Exception) {
            ambientStatus="조명은 쉬어 가요. 스탬프는 받을 수 있어요."
            Log.w("ParkOnWheels", "ambient request unavailable")
        }
    }
    /** Called only after VoiceDirector matches the authored final phrase, never from touch fallback. */
    fun completeRecognizedFinale(): Boolean {
        if(!canFinish) {
            Log.i("ParkOnWheels", "finale blocked phase=${state.phase} gate=$gate distance=$hasDistance mission=$missionStep")
            return false
        }
        if(theme.id=="magic" && ambientEnabled) {
            starlightScene=true
            effectKey="magic:starlight"
            requestAmbient(STARLIGHT_RGB)
        }
        hatch()
        return state.phase == SafariEngine.Phase.COMPLETE
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
        chargingEpisode=false
        starlightScene=false; ambientEpoch++
        themeId = nextTheme.id
        role = theme.role; missionStep = 0; feedback = null; persist(engine.snapshot())
    }
    fun chooseRole(value: String) = act { if (canPlay && !inJourney) { role = value; persist(engine.snapshot()) } }
    fun chooseMode(value: ExperienceMode) = act { if (canPlay && !inJourney) { mode = value; persist(engine.snapshot()) } }
    fun changeAmbient(value: Boolean) = act {
        if (!canPlay) return@act
        ambientEnabled = value; effectKey = null; starlightScene=false; ambientEpoch++; persist(engine.snapshot())
        if(!value) requestAmbient(null)
    }
    fun retryAmbient() { if(canPlay) { effectKey = null; refresh() } }
    fun changeSounds(value: Boolean) = act { if(canPlay) { soundsEnabled=value; persist(engine.snapshot()) } }
    fun changeReducedMotion(value: Boolean) = act { if (canPlay) { reducedMotion = value; persist(engine.snapshot()) } }
    fun start() = act {
        if (!canPlay || !hasDistance || inJourney) return@act
        starlightScene=false; effectKey=null; ambientEpoch++
        chargingEpisode=themeId=="space" && chargingEnabled && travelPolicy.isCharging(SystemClock.elapsedRealtime())
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
            starlightScene=false; effectKey=null; ambientEpoch++
            val reconnect = !engine.hasDistance()
            engine.cancel(); missionStep = 0; feedback = null; persist(engine.snapshot())
            if(reconnect) { vehicle.background(); vehicle.foreground() }
        }
    }
    fun erasePassport() = act {
        if (!canPlay) return@act
        val empty = SafariEngine.Snapshot.empty()
        val previous = stamps; val previousCount = savedCardCount;val previousVisits=visits
        try { visits=emptyList();travelPolicy.reset();stamps = emptyList(); savedCardCount = 0; missionStep = 0; persist(empty); engine = SafariEngine(empty, ::persist); vehicle.background(); vehicle.foreground() }
        catch (ex: Exception) { stamps = previous; visits=previousVisits;savedCardCount = previousCount; throw ex }
    }
    fun simulateGate(next: VehicleGateway.Gate) = act { vehicle.simulation?.gate(next) }
    fun addDistance() = act { vehicle.simulation?.addKilometres(1.0) }
    fun missingDistance() = act { vehicle.simulation?.missingDistance() }
    fun foreground() = act { foregroundActive=true;vehicle.foreground();connectTravel() }
    fun background() = act { foregroundActive=false;travelEpoch++;manualDestination=null;destinationName=null;destinationAt=-1;recommendationExpiry?.let(handler::removeCallbacks);recommendationExpiry=null;travel.stop();travelPolicy.reset();refreshTravel();feedback = null; engine.onGate(VehicleGateway.Gate.UNKNOWN); vehicle.background() }
    override fun onCleared() { cleared = true; playbackStops.clear(); travel.close();vehicle.close(); handler.removeCallbacksAndMessages(null) }
}
