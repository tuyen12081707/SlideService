package com.panda.slideservice.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import android.widget.RemoteViews
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.panda.slideservice.DefaultPreferences
import com.panda.slideservice.DetectClapClap
import com.panda.slideservice.DetectorThread

import com.panda.slideservice.MainActivity
import com.panda.slideservice.OnSignalsDetectedListener
import com.panda.slideservice.R
import com.panda.slideservice.RecorderThread
import java.util.Timer
import java.util.TimerTask


class VocalService : Service(), OnSignalsDetectedListener {
    private var classesApp: DefaultPreferences? = null
    private var detectorThread: DetectorThread? = null
    private var recorderThread: RecorderThread? = null


    private var counter = 0
    private var timer: Timer? = null
    private var timerTask: TimerTask? = null

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent, i: Int, i2: Int): Int {
        startTimer()
        Log.i("onstartCommand", "onstartCommand")
        startDetection()
        return START_STICKY
    }

    fun startDetection() {
        try {
            DetectClapClap(applicationContext).listen()
            this.classesApp = DefaultPreferences(this)
            Log.i("startDetection", "startDetection")

            classesApp?.save("detectClap", "0")
        } catch (unused: Exception) {
            Toast.makeText(this, "Recorder not supported by this device", Toast.LENGTH_LONG).show()
        }
    }

    fun stopDectection() {
        try {
            this.classesApp = DefaultPreferences(this)
            Log.i("stopDectection", "stopDectection")

            classesApp?.save("detectClap", "1")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopTimerTask()
        stopSelf()
        stopDectection()

        val recorderThread: RecorderThread? = this.recorderThread
        if (recorderThread != null) {
            recorderThread.stopRecording()
            Log.d("Hello", "record thread")
            this.recorderThread = null
            recorderThread.interrupt()
        }
        val detectorThread: DetectorThread? = this.detectorThread
        if (detectorThread != null) {
            detectorThread.stopDetection()
            Log.d("Hello", "dectect thread")
            this.detectorThread = null
            detectorThread.interrupt()
        }
        selectedDetection = 0
    }

    override fun onWhistleDetected() {
        Log.i("onWhistleDetected", "onWhistleDetected")
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("fragment", "M001FindFrg")
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        stopSelf()
    }

    fun startTimer() {
        timer = Timer()
        timerTask = object : TimerTask() {
            override fun run() {
                counter++
            }
        }
        timer?.schedule(timerTask, 3000, 3000)
    }

    fun stopTimerTask() {
        if (timer != null) {
            timer!!.cancel()
            timer = null
        }
    }

    override fun onCreate() {
        super.onCreate()

        startMyOwnForeground()
    }

    private fun startMyOwnForeground() {
        Log.e("TAG", "startMyOwnForeground: " + "1")

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // Create a notification channel
        val notificationChannel =
            NotificationChannel("VIBRATION_SERVICE", "Secret Camera", NotificationManager.IMPORTANCE_HIGH)
        notificationChannel.lightColor = -16776961
        notificationChannel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        notificationManager.createNotificationChannel(notificationChannel)


        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("NotificationMessage", true)
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        val notificationLayout = RemoteViews(packageName, R.layout.notification_layout)
        notificationLayout.setTextViewText(
            R.id.notification_title,
            "Clap to find my phone is running"
        )
        notificationLayout.setOnClickPendingIntent(R.id.notification_layout, pendingIntent)
        val builder: NotificationCompat.Builder = NotificationCompat.Builder(this, "CHANNEL_ID")
            .setOngoing(true)
            .setSmallIcon(R.drawable.ic_clap_notify)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContent(notificationLayout)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(pendingIntent)
        startForeground(2, builder.build())

        Log.e("TAG", "startMyOwnForeground: " + "2")
    }


    companion object {
        const val DETECT_NONE: Int = 0
        const val DETECT_WHISTLE: Int = 1
        private const val NOTIFICATION_Id = 1

        var selectedDetection: Int = 0
    }
}