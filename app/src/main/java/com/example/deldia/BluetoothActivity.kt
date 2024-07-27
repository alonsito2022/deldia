package com.example.deldia

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class BluetoothActivity : AppCompatActivity() {
    private lateinit var tvBluetoothStatus: TextView
    private lateinit var tvPaired: TextView
    private lateinit var ivBluetooth: ImageView
    private lateinit var btnTurnOn: Button
    private lateinit var btnTurnOff: Button
    private lateinit var btnDiscoverable: Button
    private lateinit var btnGetPairedDevices: Button
    private lateinit var listViewPairedDevices: ListView

    private var bluetoothAdapter: BluetoothAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bluetooth)

        tvBluetoothStatus = findViewById(R.id.tvBluetoothStatus)
        tvPaired = findViewById(R.id.tvPaired)
        ivBluetooth = findViewById(R.id.ivBluetooth)
        btnTurnOn = findViewById(R.id.btnTurnOn)
        btnTurnOff = findViewById(R.id.btnTurnOff)
        btnDiscoverable = findViewById(R.id.btnDiscoverable)
        btnGetPairedDevices = findViewById(R.id.btnGetPairedDevices)
        listViewPairedDevices = findViewById(R.id.listViewPairedDevices)

        val bluetoothManager = applicationContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        if(bluetoothAdapter == null) {
            tvBluetoothStatus.text = "Bluetooth is not available"
        }else{
            tvBluetoothStatus.text = "Bluetooth is available"
        }

        if(bluetoothAdapter!!.isEnabled){
            ivBluetooth.setImageResource(R.drawable.ic_baseline_bluetooth_24)
        }else{
            ivBluetooth.setImageResource(R.drawable.ic_baseline_bluetooth_disabled_24)
        }

        if (checkPermission()){
            Toast.makeText(this, "Permiso aceptado", Toast.LENGTH_LONG).show()
        }else{
            requestPermissions()
        }

        btnTurnOn.setOnClickListener {
            if(bluetoothAdapter!!.isEnabled){
                Toast.makeText(applicationContext, "Already on", Toast.LENGTH_LONG).show()
            }else{
                val intentBluetooth = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                resultLauncher.launch(intentBluetooth)
            }
        }
        btnTurnOff.setOnClickListener {
            if(!bluetoothAdapter!!.isEnabled){
                Toast.makeText(applicationContext, "Already off", Toast.LENGTH_LONG).show()
            }else{
                bluetoothAdapter!!.disable()
                ivBluetooth.setImageResource(R.drawable.ic_baseline_bluetooth_disabled_24)
                Toast.makeText(applicationContext, "Bluetooth turn off", Toast.LENGTH_LONG).show()
            }
        }
        btnDiscoverable.setOnClickListener {
            if(!bluetoothAdapter!!.isDiscovering){
                Toast.makeText(applicationContext, "Making your device discoverable", Toast.LENGTH_LONG).show()
                val intentBluetoothDiscoverable = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
                resultBluetoothDiscoverableLauncher.launch(intentBluetoothDiscoverable)
            }
        }
        btnGetPairedDevices.setOnClickListener {
            val list : ArrayList<BluetoothDevice> = ArrayList()
            if(bluetoothAdapter!!.isEnabled){
                tvPaired.text = "Paired Devices"
                val pairedDevices = bluetoothAdapter!!.bondedDevices
                for (device in pairedDevices) {
                    val deviceName = device.name
                    val macAddress = device.address
                    val deviceType = device.type
                    tvPaired.append("\nDevice: $deviceName, macAddress: $macAddress, deviceType: $deviceType")
                    list.add(device)

                }

            }

            val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, list)
            listViewPairedDevices.adapter = adapter
            listViewPairedDevices.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
                val device: BluetoothDevice = list[position]
                val address: String = device.address

                val intent = Intent(this, ControlActivity::class.java)
                intent.putExtra("Device_address", address)
                startActivity(intent)
            }
        }
    }


    private var resultBluetoothDiscoverableLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            ivBluetooth.setImageResource(R.drawable.ic_baseline_bluetooth_24)
            Toast.makeText(applicationContext, "discoverable is on", Toast.LENGTH_LONG).show()
        }else{
            Toast.makeText(applicationContext, "Could not discoverable", Toast.LENGTH_LONG).show()
        }
    }
    private var resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // There are no request codes
            val data: Intent? = result.data
            ivBluetooth.setImageResource(R.drawable.ic_baseline_bluetooth_24)
            Toast.makeText(applicationContext, "Bluetooth is on", Toast.LENGTH_LONG).show()
        }else{
            Toast.makeText(applicationContext, "Could not on Bluetooth", Toast.LENGTH_LONG).show()
        }
    }

    private fun checkPermission(): Boolean{
        val permission2 = ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
        return permission2 == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_CONNECT ),200)
        else{
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            requestBluetooth.launch(enableBtIntent)
        }
    }

    private var requestBluetooth = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            //granted
            Toast.makeText(applicationContext, "Permission Bluetooth Granted", Toast.LENGTH_LONG).show()
        }else{
            //deny
            Toast.makeText(applicationContext, "Permission Bluetooth Denied", Toast.LENGTH_LONG).show()
        }
    }
}