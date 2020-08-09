package com.example.audiotest

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import androidx.preference.PreferenceManager
import kotlinx.android.synthetic.main.activity_send_recv.*

class SendRecvActivity : AppCompatActivity() {

    val sendRecv = SendRecv()
    val audioIn = AudioIn(8000, 10)
    val audioPlay = AudioPlay(8000, 10)
    val props = mutableMapOf<String,String>()
    val keys = arrayOf("peerhost", "peerport", "myport")

    fun readProperties() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        for (key in keys) {
            val value = prefs.getString(key, "") ?: ""
            props[key] = value
            setValue(key, value)
        }
    }

    private fun getField(key: String): View {
        val id = resources.getIdentifier(key, "id", packageName)
        val fld = findViewById<View>(id)
        return fld
    }

    fun getValue(key: String): String {
        val fld = getField(key)
        var newVal = ""
        if (fld is EditText) {
            newVal = fld.text.toString()
        }
        val curVal = props[key]
        if (!newVal.equals(curVal)) {
            props[key] = newVal
            PreferenceManager.getDefaultSharedPreferences(this)
                .edit()
                .putString(key, newVal)
                .apply()
        }
        return newVal
    }

    fun setValue(key: String, value: String) {
        val fld = getField(key)
        if (fld is EditText) {
            fld.setText(value)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_send_recv)
        readProperties()
        myhost.text = sendRecv.getMyIPv4Address(this)
        audioIn.changeRate(8000)
        audioIn.addUpdater(sendRecv)
        audioPlay.dataProvider = sendRecv
        sendrecv_on.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                Log.i("xx", "start")
                startSendRecv()
            } else {
                Log.i("xx", "stop")
                stopSendRecv()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        stopSendRecv()
    }

    fun startSendRecv() {
        sendRecv.start(getValue("peerhost"), getValue("peerport"), getValue("myport"))
        audioIn.start()
        audioPlay.start()
    }

    fun stopSendRecv() {
        audioIn.stop()
        sendRecv.stop()
        audioPlay.stop()
    }
}