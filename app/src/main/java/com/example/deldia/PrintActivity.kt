package com.example.deldia
import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.deldia.localdatabase.Preference
import com.example.deldia.models.Operation
import com.example.deldia.pdf.Ticket
import com.example.deldia.retrofit.UserApiService
import com.example.deldia.web.PrinterCommands
import com.google.android.material.floatingactionbutton.FloatingActionButton
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.text.DecimalFormat
import java.util.*


class PrintActivity : AppCompatActivity() {
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    private lateinit var toolbar : Toolbar
    private lateinit var fabPrintSettings: FloatingActionButton
    private lateinit var btnPrint: Button
    private lateinit var btnDownload: Button
    private lateinit var btnStartDiscovery: Button
    private lateinit var btnCancelDiscovery: Button
    private lateinit var btnGoRoute: Button
    private lateinit var btnGoMap: Button
    private lateinit var btnGoOrders: Button
    private lateinit var btnClose: Button
    private lateinit var listViewPairedDevices: ListView
    private lateinit var textViewOperationPart1: TextView
    private lateinit var textViewOperationPart2: TextView
    private lateinit var textViewOperationPart3: TextView
    private lateinit var textViewOperationPart4: TextView
    private lateinit var textViewOperationPart5: TextView
    private lateinit var textViewOperationPart6: TextView
    private lateinit var textViewOperationPart7: TextView
    private lateinit var textViewOperationPart8: TextView
    private lateinit var tvPrinterName: TextView
    private lateinit var tvPrinterAddress: TextView
    private lateinit var progressBar: ProgressBar

    private var mDevicesArrayAdapter: ArrayAdapter<String>? = null

    private var operation: Operation = Operation()
    lateinit var preference: Preference

    private val dfZero: DecimalFormat = DecimalFormat("0.00")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_print)
        supportActionBar?.hide()

        preference = Preference(applicationContext)

        toolbar = findViewById(R.id.toolbar)
        textViewOperationPart1 = findViewById(R.id.textViewOperationPart1)
        textViewOperationPart2 = findViewById(R.id.textViewOperationPart2)
        textViewOperationPart3 = findViewById(R.id.textViewOperationPart3)
        textViewOperationPart4 = findViewById(R.id.textViewOperationPart4)
        textViewOperationPart5 = findViewById(R.id.textViewOperationPart5)
        textViewOperationPart6 = findViewById(R.id.textViewOperationPart6)
        textViewOperationPart7 = findViewById(R.id.textViewOperationPart7)
        textViewOperationPart8 = findViewById(R.id.textViewOperationPart8)
        tvPrinterName = findViewById(R.id.tvPrinterName)
        tvPrinterAddress = findViewById(R.id.tvPrinterAddress)
        progressBar = findViewById(R.id.progressBar)

        btnPrint = findViewById(R.id.btnPrint)
        btnPrint.setOnClickListener {printTicket()}
        btnDownload = findViewById(R.id.btnDownload)
        btnDownload.setOnClickListener {downloadTicket()}
        fabPrintSettings = findViewById(R.id.fabPrintSettings)
        fabPrintSettings.setOnClickListener {openModal()}

        btnStartDiscovery = findViewById(R.id.btnStartDiscovery)
        btnStartDiscovery.setOnClickListener {
            initDiscovery()
        }

        btnCancelDiscovery = findViewById(R.id.btnCancelDiscovery)
        btnCancelDiscovery.setOnClickListener {
            if (checkPermission()){
                bluetoothAdapter!!.cancelDiscovery()
            }else{
                requestPermissions()
            }
        }

        btnGoRoute = findViewById(R.id.btnGoRoute)
        btnGoRoute.setOnClickListener {
            disconnectBluetooth()
            val i = Intent(this@PrintActivity, MainActivity::class.java)
            i.putExtra("GO_TO_ROUTE", true)
            startActivity(i)
        }

        btnGoMap = findViewById(R.id.btnGoMap)
        btnGoMap.setOnClickListener {
            disconnectBluetooth()
            val i = Intent(this@PrintActivity, MainActivity::class.java)
            i.putExtra("GO_TO_MAP", true)
            startActivity(i)
        }
        btnGoOrders = findViewById(R.id.btnGoOrders)
        btnGoOrders.setOnClickListener {
            disconnectBluetooth()
            val i = Intent(this@PrintActivity, MainActivity::class.java)
            i.putExtra("GO_TO_ORDERS", true)
            startActivity(i)
        }

        if(intent != null){
            val operationID: Int = intent.getStringExtra("operationID")!!.toInt()
            loadOperation(operationID)
        }


