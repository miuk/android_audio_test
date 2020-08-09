package com.example.audiotest

import android.util.Log

class Buffer {
    val q = mutableListOf<ShortArray>()
    var pos = 0
    var rest = 0
    var lastData: ShortArray? = null

    fun put(data: ShortArray) {
        //Log.i("Buffer", "put " + data.size.toString())
        synchronized (this) {
            q.add(data)
            rest += data.size
        }
    }

    fun get(len: Int): ShortArray {
        //Log.i("Buffer", "get enter " + len.toString())
        val data = ShortArray(len)
        var dpos = 0
        synchronized (this) {
            if (rest < len) {
                //Log.i("Buffer", "returns zero, rest=$rest")
                return lastData ?: data
            }
            while (!q.isEmpty()) {
                val src = q.get(0)
                val srest = src.size - pos
                val drest = len - dpos
                val n = if (drest > srest) srest else drest
                for (i in 0 until n) {
                    data[dpos] = src[pos]
                    dpos++
                    pos++
                }
                if (pos >= src.size) {
                    q.removeAt(0)
                    pos = 0
                }
                if (dpos >= len) {
                    break
                }
            }
            rest -= dpos
        }
        for (i in dpos until len) {
            data[i] = 0
        }
        //Log.i("Buffer", "dpos=" + dpos.toString() + ", rest=" + rest.toString() + ", total=" + (dpos + rest).toString())
        lastData = data
        return data
    }
}