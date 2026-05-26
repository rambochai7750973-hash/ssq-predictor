package com.ssq.predictor

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import java.net.CookieManager
import java.net.CookiePolicy

@HiltAndroidApp
class SSQApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CookieManager.setDefault(CookieManager(null, CookiePolicy.ACCEPT_ALL))
    }
}
