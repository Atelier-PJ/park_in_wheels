package com.parkonwheels.demo

import android.app.Application
import android.content.Context
import android.os.Build
import androidx.test.runner.AndroidJUnitRunner

/** Exists only in the test APK and refuses physical vehicles. */
class E2eRunner:AndroidJUnitRunner() {
    override fun newApplication(cl:ClassLoader?,className:String?,context:Context?):Application {
        check(Build.HARDWARE.contains("ranchu") || Build.MODEL.contains("emulator",true)){"E2E runner is emulator only"}
        return super.newApplication(cl,E2eApplication::class.java.name,context)
    }
}
internal object E2eRuntime {
    lateinit var model:(Application)->SafariModel
    lateinit var speech:(Context)->SpeechInput?
}
class E2eApplication:Application(),AdventureDependencies {
    override fun createModel(application:Application)=E2eRuntime.model(application)
    override fun createSpeechInput(context:Context)=E2eRuntime.speech(context)
}
