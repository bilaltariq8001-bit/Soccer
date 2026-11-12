package com.innovativeinside.pitchside

import android.app.Application

open class PitchsideApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
    }

    /** Tests can swap the container before the activity starts. */
    fun overrideContainer(c: AppContainer) { container = c }
}
