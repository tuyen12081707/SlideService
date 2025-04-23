package com.panda.slideservice

import android.Manifest
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.annotation.RequiresPermission
import kotlin.math.abs

class RecorderThread : Thread() {
    private val audioEncoding = AudioFormat.ENCODING_PCM_16BIT
    private val channelConfiguration = AudioFormat.CHANNEL_IN_MONO
    private val frameByteSize = 2048
    private val buffer: ByteArray = ByteArray(frameByteSize)

    private var isRecording = false
    private var sampleRate: Int = getValidSampleRate()
    private lateinit var audioRecord: AudioRecord
    fun getAudioRecord(): AudioRecord {
        return this.audioRecord
    }
    private fun getValidSampleRate(): Int {
        val rates = intArrayOf(44100, 22050, 16000, 11025, 8000)
        for (rate in rates) {
            val bufferSize = AudioRecord.getMinBufferSize(rate, channelConfiguration, audioEncoding)
            if (bufferSize > 0) {
                return rate
            }
        }
        throw IllegalStateException("No valid sample rate found")
    }

    fun isRecording(): Boolean = isAlive && isRecording

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun startRecording() {
        try {
            val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfiguration, audioEncoding)
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfiguration,
                audioEncoding,
                minBufferSize
            )

            audioRecord.startRecording()
            isRecording = true
        } catch (e: Exception) {
            e.printStackTrace()
            isRecording = false
        }
    }

    fun stopRecording() {
        try {
            if (::audioRecord.isInitialized) {
                audioRecord.stop()
                audioRecord.release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isRecording = false
        }
    }

    val frameBytes: ByteArray?
        get() {
            if (!::audioRecord.isInitialized) return null

            audioRecord.read(buffer, 0, frameByteSize)
            var total = 0
            var i = 0
            while (i < frameByteSize) {
                val value = ((buffer[i + 1].toInt() shl 8) or (buffer[i].toInt() and 0xFF)).toShort()
                total += abs(value.toInt())
                i += 2
            }
            val avgAmplitude = total / (frameByteSize / 2)
            return if (avgAmplitude < 30) null else buffer
        }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    override fun run() {
        startRecording()
    }
}
