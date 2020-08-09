package com.example.audiotest

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), AdapterView.OnItemClickListener {

    val menuList = arrayOf(
        "Single Tone",
        "Audio In",
        "Send Receive"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        menu.adapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, menuList)
        menu.onItemClickListener = this

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.RECORD_AUDIO)) {
                Log.i("main", "requesting permission")
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.RECORD_AUDIO), 1)
            } else {
                Log.i("main", "permission already rejected")
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.RECORD_AUDIO), 1)
            }
        } else {
            Log.i("main", "permission granted")
        }
    }

    override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        Log.i("MainActivity", "onItemClick " + position.toString())
        val clz = when(position) {
            0 -> SingleToneActivity::class.java
            1 -> AudioInActivity::class.java
            2 -> SendRecvActivity::class.java
            else -> SingleToneActivity::class.java
        }
        startActivity(Intent(this, clz))
    }

    companion object {
        init {
            System.loadLibrary("udpsocket")
        }
    }
}