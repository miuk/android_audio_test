package com.example.audiotest

import android.media.AudioFormat
import android.media.AudioRecord
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.widget.AdapterView
import kotlinx.android.synthetic.main.activity_audio_in.*
import kotlin.math.max

class AudioInActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener {

    val audioIn = AudioIn(8000, 100)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_audio_in)
        val timeDomain = TimeDomainVisualizer(this, time_domain)
        audioIn.addUpdater(timeDomain)
        val frequencyDomain = FrequencyDomainVisualizer(this, frequency_domain, audioIn.samplesPerFrame, audioIn.sampleRate)
        audioIn.addUpdater(frequencyDomain)
        audio_in.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                audioIn.start()
                audio_in_message.text = "input ON"
            } else {
                audioIn.stop()
                audio_in_message.text = "input OFF " + audioIn.count.toString()
            }
        }
        rate.onItemSelectedListener = this
    }

    override fun onPause() {
        super.onPause()
        audioIn.stop()
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
        audio_in_message.text = pos.toString()
        val newRate = when(pos) {
            0 -> 8000
            1 -> 16000
            2 -> 32000
            3 -> 44100
            4 -> 48000
            else -> 8000
        }
        audioIn.changeRate(newRate)
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        //
    }
}