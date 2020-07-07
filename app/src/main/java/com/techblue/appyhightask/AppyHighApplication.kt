package com.techblue.appyhightask

import android.app.Application
import com.google.android.gms.ads.MobileAds

class AppyHighApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        MobileAds.initialize(applicationContext)
    }
}