package com.example.cameraxcropimage

import android.app.Application
import timber.log.Timber


/**
 * Created by gideon on 05 January 2023
 * gideon@cicil.co.id
 * https://www.cicil.co.id/
 */
class CameraApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree());
        }
    }
}