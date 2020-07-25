package com.example.audiotest

import kotlin.math.*

class FFT {

    private var n = 0
    private var bitRev = IntArray(0)
    private var sinTbl = DoubleArray(0)
    private var cosTbl = DoubleArray(0)

    constructor(_n: Int) {
        n = 1
	while (n < _n) {
	    n = n.shl(1)
	}
        var i = 0
        var j = 0
        var n2 = n.shr(1)
        bitRev = IntArray(n)
        while (true) {
            bitRev[i] = j
            if (++i >= n)
                break
            var k = n2
            while (k <= j) {
                j -= k
                k = k.shr(1)
            }
            j += k
        }
        n2 = n.shr(1)
        val n4 = n.shr(2)
        val n8 = n.shr(3)
        var t = sin(PI / n.toDouble())
        var dc = 2 * t * t
        var ds = sqrt(dc * (2 - dc))
        t = 2 * dc
        sinTbl = DoubleArray(n+n4)
        var c = 1.0
        sinTbl[n4] = 1.0
        var s = 0.0
        sinTbl[0] = 0.0
        for (i in 1 until n8) {
            c -= dc
            dc += t * c
            s += ds
            ds -= t * s
            sinTbl[i] = s
            sinTbl[n4 - i] = c
        }
        if (n8 != 0) {
            sinTbl[n8] = sqrt(0.5)
        }
        for (i in 0 until n4) {
            sinTbl[n2 - i] = sinTbl[i]
        }
        for (i in 0 until (n2+n4)) {
            sinTbl[i + n2] = - sinTbl[i]
        }
        cosTbl = DoubleArray(n)
        for (i in 0 until n) {
            val t = i.toDouble() / n.toDouble()
            cosTbl[i] = cos(2 * PI * t)
        }
    }

    fun getSize(): Int {
        return n
    }

    fun s2d(src: ShortArray): DoubleArray {
        var dst = DoubleArray(n)
        val nn = min(n, src.count())
        for (i in 0 until nn) {
            dst[i] = src[i].toDouble() / 32767.0
        }
        for (i in nn until n) {
            dst[i] = 0.0
        }
        return dst
    }

    fun d2s(src: DoubleArray): ShortArray {
        var dst = ShortArray(n)
        for (i in 0 until n) {
            dst[i] = (src[i] * 32767.0).toShort()
        }
        return dst
    }

    fun fft(x: DoubleArray, y: DoubleArray) {
        val n4 = n.shr(2)
        for (i in 0 until n) {
            val j = bitRev[i]
            if (i < j) {
                var t = x[i]
                x[i] = x[j]
                x[j] = t
                t = y[i]
                y[i] = y[j]
                y[j] = t
            }
        }
        var k2 = 0
        var d = n
        var k = 1
        while (k < n) {
            var h = 0
            k2 = k.shl(1)
            d = n / k2
            for (j in 0 until k) {
                val c = sinTbl[h + n4]
                val s = sinTbl[h]
                for (i in j until n step k2) {
                    val ik = i + k
                    val dx = s * y[ik] + c * x[ik]
                    val dy = c * y[ik] - s * x[ik]
                    x[ik] = x[i] - dx
                    x[i] += dx
                    y[ik] = y[i] - dy
                    y[i] += dy
                }
                h += d
            }
            k = k2
        }
        for (i in 0 until n) {
            x[i] /= n.toDouble()
            y[i] /= n.toDouble()
        }
    }

    fun ifft(x: DoubleArray, y: DoubleArray) {
        val n4 = n.shr(2)
        for (i in 0 until n) {
            val j = bitRev[i]
            if (i < j) {
                var t = x[i]
                x[i] = x[j]
                x[j] = t
                t = y[i]
                y[i] = y[j]
                y[j] = t
            }
        }
        var k2 = 0
        var d = n
        var k = 1
        while (k < n) {
            var h = 0
            k2 = k.shl(1)
            d = n / k2
            for (j in 0 until k) {
                val c = sinTbl[h + n4]
                val s = -sinTbl[h]
                for (i in j until n step k2) {
                    val ik = i + k
                    val dx = s * y[ik] + c * x[ik]
                    val dy = c * y[ik] - s * x[ik]
                    x[ik] = x[i] - dx
                    x[i] += dx
                    y[ik] = y[i] - dy
                    y[i] += dy
                }
                h += d
            }
            k = k2
        }
    }

    fun abs(x: DoubleArray, y: DoubleArray, bHalf: Boolean): DoubleArray {
        val _n = if (bHalf) n.shr(1) else n
        val a = DoubleArray(_n)
        for (i in 0 until _n) {
            a[i] = sqrt(x[i]*x[i] + y[i]*y[i])
        }
        return a
    }

    fun angle(x: DoubleArray, y: DoubleArray, bHalf: Boolean): DoubleArray {
        val _n = if (bHalf) n.shr(1) else n
        val a = DoubleArray(_n)
        for (i in 0 until _n) {
            a[i] = atan2(y[i], x[i])
        }
        return a
    }

    fun hamming(x: DoubleArray) {
        for (i in 0 until n) {
            x[i] *= 0.54 - 0.46 * cosTbl[i]
        }
    }

    fun hanning(x: DoubleArray) {
        for (i in 0 until n) {
            x[i] *= 0.5 - 0.5 * cosTbl[i]
        }
    }

    fun fft(src: ShortArray): DoubleArray {
        val x = s2d(src)
        val y = DoubleArray(n)
        hanning(x)
        fft(x, y)
        val a = abs(x, y, true)
        return a
    }
}
