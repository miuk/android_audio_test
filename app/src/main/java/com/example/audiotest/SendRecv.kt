package com.example.audiotest

import android.content.Context
import android.content.Context.WIFI_SERVICE
import android.net.wifi.WifiManager
import android.util.Log
import androidx.core.content.ContextCompat.getSystemService
import java.net.NetworkInterface
import java.net.SocketException


class SendRecv : AudioIn.Updater, AudioPlay.DataProvider {

    var udpsock = -1
    val q = Buffer()

    fun getMyIPv4Address(context: Context):String {
        var wifi_ip = ""
        var mobile_ip = ""
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            if (interfaces == null) {
                return "IPアドレス取得できませんでした。" + System.lineSeparator()
            }
            while(interfaces.hasMoreElements()){
                val network = interfaces.nextElement()
                if (network.isLoopback or network.isPointToPoint) {
                    continue
                }
                val addresses = network.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    val ip = address.hostAddress
                    if (ip.contains(":")) {
                        continue
                    }
                    mobile_ip = ip
                }
            }
        } catch (e : SocketException){
            e.printStackTrace()
        }
        val manager =
            context.getSystemService(WIFI_SERVICE) as WifiManager?
        val info = manager!!.connectionInfo
        val ipAdr = info.ipAddress
        wifi_ip = String.format(
            "%02d.%02d.%02d.%02d",
            ipAdr shr 0 and 0xff,
            ipAdr shr 8 and 0xff,
            ipAdr shr 16 and 0xff,
            ipAdr shr 24 and 0xff
        )
        if (wifi_ip.isEmpty()) {
            return mobile_ip
        } else {
            return wifi_ip
        }
    }

    override fun changeRate(newRate: Int, newSamplesPerFrame: Int) {}

    override fun update(buf: ShortArray) {
        //Log.i("SendRecv", "update " + buf.size.toString())
        sendUDPDatagram(udpsock, buf, buf.size)
    }

    fun received(buf: ShortArray) {
        //Log.i("SendRecv", "received " + buf.size.toString())
        q.put(buf)
    }

    external fun openUDPSocket(remoteHost: String, remotePort: Int, localPort: Int): Int
    external fun sendUDPDatagram(sock: Int, data: ShortArray, len: Int): Int
    external fun closeUDPSocket(sock: Int)

    fun start(peerHost: String, strPeerPort: String, strMyPort: String): String {
        val peerPort = strPeerPort.toInt()
        val myPort = strMyPort.toInt()
        udpsock = openUDPSocket(peerHost, peerPort, myPort)
        if (udpsock < 0) {
            return "socket open failed"
        }
        return ""
    }

    fun stop() {
        closeUDPSocket(udpsock)
        udpsock = -1
    }

    override fun getAudioPlayData(len: Int): ShortArray {
        //Log.i("SendRecv", "getAudioPlayData")
        val data = q.get(len)
        return data
    }
}