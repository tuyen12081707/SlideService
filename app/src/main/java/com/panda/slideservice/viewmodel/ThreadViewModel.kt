package com.panda.slideservice.viewmodel

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.panda.slideservice.service.VibrationService
import java.util.concurrent.atomic.AtomicBoolean


open class ThreadViewModel : ViewModel() {
    var isServiceRunning: MutableLiveData<Boolean> = MutableLiveData(false)

    fun setVibrator(enable: Boolean, context: Context) {
        val serviceIntent = Intent(context, VibrationService::class.java)
        if (enable) {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.S) {
                try {
                    context.startForegroundService(serviceIntent)
                } catch (ignored: Exception) {
                }
            } else {
                try {
                    context.startForegroundService(serviceIntent)
                } catch (ignored: Exception) {
                }
            }
        } else {
            try {
                try {
                    context.stopService(serviceIntent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } catch (e: Exception) {
            }
        }
    }

    private fun startVibrator(context: Context) {
        if (isVibratorRunning.get()) {
            // Vibrator is already running, no need to start a new thread
            return
        }

        vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (vibrator != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                isVibratorRunning.set(true)
                vibratorThread = Thread {
                    while (isVibratorRunning.get()) {
                        try {
                            Thread.sleep(1000)
                            vibrator!!.vibrate(
                                VibrationEffect.createOneShot(
                                    1000,
                                    VibrationEffect.DEFAULT_AMPLITUDE
                                )
                            )
                        } catch (e: InterruptedException) {
                            e.printStackTrace()
                        }
                    }
                    vibrator!!.cancel()
                    isVibratorRunning.set(false)
                }
                vibratorThread!!.start()
            }
        }
    }

    private fun stopVibrator() {
        isVibratorRunning.set(false)
        if (vibratorThread != null) {
            try {
                vibratorThread!!.join() // Wait for the thread to finish
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
            vibratorThread = null
        }
        if (vibrator != null) {
            vibrator!!.cancel()
            vibrator = null
        }
    }

    companion object {
        private var vibrator: Vibrator? = null
        private var vibratorThread: Thread? = null
        private val isVibratorRunning = AtomicBoolean(false)
    }
}