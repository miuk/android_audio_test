package com.example.audiotest

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlin.math.*;

class AudioPlay(_sampleRate: Int, _frameRate: Int) {

    interface DataProvider {
        fun getAudioPlayData(len: Int): ShortArray
    }

    val sampleRate = _sampleRate
    val frameRate = _frameRate
    private val samplesPerFrame = sampleRate * frameRate / 1000
    private val bytesPerFrame = samplesPerFrame * 2
    val bufsiz: Int
    lateinit var dataProvider: DataProvider
    var audioTrack: AudioTrack

    init {
        Log.i("AudioPlay", "init")
        val minbufsiz = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        Log.i("AudioPlay", "init sampleRate=" + sampleRate.toString())
        Log.i("AudioPlay", "init minbufsiz=" + minbufsiz.toString())
        bufsiz = max(bytesPerFrame, minbufsiz)
        AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build())
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build())
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setBufferSizeInBytes(bufsiz*2)
            .build()

        audioTrack.setPlaybackPositionUpdateListener(
            object: AudioTrack.OnPlaybackPositionUpdateListener {
                override fun onPeriodicNotification(p0: AudioTrack?) {
                    Log.i("AudioPlay", "onPeriodicNotification")
                }

                override fun onMarkerReached(p0: AudioTrack?) {
                    val data = dataProvider.getAudioPlayData(bufsiz)
                    val ret = audioTrack.write(data, 0, data.count(), AudioTrack.WRITE_BLOCKING)
                    audioTrack.notificationMarkerPosition = data.count()
                }
            }
        )
    }

    fun start() {
        Log.i("AudioPlay", "start enter")
        for (i in 0..1) {
            val data = dataProvider.getAudioPlayData(bufsiz)
            audioTrack.write(data, 0, data.count(), AudioTrack.WRITE_BLOCKING)
        }
        audioTrack.notificationMarkerPosition = bufsiz
        audioTrack.play()

        Log.i("AudioPlay", "start leave")

    }

    fun stop() {
        audioTrack.pause()
        audioTrack.stop()
        audioTrack.flush()
    }

    fun changeVolume(value: Int) {
        Log.i("AudioPlay", "changeVolume " + value.toString())
        audioTrack.setVolume(value.toFloat() / 100f)
    }
}