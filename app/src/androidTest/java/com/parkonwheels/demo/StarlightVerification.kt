package com.parkonwheels.demo

import android.app.Application
import android.app.Instrumentation
import android.os.Bundle
import com.parkonwheels.core.VehicleGateway

/** Isolated model + fake light gateway. Does not assert physical vehicle operation. */
class StarlightVerification : Instrumentation() {
    override fun onCreate(arguments:Bundle?) {super.onCreate(arguments);start()}
    override fun onStart() {
        val evidence=StringBuilder();var success=false
        try {
            runOnMainSync {
                val app=targetContext.applicationContext as Application
                var serial=0
                fun scenario(optIn:Boolean=true, theme:String="magic", test:(SafariModel,FakeLight)->Unit) {
                    val pref="starlight_test_${System.nanoTime()}_${serial++}"
                    app.getSharedPreferences(pref,0).edit().clear().commit()
                    val gateway=FakeLight()
                    val model=SafariModel(app,gateway,pref)
                    try {
                        check(model.canPlay)
                        model.chooseTheme(theme);model.changeAmbient(optIn);model.start()
                        repeat(3){model.answer(0);model.nextMission()}
                        check(model.canFinish)
                        gateway.colors.clear()
                        gateway.beforeWrite={check(model.stamps.isEmpty()) {"Light request must precede stamp"}}
                        test(model,gateway)
                    } finally {model.background();app.getSharedPreferences(pref,0).edit().clear().commit()}
                }
                scenario {m,g->
                    m.completeRecognizedFinale()
                    check(g.colors==listOf(SafariModel.STARLIGHT_RGB));check(m.stamps.size==1)
                    m.completeRecognizedFinale();check(g.colors.size==1 && m.stamps.size==1)
                    evidence.append("PASS recognized finale requests starlight before one stamp; no refresh overwrite or duplicate\n")
                }
                scenario(false){m,g->m.completeRecognizedFinale();check(g.colors.isEmpty());check(m.stamps.size==1);evidence.append("PASS opt-out awards stamp without light write\n")}
                scenario {m,g->g.failure=true;m.completeRecognizedFinale();check(m.stamps.size==1);evidence.append("PASS unsupported light response does not block reward\n")}
                scenario {m,g->g.throwFailure=true;m.completeRecognizedFinale();check(m.stamps.size==1);evidence.append("PASS gateway exception does not block reward\n")}
                scenario {m,g->g.listener.onGate(VehicleGateway.Gate.RESTRICTED);m.completeRecognizedFinale();check(g.colors.isEmpty() && m.stamps.isEmpty());evidence.append("PASS restricted blocks light and reward\n")}
                scenario {m,g->g.listener.onOdometer(null);m.completeRecognizedFinale();check(g.colors.isEmpty() && m.stamps.isEmpty());evidence.append("PASS missing distance blocks light and reward\n")}
                scenario {m,g->m.hatch();check(g.colors.isEmpty() && m.stamps.size==1);evidence.append("PASS touch completion has no voice light effect\n")}
                scenario(theme="ocean"){m,g->m.completeRecognizedFinale();check(g.colors.isEmpty() && m.stamps.size==1);evidence.append("PASS other theme does not request starlight\n")}
                scenario {m,g->g.delayed=true;m.completeRecognizedFinale();m.background();val backgroundStatus=m.ambientStatus;g.late?.invoke("stale success");check(m.ambientStatus==backgroundStatus && m.ambientStatus!="stale success");evidence.append("PASS stale light report ignored after background\n")}
                scenario {m,g->m.pause();m.completeRecognizedFinale();check(g.colors.isEmpty() && m.stamps.isEmpty());evidence.append("PASS paused completion ignored\n")}
            }
            success=true
        } catch(t:Throwable) {evidence.append("FAIL ${t.message}\n")}
        finish(if(success)-1 else 0,Bundle().apply{putString("stream",evidence.toString())})
    }
    companion object {
        /** Invoked on the main thread only after the real SDK returns the expected final phrase. */
        fun verifyMatchedPhrase(app:Application) {
            val pref="starlight_pcm_test_${System.nanoTime()}"
            val gateway=FakeLight()
            val model=SafariModel(app,gateway,pref)
            try {
                model.chooseTheme("magic");model.changeAmbient(true);model.start()
                repeat(3){model.answer(0);model.nextMission()}
                gateway.colors.clear()
                model.completeRecognizedFinale()
                check(gateway.colors==listOf(SafariModel.STARLIGHT_RGB) && model.stamps.size==1)
            } finally {model.background();app.getSharedPreferences(pref,0).edit().clear().commit()}
        }
    }
    private class FakeLight:AppVehicleGateway {
        override val label="isolated test gateway"
        lateinit var listener:VehicleGateway.Listener
        val colors=mutableListOf<Int?>()
        var failure=false;var throwFailure=false;var delayed=false
        var late:((String)->Unit)?=null
        var beforeWrite:(()->Unit)?=null
        override fun start(listener:VehicleGateway.Listener){this.listener=listener;listener.onGate(VehicleGateway.Gate.AVAILABLE);listener.onOdometer(100.0)}
        override fun ambientColor(color:Int?,report:(String)->Unit){
            beforeWrite?.invoke();colors.add(color)
            if(throwFailure)throw IllegalStateException("test light failure")
            if(delayed){late=report;return}
            report(if(failure)"unsupported" else "applied")
        }
        override fun foreground()=Unit
        override fun background()=Unit
        override fun close()=Unit
    }
}