//        var filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
//        registerReceiver(bluetoothScanBroadcastReceiver, filter)
//        filter = IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
//        registerReceiver(bluetoothScanBroadcastReceiver, filter)

        init()
        initBluetooth()


        val bundle = Bundle()
        bundle.putInt("userID", preference.getData("userID").toInt())
        bundle.putInt("vehicleID", preference.getData("vehicleID").toInt())
        bundle.putString("vehicleLicensePlate", preference.getData("vehicleLicensePlate"))

    }

    private fun initDiscovery(){
        if(preference.getData("printerName") == ""){
            Toast.makeText(applicationContext, "Seleccione dispositivo", Toast.LENGTH_LONG).show()

        }else{
            if (checkPermission()){

                if(bluetoothAdapter!!.isDiscovering)
                    bluetoothAdapter!!.cancelDiscovery()

                var filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
                registerReceiver(bluetoothScanBroadcastReceiver, filter)
                filter = IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
                registerReceiver(bluetoothScanBroadcastReceiver, filter)
                filter = IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
                registerReceiver(bluetoothScanBroadcastReceiver, filter)

                bluetoothAdapter!!.startDiscovery()
                progressBar.visibility = View.VISIBLE

            }else{
                requestPermissions()
            }
        }
    }
    private fun init(){
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun initBluetooth() {

        val bluetoothManager = applicationContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        if (bluetoothAdapter != null) {

            if (bluetoothAdapter!!.isEnabled) {
                getPrinter()
                /*if (checkPermission()){

                    var filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
                    registerReceiver(bluetoothScanBroadcastReceiver, filter)
                    filter = IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
                    registerReceiver(bluetoothScanBroadcastReceiver, filter)
                    filter = IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
                    registerReceiver(bluetoothScanBroadcastReceiver, filter)
                    bluetoothAdapter!!.startDiscovery()
                }else{
                    requestPermissions()
                }*/


            } else {
                if (checkPermission()){
                    val intentBluetooth = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    resultLauncher.launch(intentBluetooth)
                }else{
                    requestPermissions()
                }

            }
        } else {
            Toast.makeText(applicationContext, "Bluetooth is not available", Toast.LENGTH_LONG).show()
        }
    }

    private var resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            Toast.makeText(applicationContext, "Permission Bluetooth Granted", Toast.LENGTH_LONG).show()
        }else{
            Toast.makeText(applicationContext, "Permission Bluetooth Denied", Toast.LENGTH_LONG).show()
        }
    }


    private fun checkPermission(): Boolean{

        val permission3 = ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH)
        val permission4 = ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN)
        val permission5 = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
        val permission6 = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // Snow Cone
            val permission1 = ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
            val permission2 = ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
