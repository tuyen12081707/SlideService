package com.panda.slideservice

import android.media.AudioRecord
import com.musicg.api.WhistleApi
import com.musicg.wave.WaveHeader
import java.util.LinkedList
import kotlin.concurrent.Volatile


class DetectorThread(recorderThread: RecorderThread) : Thread() {
    @Volatile
    private var _thread: Thread? = null
    private var numWhistles = 0
    private var onSignalsDetectedListener: OnSignalsDetectedListener? = null
    private val recorder: RecorderThread = recorderThread
    private val waveHeader: WaveHeader
    private val whistleApi: WhistleApi
    private val whistleCheckLength = 3
    private val whistlePassScore = 3
    private val whistleResultList: LinkedList<Any?> = LinkedList<Any?>()

    init {
        val audioRecord: AudioRecord = recorderThread.getAudioRecord()
        var i = 0
        val i2 =
            if (audioRecord.audioFormat == 2) 16 else if (audioRecord.audioFormat == 3) 8 else 0
        if (audioRecord.channelConfiguration == 16) {
            i = 1
        }
        this.waveHeader = WaveHeader()
        waveHeader.setChannels(i)
        waveHeader.setBitsPerSample(i2)
        waveHeader.setSampleRate(audioRecord.sampleRate)
        this.whistleApi = WhistleApi(this.waveHeader)
    }

    private fun initBuffer() {
        this.numWhistles = 0
        whistleResultList.clear()
        for (i in 0..<this.whistleCheckLength) {
            whistleResultList.add(false)
        }
    }

    override fun start() {
        this._thread = Thread(this)
        _thread!!.start()
    }

    fun stopDetection() {
        this._thread = null
    }

    override fun run() {
        try {
            initBuffer()
            val currentThread = currentThread()
            while (this._thread === currentThread) {
                val frameBytes: ByteArray? = recorder.frameBytes
                if (frameBytes != null) {
                    val isWhistle: Boolean = whistleApi.isWhistle(frameBytes)
                    if ((whistleResultList.first as Boolean)) {
                        numWhistles--
                    }
                    whistleResultList.removeFirst()
                    whistleResultList.add(isWhistle)
                    if (isWhistle) {
                        numWhistles++
                    }
                    if (this.numWhistles >= this.whistlePassScore) {
                        initBuffer()
                        onWhistleDetected()
                    }
                } else {
                    if ((whistleResultList.first as Boolean)) {
                        numWhistles--
                    }
                    whistleResultList.removeFirst()
                    whistleResultList.add(false)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun onWhistleDetected() {
        val onSignalsDetectedListener = this.onSignalsDetectedListener
        onSignalsDetectedListener?.onWhistleDetected()
    }

    fun setOnSignalsDetectedListener(onSignalsDetectedListener: OnSignalsDetectedListener?) {
        this.onSignalsDetectedListener = onSignalsDetectedListener
    }
}