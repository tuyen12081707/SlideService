package com.panda.slideservice

import android.Manifest
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.media.AudioManager.OnAudioFocusChangeListener
import android.media.AudioRecord
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import be.hogent.tarsos.dsp.AudioEvent
import be.hogent.tarsos.dsp.AudioFormat
import be.hogent.tarsos.dsp.onsets.OnsetHandler
import be.hogent.tarsos.dsp.onsets.PercussionOnsetDetector
import com.panda.slideservice.service.VibrationService
import com.panda.slideservice.viewmodel.ThreadViewModel

class DetectClapClap(context: Context) : ThreadViewModel(), OnsetHandler {

    private val buffer: ByteArray
    private var recorder: AudioRecord
    private var mPercussionOnsetDetector: PercussionOnsetDetector
    private var defaultPreferences: DefaultPreferences
    @SuppressLint("StaticFieldLeak")
    private val mContext: Context = context.applicationContext
    private var cameraId: String? = null
    private var camManager: CameraManager? = null

    private var flashbox: String? = null
    private var vibratebox: String? = null
    private var soundbox: String? = null

    private var clap = 0
    private val nb_claps = 3
    private var rateSupported = 0
    private var rate_send = false
    var flashRunnable: Runnable? = null
    var flashOn: Boolean = true

    override fun handleOnset(d: Double, d2: Double) {
        clap++
        clapping++
        if (clapping == 5 && !isClapped && hasAudioFocus()) {
            isClapped = true
            clapping = 0
            defaultPreferences.save("detectClap", "1")

            Log.i("detect clap calp", "vocalcalp")
            flashbox = defaultPreferences.read("flashbox", "1")
            vibratebox = defaultPreferences.read("vibratebox", "1")
            soundbox = defaultPreferences.read("soundbox", "1")

            if (!isMyServiceRunning(VibrationService::class.java)) {
                check()
            }
        }
    }

    private fun isMyServiceRunning(cls: Class<*>): Boolean {
        val manager = mContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return manager.getRunningServices(Int.MAX_VALUE).any {
            it.service.className == cls.name
        }
    }

    private fun check() {
        if (flashbox == "1") setFlash()
        if (vibratebox == "1") App.instance?.let { setVibrator(true, it) }
        if (soundbox == "1") playSound()
    }

    private fun setFlash() {
        camManager = App.instance?.getSystemService(Context.CAMERA_SERVICE) as CameraManager?
        try {
            handler.removeCallbacks(flashRunnable!!)
            val localCameraId = camManager?.cameraIdList?.getOrNull(0)
            localCameraId?.let { id ->
                cameraId = id
                camManager?.setTorchMode(id, true)
            }
        } catch (_: Exception) {}
    }

    private fun closeFlash() {
        flashOn = false
        try {
            cameraId?.let { camManager?.setTorchMode(it, false) }
        } catch (_: Exception) {}

        handler.removeCallbacks(flashRunnable!!)
    }

    private fun playSound() {
        handler.post(object : Runnable {
            override fun run() {
                val duration = 5000
                MediaManager.getInstance().playSound(
                    App.instance,
                    "sound/1-cat-meowing.mp3",
                    duration
                ) {
                    closeFlash()
                    App.instance?.let { setVibrator(false, it) }
                    clapping = 0
                    isClapped = false
                }
            }
        })
    }

    fun listen() {
        recorder.startRecording()
        val tarsosFormat = AudioFormat(SAMPLE_RATE.toFloat(), 16, 1, true, false)
        Thread {
            while (mIsRecording && isRunning) {
                val bytesRead = recorder.read(buffer, 0, buffer.size)
                val audioEvent = AudioEvent(tarsosFormat, bytesRead.toLong())
                audioEvent.setFloatBufferWithByteBuffer(buffer)
                mPercussionOnsetDetector.process(audioEvent)
            }
            recorder.stop()
        }.start()
    }

    private val audioFocusChangeListener =
        AudioManager.OnAudioFocusChangeListener { /* handle if needed */ }

    private fun hasAudioFocus(): Boolean {
        val audioManager = mContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val result = audioManager.requestAudioFocus(
            audioFocusChangeListener,
            AudioManager.STREAM_MUSIC,
            AudioManager.AUDIOFOCUS_GAIN
        )
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    fun requestAudioFocus() {
        hasAudioFocus()
    }

    private val validSampleRates: Int
        get() {
            for (rate in intArrayOf(44100, 22050, 16000, 11025, 8000)) {
                if (AudioRecord.getMinBufferSize(rate, 1, 2) > 0 && !rate_send) {
                    rateSupported = rate
                    rate_send = true
                }
            }
            return rateSupported
        }

    init {
        defaultPreferences = DefaultPreferences(mContext)
        SAMPLE_RATE = validSampleRates
        val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, 1, 2)
        buffer = ByteArray(minBufferSize)
        if (ActivityCompat.checkSelfPermission(
                mContext,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        ) {
        }
        recorder = AudioRecord(1, SAMPLE_RATE, 1, 2, minBufferSize)
        mPercussionOnsetDetector = PercussionOnsetDetector(
            SAMPLE_RATE.toFloat(),
            minBufferSize / 2,
            this,
            24.0,
            5.0
        )
        mIsRecording = true
        requestAudioFocus()
    }

    companion object {
        var SAMPLE_RATE: Int = 8000
        var mIsRecording: Boolean = false
        var isRunning: Boolean = false
        val handler: Handler = Handler(Looper.getMainLooper())

        var clapping: Int = 0
        var isClapped: Boolean = false
    }
}
