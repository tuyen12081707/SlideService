package com.panda.slideservice.service

import android.Manifest
import android.R
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import java.util.concurrent.atomic.AtomicBoolean

class VibrationService : Service() {
    var vibrator: Vibrator? = null
    var vibratorThread: Thread? = null
    var isVibratorRunning: AtomicBoolean = AtomicBoolean(false)


    override fun onCreate() {
        super.onCreate()
        val notification = createNotification()
        createNotificationChannel()
        try {
            startForeground(1, notification)
        } catch (ignored: Exception) {
        }
    }

    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
            "vibration_channel",
            "Vibration Service Channel",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(serviceChannel)
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        startVibrator()
        return START_STICKY
    }

    override fun onDestroy() {
        stopVibrator()
        stopSelf()
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    private fun startVibrator() {
        if (isVibratorRunning.get()) {
            return
        }
        vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
        if (vibrator != null) {
            isVibratorRunning.set(true)
            vibratorThread = Thread {
                while (isVibratorRunning.get()) {
                    try {
                        Thread.sleep(1000)
                        vibrator?.vibrate(
                            VibrationEffect.createOneShot(
                                1000,
                                VibrationEffect.DEFAULT_AMPLITUDE
                            )
                        )
                    } catch (ignored: Exception) {
                    }
                }
            }
            vibratorThread!!.start()
        }
    }

    private fun stopVibrator() {
        isVibratorRunning.set(false)
        if (vibratorThread != null) {
            try {
                vibratorThread!!.join() // Wait for the thread to finish
            } catch (ignored: Exception) {
            }
            vibratorThread = null
        }
        if (vibrator != null) {
            vibrator!!.cancel()
            vibrator = null
        }

        // Stop the foreground service and remove the notification
        stopForeground(true)
    }

    private fun createNotification(): Notification {
        val builder: NotificationCompat.Builder =
            NotificationCompat.Builder(this, "vibration_channel")
                .setContentTitle("Vibration Service")
                .setContentText("Vibration in progress")
                .setSmallIcon(R.mipmap.sym_def_app_icon)

        return builder.build()
    }
}