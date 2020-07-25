package com.example.audiotest


import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.example.audiotest.AudioIn
import kotlin.math.*

class FrequencyDomainVisualizer : SurfaceView, SurfaceHolder.Callback, AudioIn.Updater {

    private var _holder: SurfaceHolder
    private val paint = Paint()
    private var fft: FFT
    private var sampleRate = 0

    constructor(ctx: Context, surface: SurfaceView, frameLen: Int, _sampleRate: Int) : super(ctx) {
        _holder = surface.holder
        _holder.addCallback(this)
        paint.strokeWidth = 2f
        paint.isAntiAlias = true
        paint.color = Color.WHITE
        paint.textSize = 40f
        fft = FFT(frameLen)
        sampleRate = _sampleRate
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

    override fun changeRate(newRate: Int, newSamplesPerFrame: Int) {
        if (sampleRate == newRate)
            return
        sampleRate = newRate
        fft = FFT(newSamplesPerFrame)
    }

    override fun update(buf: ShortArray) {
        val spectrum = fft.fft(buf)
        val canvas = _holder.lockCanvas()
        try {
            canvas.drawColor(Color.BLACK)
            val height = canvas.height.toFloat()
            val divX = spectrum.count().toFloat()
            var oldX: Float = 0f
            var oldY = height
            var peakX = 0f
            var peakY = height
            var peakIdx = 0
            for ((idx, value) in spectrum.withIndex()) {
                val x = canvas.width.toFloat() * idx.toFloat() / divX
                //val y = height - height * value.toFloat() - 10f
                val db = if (value > 0.0) log10(value) * 20.0 else 0.0
                val y = -db.toFloat()
                //Log.i("TimeDomainVisualizer", "update " + x.toString() + ", " + value.toString())
                canvas.drawLine(oldX, oldY, x, y, paint)
                oldX = x
                oldY = y
                if (y < peakY) {
                    peakY = y
                    peakX = x
                    peakIdx = idx
                }
            }
            val peakFreq = peakIdx.toFloat() * sampleRate.toFloat() / (divX * 2f)
            val s = "%.0fHz/%.0fdB"
            canvas.drawText(s.format(peakFreq, -peakY), peakX, peakY, paint)
        } finally {
            _holder.unlockCanvasAndPost(canvas)
        }
    }
}
