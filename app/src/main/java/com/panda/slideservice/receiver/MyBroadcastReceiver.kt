package com.panda.slideservice.receiver

import android.app.ActivityManager
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.panda.slideservice.DefaultPreferences
import com.panda.slideservice.MainActivity
import com.panda.slideservice.service.VocalService


class MyBroadcastReceiver : BroadcastReceiver() {
    private var i: Intent? = null
    private var mCtx: Context? = null

    override fun onReceive(context: Context, intent: Intent) {
        mCtx = context

        val stopServiceValue = DefaultPreferences(context).read("StopService", "1")
        if (stopServiceValue != "1") {
            startAlert(context)
            if (!isMyServiceRunning(VocalService::class.java)) {
                i = Intent(context, VocalService::class.java)
                try {
                    context.startForegroundService(i)
                } catch (_: Exception) {
                    // Ignored
                }
            }
        } else {
            Log.i("helloFragment", "helloFragment")
            val clickIntent = Intent(context, MainActivity::class.java).apply {
                putExtra("fragment", "M001FindFrg")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            context.startActivity(clickIntent)
        }
    }

    private fun startAlert(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, MyBroadcastReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(context, 234, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 1000, pendingIntent)
    }

    private fun isMyServiceRunning(cls: Class<*>): Boolean {
        for (runningServiceInfo in (mCtx?.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).getRunningServices(
            Int.MAX_VALUE
        )) {
            if (cls.name == runningServiceInfo.service.className) {
                return true
            }
        }
        return false
    }
}
