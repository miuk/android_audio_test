package com.example.audiotest

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.example.audiotest.AudioIn

class TimeDomainVisualizer : SurfaceView, SurfaceHolder.Callback, AudioIn.Updater {

    private var _holder: SurfaceHolder
    private val paint = Paint()

    constructor(ctx: Context, surface: SurfaceView) : super(ctx) {
        _holder = surface.holder
        _holder.addCallback(this)
        paint.strokeWidth = 2f
        paint.isAntiAlias = true
        paint.color = Color.WHITE

    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        if (holder != null) {
            val canvas = holder.lockCanvas()
            holder.unlockCanvasAndPost(canvas)
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {

    }
    override fun surfaceDestroyed(holder: SurfaceHolder) {

    }

    override fun changeRate(newRate: Int, newSamplesPerFrame: Int) {}

    override fun update(buf: ShortArray) {
        val canvas = _holder.lockCanvas()
        try {
            canvas.drawColor(Color.BLACK)
            val baseLine = canvas.height / 2f
            val divX = buf.count().toFloat()
            var oldX : Float = 0f
            var oldY = baseLine
            for ((idx, value) in buf.withIndex()) {
                val x = canvas.width.toFloat() * idx.toFloat() / divX
                val y = baseLine + baseLine * value.toFloat() / 32767f
                //Log.i("TimeDomainVisualizer", "update " + x.toString() + ", " + value.toString())
                canvas.drawLine(oldX, oldY, x, y, paint)
                oldX = x
                oldY = y
            }
        } finally {
            _holder.unlockCanvasAndPost(canvas)
        }
    }
}