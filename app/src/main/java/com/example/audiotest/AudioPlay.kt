package com.example.audiotest

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlin.math.*;

class AudioPlay {

    var frequency = 440.0

    val rate = 44100
    val bufsiz = rate / 10
    val data = ShortArray(bufsiz)
    var lastPhase = 0.0

    val audioTrack = AudioTrack.Builder()
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build())
        .setAudioFormat(
            AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(rate)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build())
        .setBufferSizeInBytes(bufsiz*2)
        .build()

    init {
        Log.i("AudioPlay", "init")
        audioTrack.setPlaybackPositionUpdateListener(
            object: AudioTrack.OnPlaybackPositionUpdateListener {
                override fun onPeriodicNotification(p0: AudioTrack?) {
                    Log.i("AudioPlay", "onPeriodicNotification")
                }

                override fun onMarkerReached(p0: AudioTrack?) {
                    fill()
                    val ret = audioTrack.write(data, 0, data.count(), AudioTrack.WRITE_BLOCKING)
                    audioTrack.notificationMarkerPosition = data.count()
                }
            }
        )
    }

    fun start() {
        Log.i("AudioPlay", "start enter")
        for (i in 0..1) {
            fill()
            audioTrack.write(data, 0, data.count(), AudioTrack.WRITE_BLOCKING)
        }
        audioTrack.notificationMarkerPosition = data.count()
        audioTrack.play()

        Log.i("AudioPlay", "start leave")

    }

    fun stop() {
        audioTrack.pause()
        audioTrack.stop()
        audioTrack.flush()
    }

    fun fill() {
        val omega = 2 * PI * frequency / rate
        var p: Double = 0.0
        for (i in 0 until data.count()) {
            p = omega * i + lastPhase
            val y = sin(p) * 32767.0
            data[i] = y.toShort()
        }
        lastPhase = (p + omega).IEEErem(2 * PI)
    }

    fun changeFrequency(value: Double) {
        frequency = value
        if (audioTrack.playState == AudioTrack.PLAYSTATE_PLAYING) {
            stop()
            start()
        }
    }

    fun changeVolume(value: Int) {
        Log.i("AudioPlay", "changeVolume " + value.toString())
        audioTrack.setVolume(value.toFloat() / 100f)
    }
}