package com.example.audiotest

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.math.max

class AudioIn(_sampleRate: Int, _frameRate: Int) {

    interface Updater {
        fun update(buf: ShortArray)
        fun changeRate(newRate: Int, newSamplesPerFrame: Int)
    }

    var frameRate = _frameRate
    var sampleRate = _sampleRate

    var samplesPerFrame = 0
    private var bytesPerFrame = 0
    private var bufsiz = 0
    private var audioRec: AudioRecord? = null

    var count = 0
    private var worker: Thread? = null
    val q = LinkedBlockingDeque<ShortArray>()
    val updaters = ArrayList<Updater>()

    init {
        calcSizes()
    }

    private fun calcSizes() {
        samplesPerFrame = sampleRate * frameRate / 1000
        bytesPerFrame = samplesPerFrame * 2
        bufsiz = max(bytesPerFrame, AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT))
    }

    fun changeRate(newRate: Int) {
        if (audioRec != null && sampleRate == newRate)
            return
        val bRecording = isRecording()
        stop()
        sampleRate = newRate
        calcSizes()
        audioRec = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufsiz
        )
        audioRec?.positionNotificationPeriod = samplesPerFrame
        //audioRec.notificationMarkerPosition = 40000
        audioRec?.setRecordPositionUpdateListener(object: AudioRecord.OnRecordPositionUpdateListener {
            override fun onPeriodicNotification(recorder: AudioRecord?) {
                record()
            }

            override fun onMarkerReached(recorder: AudioRecord?) {
                record()
            }
        })

        for (updater in updaters) {
            updater.changeRate(sampleRate, samplesPerFrame)
        }

        if (bRecording)
            start()
    }

    fun addUpdater(updator: Updater) {
        updaters.add(updator)
    }

    private fun record() {
        //Log.i("AudioIn", "record")
        val buf = ShortArray(samplesPerFrame)
        audioRec?.read(buf, 0, buf.count())
        q.put(buf)
    }

    fun process(buf: ShortArray) {
        count++
        for (updater in updaters) {
            updater.update(buf)
        }
    }

    fun isRecording(): Boolean {
        return (audioRec?.recordingState == AudioRecord.RECORDSTATE_RECORDING)
    }

    fun start() {
        audioRec?.startRecording()
        worker = thread() {
            Log.i("AudioIn", "worker begin")
            while (isRecording()) {
                val buf = q.poll(100, TimeUnit.MILLISECONDS)
                if (buf != null)
                    process(buf)
            }
            Log.i("AudioIn", "worker end")
        }
    }

    fun stop() {
        Log.i("AudioIn", "stop begin")
        audioRec?.stop()
        worker?.join()
        Log.i("AudioIn", "stop end")
    }
}