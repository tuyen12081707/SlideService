package com.panda.slideservice

import android.app.Activity
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Bundle

import java.util.Locale

class App : Application(), Application.ActivityLifecycleCallbacks {
    val channelId: String? = null
    override fun onCreate() {
        super.onCreate()
        val channel = NotificationChannel(
            "vibration_channel",
            "Vibration Channel",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
        registerActivityLifecycleCallbacks(this)
        instance = this
    }


    internal inner class AdjustLifecycleCallbacks : ActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: Activity, bundle: Bundle?) {}
        override fun onActivityStarted(activity: Activity) {}
        override fun onActivityResumed(activity: Activity) {
        }

        override fun onActivityPaused(activity: Activity) {
        }

        override fun onActivityStopped(activity: Activity) {}
        override fun onActivitySaveInstanceState(activity: Activity, bundle: Bundle) {}
        override fun onActivityDestroyed(activity: Activity) {}
    }

    companion object {
        @JvmStatic
        @get:Synchronized
        var instance: App? = null
            private set
    }

    override fun onActivityCreated(p0: Activity, p1: Bundle?) {
        var locale: Locale? = null
        locale = Locale("en")
        Locale.setDefault(locale)
        val resource = p0.resources
        val configuration = resource.configuration
        configuration.setLocale(locale)
        configuration.setLayoutDirection(Locale("en"))
        resource.updateConfiguration(configuration, resource.displayMetrics)
    }

    override fun onActivityStarted(p0: Activity) {

    }

    override fun onActivityResumed(p0: Activity) {

    }

    override fun onActivityPaused(p0: Activity) {

    }

    override fun onActivityStopped(p0: Activity) {

    }

    override fun onActivitySaveInstanceState(p0: Activity, p1: Bundle) {

    }

    override fun onActivityDestroyed(p0: Activity) {

    }
}