//            val permission7 = ContextCompat.checkSelfPermission(this, Manifest.permission.MANAGE_EXTERNAL_STORAGE)
            return permission1 == PackageManager.PERMISSION_GRANTED && permission2 == PackageManager.PERMISSION_GRANTED && permission5 == PackageManager.PERMISSION_GRANTED && permission6 == PackageManager.PERMISSION_GRANTED
        }else if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.R){ // Red Velvet Cake
            return permission3 == PackageManager.PERMISSION_GRANTED && permission4 == PackageManager.PERMISSION_GRANTED && permission5 == PackageManager.PERMISSION_GRANTED && permission6 == PackageManager.PERMISSION_GRANTED
        }else
            return permission3 == PackageManager.PERMISSION_GRANTED && permission5 == PackageManager.PERMISSION_GRANTED && permission6 == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) // Snow Cone
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION),200)
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P){
            Toast.makeText(applicationContext, "Android 9.0 Pistachio Ice", Toast.LENGTH_LONG).show()
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION),200)
        }
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
            Toast.makeText(applicationContext, "Android 10.0 Quince Tart", Toast.LENGTH_LONG).show()
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION),200)
        }
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            Toast.makeText(applicationContext, "Android 8.0 Oreo", Toast.LENGTH_LONG).show()
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION),200)
        }
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
            Toast.makeText(applicationContext, "Android 7.0 Nougat", Toast.LENGTH_LONG).show()
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION),200)
        }
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            Toast.makeText(applicationContext, "Android 6.0 Marshmallow", Toast.LENGTH_LONG).show()
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION),200)
        }
        else {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            requestBluetooth.launch(enableBtIntent)
        }
    }

    private var requestBluetooth = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            //granted
            if(listViewPairedDevices != null)
                getConnectedDevices()
            else
                getPrinter()
            Toast.makeText(applicationContext, "bluetooth request is granted", Toast.LENGTH_LONG).show()
        }else{
            //deny
            Toast.makeText(applicationContext, "bluetooth request is denied", Toast.LENGTH_LONG).show()
        }
    }
    private fun openModal(){
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.dialog_show_devices)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        listViewPairedDevices = dialog.findViewById(R.id.listViewPairedDevices)
        btnClose = dialog.findViewById(R.id.btnClose)
        btnClose.setOnClickListener{
            getPrinter()
            dialog.dismiss()
        }
        getConnectedDevices()
        dialog.show()
    }

    private fun getPrinter(){
        if(preference.getData("printerName") != ""){
            tvPrinterName.text = preference.getData("printerName")
            tvPrinterAddress.text = preference.getData("printerAddress")
            // btnStartDiscovery.visibility = View.VISIBLE
            initDiscovery()

        }else{
            btnPrint.isEnabled = false
        }
    }

    private fun getConnectedDevices(){

        if(bluetoothAdapter == null) {
            Toast.makeText(applicationContext, "Bluetooth is not available", Toast.LENGTH_LONG).show()
        }else{

            if(bluetoothAdapter!!.isEnabled){

                if (checkPermission()){
                    val pairedDevices = bluetoothAdapter!!.bondedDevices
                    mDevicesArrayAdapter = ArrayAdapter(this, R.layout.bluetooth_device_name_item)
                    // DEVICE_TYPE_CLASSIC: 1, DEVICE_TYPE_LE: 2, DEVICE_TYPE_DUAL: 3, DEVICE_TYPE_UNKNOWN: 0
                    // BOND_BONDED: 12, BOND_BONDING: 11, BOND_NONE: 10, DEVICE_TYPE_UNKNOWN: 0
                    if (pairedDevices.isNotEmpty()) {
                        var deviceType: String = ""
                        var deviceStatus: String = ""
                        for (device in pairedDevices) {
                            when (device.type){
                                BluetoothDevice.DEVICE_TYPE_CLASSIC -> {deviceType = "classic - BR/EDR"}
                                BluetoothDevice.DEVICE_TYPE_DUAL -> {deviceType = "dual - BR/EDR/LE"}
                                BluetoothDevice.DEVICE_TYPE_LE -> {deviceType = "low energy - LE-only"}
                                BluetoothDevice.DEVICE_TYPE_UNKNOWN -> {deviceType = "unknown"}
                            }
                            when (device.bondState){
                                BluetoothDevice.BOND_BONDED -> {deviceStatus = "emparejado"}
                                BluetoothDevice.BOND_BONDING -> {deviceStatus = "emparejamiento en curso"}
                                BluetoothDevice.BOND_NONE -> {deviceStatus = "no emparejado"}
                            }
                            mDevicesArrayAdapter!!.add("${device.name} | ${device.address} | $deviceType | ${deviceStatus}".trimIndent())
                        }
                        listViewPairedDevices.adapter = mDevicesArrayAdapter

                        listViewPairedDevices.setOnItemClickListener { adapterView, view, position, id ->

                            val valueText: String? = mDevicesArrayAdapter!!.getItem(position)
                            val list = valueText?.split(" | ")
                            if(list!!.isNotEmpty()){
                                preference.saveData("printerName",list[0].trim())
                                preference.saveData("printerAddress",list[1].trim())
                                btnClose.callOnClick()
                                // btnStartDiscovery.visibility = View.VISIBLE

                            }

                        }

                    }
                }else{
                    requestPermissions()
                }

            }
            else{
                Toast.makeText(applicationContext, "bluetoothAdapter is not enabled", Toast.LENGTH_LONG).show()
            }
        }
    }

    private val bluetoothScanBroadcastReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action){
                BluetoothDevice.ACTION_FOUND -> {
                    // Discovery has found a device. Get the BluetoothDevice
                    // object and its info from the Intent.
                    val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    if (checkPermission()){
                        val deviceName = device!!.name
                        val deviceHardwareAddress = device.address // MAC address
                        Log.d("DISCOVERING-DEVICE", "$deviceName $deviceHardwareAddress")
                        if (!deviceName.isNullOrBlank() && deviceName == preference.getData("printerName")) { // plinter1 Furgon
                            val uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") //Standard SerialPortService ID

                            bluetoothSocket = device.createRfcommSocketToServiceRecord(uuid)
                            bluetoothAdapter!!.cancelDiscovery()
                            btnPrint.isEnabled = true
                            // btnStartDiscovery.visibility = View.GONE
                            try {
                                bluetoothSocket!!.connect()
                                outputStream = bluetoothSocket!!.outputStream
                            } catch (exception: IOException) {
                                Toast.makeText(applicationContext, "IOException: $exception", Toast.LENGTH_LONG).show()
                            }
                        }
                    }else
                        requestPermissions()
                }
                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                    Log.d("DISCOVERING-STARTED", "isDiscovering")

                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED  -> {
                    Log.d("DISCOVERING-FINISHED", "FinishedDiscovering")
                    progressBar.visibility = View.GONE
                }
            }


        }
    }


    private fun loadOperation(id: Int) {

        val o = Operation()
        o.operationID=id
        val apiInterface = UserApiService.create().getSaleByID(o)
        apiInterface.enqueue(object : Callback<Operation> {
            override fun onResponse(call: Call<Operation>, response: Response<Operation>) {

                if (response.body() != null) {
                    operation = response.body()!!
                    showOperation()
                    btnDownload.isEnabled = true
                }
            }

            override fun onFailure(call: Call<Operation>, t: Throwable) {
                Log.d("MIKE", "loadOperation. Algo salio mal..." + t.message.toString())
            }
        })
    }

    private fun showOperation(){

        var textTicketPart1 = "DELDIA \n"
        textTicketPart1 += "DISTRIBUCIONES SAC \n"
        textTicketPart1 += "DISTRIBUIDOR FRITO LAY-KARINTO \n"
        textTicketPart1 += "Guillermo Delgado Miranda \n"
        textTicketPart1 += "TELEFONO 958245315 \n"
        textTicketPart1 += "------------------------------\n"

        textTicketPart1 += operation.documentTypeDisplay + " \n"
        textTicketPart1 += operation.documentNumber + " \n"

        var textTicketPart2 = "CLIENTE: " + operation.clientFullName + " \n"
        textTicketPart2 += operation.clientDocumentType + ": " + operation.clientDocumentNumber +" \n"
        textTicketPart2 += "DIRECCION: " + operation.clientAddress + " \n"
        textTicketPart2 += "CELULAR: " + operation.clientCellphone + " \n"
        textTicketPart2 += "FECHA: " + operation.operationDate + " \n"
        textTicketPart2 += "HORA: " + operation.operationTime + " \n"
        textTicketPart2 += "VENDEDOR: " + operation.userFullName + " \n"
        textTicketPart2 += "CUADRILLA: ${operation.gangName} ${operation.clientVisitDayDisplay.subSequence(0,2)}-${operation.clientObservation} \n"
        textTicketPart2 += "TIPO ENTREGA: ${operation.physicalDistributionDisplay} \n"

        val textTicketPart3 = "------------------------------\n"
        var textTicketPart4 = ""

        for (item in operation.details){
            textTicketPart4 += item.productSaleName.trim() + " \n"
            textTicketPart4 += "${dfZero.format(item.price)}  x  ${item.quantity}  =  ${dfZero.format(item.subtotal)} \n"
        }

        val textTicketPart5 = "------------------------------\n"
        var textTicketPart6 = ""
        val base: Double = Math.round(operation.total / (1.18) * 100.0) / 100.0
        val igv: Double = Math.round((operation.total - base) * 100.0) / 100.0
        textTicketPart6 += "OP. GRAVADAS: S/ ${dfZero.format(base)} \n"
        textTicketPart6 += "IGV: S/ ${dfZero.format(igv)} \n\n"
        textTicketPart6 += "TOTAL A PAGAR: S/ ${dfZero.format(operation.total)} \n\n\n"

        val textTicketPart7 = "SON: ${operation.numberToCurrency} \n\n"

        var textTicketPart8 = "Representacion impresa de la \n"
        textTicketPart8 += "${operation.documentTypeDisplay} DE VENTA ELECTRONICA,\n"
        textTicketPart8 += "consulte el documento en \n"
        textTicketPart8 += "https://4soluciones.pse.pe/20600854535\n"
        textTicketPart8 += "Emitido mediante un PROVEEDOR\n"
        textTicketPart8 += "Autorizado por la SUNAT mediante Resolucion de Intendencia\n"
        textTicketPart8 += "Nro.304 - 005 - 0005315 \n"
        textTicketPart8 += "GRACIAS POR SU COMPRA! \n"


        textViewOperationPart1.text = textTicketPart1
        textViewOperationPart2.text = textTicketPart2

        textViewOperationPart3.text = textTicketPart3
        textViewOperationPart4.text = textTicketPart4
        textViewOperationPart5.text = textTicketPart5
        textViewOperationPart6.text = textTicketPart6

        textViewOperationPart7.text = textTicketPart7
        textViewOperationPart8.text = textTicketPart8
    }

    private fun downloadTicket(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
        {
            if (!Environment.isExternalStorageManager())
            {

                val tk = Ticket(applicationContext!!, operation)
                tk.generatePDF()


                Handler().postDelayed({
//                    Log.d("MIKE", "Se genero el pdf. ${tk.getFilePathDirectory()}")
                    val internal = File("/sdcard/Documents/4SOFT-PDF")
//                    val internalContents: Array<File> = internal.listFiles()!!
//                    for (i in internalContents) {
//                        Log.d("MIKE", "directorio name:${i.name} path: ${i.path}")
//                    }

//                    val specificFile = File("/sdcard/Documents/4SOFT-PDF", "${operation.documentNumber}.pdf")
//                    Log.d("MIKE", "directorio specificFile: ${specificFile.name}")

                    val shareIntent = Intent(Intent.ACTION_SEND)
//                shareIntent.putExtra(Intent.EXTRA_STREAM,  uriFromFile(applicationContext,File(this.getExternalFilesDir(null)?.absolutePath.toString(), "$aName")))
                    shareIntent.putExtra(Intent.EXTRA_STREAM,  uriFromFile(applicationContext,File(internal.absolutePath.toString(), "${operation.documentNumber}.pdf")))
                    shareIntent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    shareIntent.type = "application/pdf"
//                    startActivity(Intent.createChooser(shareIntent, "share.."))


                    val chooser = Intent.createChooser(shareIntent, "Share File")

                    val resInfoList = this.packageManager.queryIntentActivities(
                        chooser,
                        PackageManager.MATCH_DEFAULT_ONLY
                    )

                    for (resolveInfo in resInfoList) {
                        val packageName = resolveInfo.activityInfo.packageName
                        grantUriPermission(
                            packageName,
                            uriFromFile(applicationContext,File(internal.absolutePath.toString(), "${operation.documentNumber}.pdf")),
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    }
                    startActivity(chooser)


                }, 1000)
            }
            else{

                Toast.makeText(applicationContext, "Permission needed!", Toast.LENGTH_SHORT).show()

            }

        }


            // Delay of 5 sec
//            Timer().schedule(2000){
//
//                Toast.makeText(applicationContext, "Se genero el pdf. ${tk.getFilePathDirectory()}", Toast.LENGTH_SHORT).show()
//                val shareIntent = Intent()
//                shareIntent.action = Intent.ACTION_SEND
//                shareIntent.type = "application/pdf"
//                shareIntent.addCategory(Intent.CATEGORY_OPENABLE)
////        startActivityForResult(shareIntent, 12)
////        shareIntent.putExtra(Intent.EXTRA_STREAM, tk.getFilePathDirectory())
//                ContextCompat.startActivity(applicationContext, Intent.createChooser(shareIntent, "Compartiendo archivo"), null)
//            }

    }

    private fun uriFromFile(context:Context, file:File):Uri {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
        {
            return FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".provider", file)
        }
        else
        {
            return Uri.fromFile(file)
        }
    }

    private fun printTicket(){
        if (outputStream != null) {

            val format = byteArrayOf(27, 33, 0)
            val arrayOfByte1 = byteArrayOf(27, 33, 0)
            format[2] = (0x8 or arrayOfByte1[2].toInt()).toByte()  // Bold
//                format[2] = (0x10 or arrayOfByte1[2].toInt()).toByte()  // Height
//                format[2] = (0x1 or arrayOfByte1[1].toInt()).toByte()  // Small

            outputStream!!.write(PrinterCommands.INIT)
            outputStream!!.write(PrinterCommands.ESC_ALIGN_CENTER)
//                outputStream!!.write(format)
            outputStream!!.write(format)
            outputStream!!.write("DELDIA \n".toByteArray())
            format[2] = (0x0 or arrayOfByte1[2].toInt()).toByte()
            outputStream!!.write(PrinterCommands.ESC_CANCEL_BOLD)
            outputStream!!.write("DISTRIBUCIONES SAC \n".toByteArray())
            outputStream!!.write("DISTRIBUIDOR FRITO LAY-KARINTO \n".toByteArray(Charsets.UTF_8))
            outputStream!!.write("Guillermo Delgado Miranda \n".toByteArray())
            outputStream!!.write("TELEFONO 958245315 \n".toByteArray())
            outputStream!!.write("------------------------------\n".toByteArray())

            outputStream!!.write(String("${operation.documentTypeDisplay} \n".toByteArray(Charsets.UTF_8), StandardCharsets.UTF_8).toByteArray())
            outputStream!!.write("${operation.documentNumber} \n".toByteArray())

            outputStream!!.write(PrinterCommands.ESC_ALIGN_LEFT)
            outputStream!!.write("\n".toByteArray())
            format[2] = (0x8 or arrayOfByte1[2].toInt()).toByte()
            outputStream!!.write(format)
            outputStream!!.write("CLIENTE: ${operation.clientFullName} \n".toByteArray())
            format[2] = (0x0 or arrayOfByte1[2].toInt()).toByte()
            outputStream!!.write(PrinterCommands.ESC_CANCEL_BOLD)
            outputStream!!.write("${operation.clientDocumentType}: ${operation.clientDocumentNumber} \n".toByteArray())
            outputStream!!.write("DIRECCION: ${operation.clientAddress} \n".toByteArray())
            outputStream!!.write("CELULAR: ${operation.clientCellphone} \n".toByteArray())
            outputStream!!.write("FECHA: ${operation.operationDate} \n".toByteArray())
            outputStream!!.write("HORA: ${operation.operationTime} \n".toByteArray())
            outputStream!!.write("VENDEDOR: ${operation.userFullName} \n".toByteArray())
            outputStream!!.write("CUADRILLA: ${operation.gangName} ${operation.clientVisitDayDisplay.subSequence(0,2)}-${operation.clientObservation} \n".toByteArray())
            outputStream!!.write("TIPO ENTREGA: ${operation.physicalDistributionDisplay} \n".toByteArray())
            outputStream!!.write(PrinterCommands.ESC_ALIGN_CENTER)
            outputStream!!.write("------------------------------\n".toByteArray())

            for (item in operation.details){
                outputStream!!.write(PrinterCommands.ESC_ALIGN_LEFT)
                outputStream!!.write("${item.productSaleName.trim()} \n".toByteArray())
                outputStream!!.write(PrinterCommands.ESC_ALIGN_RIGHT)
                outputStream!!.write("${dfZero.format(item.price)}  x  ${item.quantity}  =  ${dfZero.format(item.subtotal)} \n".toByteArray())
            }

            outputStream!!.write(PrinterCommands.ESC_ALIGN_CENTER)
            outputStream!!.write("------------------------------\n".toByteArray())


            val base: Double = Math.round(operation.total / (1.18) * 100.0) / 100.0
            val igv: Double = Math.round((operation.total - base) * 100.0) / 100.0


            outputStream!!.write(format)
            outputStream!!.write(PrinterCommands.ESC_ALIGN_RIGHT)
            outputStream!!.write("OP. GRAVADAS: S/ ${dfZero.format(base)} \n".toByteArray())
            outputStream!!.write("IGV: S/ ${dfZero.format(igv)} \n".toByteArray())
            outputStream!!.write("\n".toByteArray())
            outputStream!!.write("\n".toByteArray())
            format[2] = (0x8 or arrayOfByte1[2].toInt()).toByte()
            outputStream!!.write(format)
            outputStream!!.write("TOTAL A PAGAR: S/ ${dfZero.format(operation.total)} \n".toByteArray())
            format[2] = (0x0 or arrayOfByte1[2].toInt()).toByte()

            outputStream!!.write("\n".toByteArray())
            outputStream!!.write("\n".toByteArray())
            outputStream!!.write(PrinterCommands.ESC_ALIGN_LEFT)
            outputStream!!.write("SON: ${operation.numberToCurrency} \n".toByteArray())
            outputStream!!.write("\n".toByteArray())

            outputStream!!.write(PrinterCommands.ESC_ALIGN_CENTER)
//                format[2] = (0x1 or arrayOfByte1[1].toInt()).toByte()
//                outputStream!!.write(format)
            outputStream!!.write("Representacion impresa de la \n".toByteArray())
            outputStream!!.write("${operation.documentTypeDisplay} DE VENTA ELECTRONICA,\n".toByteArray())
            outputStream!!.write("consulte el documento en \n".toByteArray())
            outputStream!!.write("https://4soluciones.pse.pe/20600854535\n".toByteArray())
            outputStream!!.write("Emitido mediante un PROVEEDOR\n".toByteArray())
            outputStream!!.write("Autorizado por la SUNAT mediante Resolucion de Intendencia\n".toByteArray())
            outputStream!!.write("Nro.304 - 005 - 0005315 \n".toByteArray())
            outputStream!!.write("GRACIAS POR SU COMPRA! \n".toByteArray())
            outputStream!!.write("\n".toByteArray())
            outputStream!!.write("\n".toByteArray())

            // outputStream!!.write("TOC 20220906-00000-542216 \n".toByteArray())

            outputStream!!.write(PrinterCommands.FEED_LINE)

            // disconnectBluetooth()

            // val i = Intent(this@PrintActivity, MainActivity::class.java)
            /*val bundle = Bundle().apply {
                putInt("userID", operation.userID)
                putInt("operationID", operation.operationID.toInt())
            }*/

            // startActivity(i)

        }
        else{
//            if (checkPermission()){
//                bluetoothAdapter!!.startDiscovery()
//                val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
//                registerReceiver(bluetoothScanBroadcastReceiver, filter)
//            }else{
//                requestPermissions()
//            }
             Toast.makeText(applicationContext, "No hay flujo de salida de bytes", Toast.LENGTH_LONG).show()
        }
    }
    private fun disconnectBluetooth(){
        val outputStream : OutputStream? = null

        try {
            outputStream?.close()
        } catch (e: IOException ){}

        try {
            bluetoothSocket?.close()
        } catch (e: IOException){}

        bluetoothSocket = null
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiver(bluetoothScanBroadcastReceiver, filter)
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(bluetoothScanBroadcastReceiver)
    }

    /*override fun onDestroy() {
        super.onDestroy()
        if (bluetoothAdapter != null)
            bluetoothAdapter!!.cancelDiscovery()
    }*/
}