package com.parkonwheels.demo

import android.app.Application
import android.content.Context

/** Application-owned dependencies. The normal Application uses the production defaults.
 * No intent, preference or exported component can select a simulated vehicle in Pleos.
 */
internal interface AdventureDependencies {
    fun createModel(application:Application):SafariModel
    fun createSpeechInput(context:Context):SpeechInput?
}
