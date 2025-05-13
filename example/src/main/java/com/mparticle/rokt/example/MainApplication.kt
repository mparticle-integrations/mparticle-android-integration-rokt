package com.mparticle.rokt.example

import android.app.Application
import com.mparticle.kits.RoktKit

class MainApplication : Application() {
    val kit = RoktKit()
    override fun onCreate() {
        super.onCreate()
        // Initialize any libraries or SDKs here
        val settings = mapOf("accountId" to "2754655826098840951")
        kit.onKitCreate(settings, this)
    }
}
