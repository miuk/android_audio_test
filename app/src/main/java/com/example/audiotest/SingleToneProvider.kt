package com.example.audiotest

import kotlin.math.IEEErem
import kotlin.math.PI
import kotlin.math.sin

class SingleToneProvider(_rate: Int) : AudioPlay.DataProvider {

    var frequency = 440
    var rate = _rate
    var lastPhase = 0.0

    override fun getAudioPlayData(len: Int): ShortArray {
        val data = ShortArray(len)
        val omega = 2 * PI * frequency / rate
        var p: Double = 0.0
        for (i in 0 until data.count()) {
            p = omega * i + lastPhase
            val y = sin(p) * 32767.0
            data[i] = y.toShort()
        }
        lastPhase = (p + omega).IEEErem(2 * PI)
        return data
    }
}