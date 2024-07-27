package com.example.deldia

import android.Manifest
import android.app.Activity
import android.app.ProgressDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.AsyncTask
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.IOException
import java.util.*

class ControlActivity : AppCompatActivity() {


    private lateinit var control_led_on: Button
    private lateinit var control_led_disconnect: Button
    companion object {
    lateinit var m_address: String
        var m_bluetoothSocket: BluetoothSocket? = null
        var m_isConnected: Boolean = false
        var m_myUUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        lateinit var m_progress: ProgressDialog
        lateinit var m_bluetoothAdapter: BluetoothAdapter
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_control)
        m_address = intent.getStringExtra("Device_address").toString()
//        Toast.makeText(applicationContext, "m_address: $m_address", Toast.LENGTH_LONG).show()

        control_led_on = findViewById(R.id.control_led_on)
        control_led_disconnect = findViewById(R.id.control_led_disconnect)

        control_led_on.setOnClickListener { sendCommand("4SOLUCIONES \n") }
        control_led_disconnect.setOnClickListener { disconnect() }

        ConnectToDevice(this).execute()

    }

    private fun sendCommand(input: String) {
        if (m_bluetoothSocket != null) {
            try{
                m_bluetoothSocket!!.outputStream.write(input.toByteArray())
            } catch(e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun disconnect() {
        if (m_bluetoothSocket != null) {
            try {
                m_bluetoothSocket!!.close()
                m_bluetoothSocket = null
                m_isConnected = false
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        finish()
    }

    private class ConnectToDevice(c: Context) : AsyncTask<Void, Void, String>() {
        private var connectSuccess: Boolean = true
        private val context: Context

        init {
            this.context = c
        }
        private fun checkPermission(): Boolean{
            val permission2 = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
            return permission2 == PackageManager.PERMISSION_GRANTED
        }

        override fun onPreExecute() {
            super.onPreExecute()
            m_progress = ProgressDialog.show(context, "Connecting...", "please wait")
        }

        override fun doInBackground(vararg p0: Void?): String? {
            try {
                if (m_bluetoothSocket == null || !m_isConnected) {
                    m_bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
                    val device: BluetoothDevice = m_bluetoothAdapter.getRemoteDevice(m_address)
                    if (checkPermission()){
                        m_bluetoothSocket = device.createInsecureRfcommSocketToServiceRecord(m_myUUID)
                        BluetoothAdapter.getDefaultAdapter().cancelDiscovery()
                        m_bluetoothSocket!!.connect()
                    }

                }
            } catch (e: IOException) {
                connectSuccess = false
                e.printStackTrace()
            }
            return null
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            if (!connectSuccess) {
                Toast.makeText(context, "couldn't connect", Toast.LENGTH_LONG).show()
            } else {
                m_isConnected = true
            }
            m_progress.dismiss()
        }
    }
}