package com.example.audiotest

import android.media.AudioTrack
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import android.widget.TextView
import android.widget.ToggleButton
import java.lang.Math.pow
import kotlinx.android.synthetic.main.activity_single_tone.*

class SingleToneActivity : AppCompatActivity() {

    val audioPlay = AudioPlay(44100, 100)
    val singleToneProvider = SingleToneProvider(44100)

    var curTone = "A"
    var toneFreqMap = mutableMapOf<String,Double>(
        "C" to 261.63, "D" to 293.66, "E" to 329.63, "F" to 349.23,
        "G" to 392.00, "A" to 440.00, "B" to 493.88, "C2" to 523.25,
        "C#" to 277.18, "D#" to 311.13, "F#" to 369.99, "G#" to 415.30, "A#" to 466.16)
    val idxToneMap = mapOf(
        -9 to "C", -8 to "C#", -7 to "D", -6 to "D#", -5 to "E", -4 to "F",
        -3 to "F#", -2 to "G", -1 to "G#", 0 to "A", 1 to "A#", 2 to "B", 3 to "C2"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_single_tone)

        audioPlay.dataProvider = singleToneProvider

        volume.min = 0
        volume.max = 100
        volume.progress = 50
        volume.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    setVolume(progress)
                }

                override fun onStartTrackingTouch(p0: SeekBar?) {}
                override fun onStopTrackingTouch(p0: SeekBar?) {}
            }
        )

        frequency.min = 100
        frequency.max = 10000
        frequency.progress = singleToneProvider.frequency
        frequency.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    setFrequency(progress)
                }

                override fun onStartTrackingTouch(p0: SeekBar?) {}
                override fun onStopTrackingTouch(p0: SeekBar?) {}
            }
        )

        octave.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    makeToneSet(progress)
                }

                override fun onStartTrackingTouch(p0: SeekBar?) {}
                override fun onStopTrackingTouch(p0: SeekBar?) {}
            }
        )

        sound_on.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                message.text = "ON"
                audioPlay.start()
            } else {
                message.text = "OFF"
                audioPlay.stop()
            }
        }

        setVolume(volume.progress)
        setFrequency(singleToneProvider.frequency)
        makeToneSet(0)
        val minv =AudioTrack.getMaxVolume()
        val maxv = AudioTrack.getMinVolume()
        message.text = minv.toString() + "/" + maxv.toString()
    }

    override fun onPause() {
        super.onPause()
        sound_on.isChecked = false
    }

    fun setVolume(value: Int) {
        value_volume.text = value.toString()
        audioPlay.changeVolume(value)
    }

    fun setFrequency(value: Int) {
        value_frequency.text = value.toString()
        singleToneProvider.frequency = value
    }

    fun makeToneSet(oct: Int) {
        val base = 440.0 * pow(2.0, oct.toDouble())
        for (n in -9..3) {
            val freq = base * pow(pow(2.0, n.toDouble()), 1.0 / 12.0)
            val tone = idxToneMap[n] ?: "A"
            toneFreqMap[tone] = freq
        }
        setTone(curTone)
    }

    fun setTone(tone: String) {
        curTone = tone
        frequency.progress = (toneFreqMap[tone] ?: 440.0).toInt()
    }

    fun onTone(view: View) {
        val tone= view as TextView
        message.text = tone.text
        setTone(tone.text.toString())
    }
}