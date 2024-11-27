package com.example.deldia

import android.Manifest
import android.Manifest.permission.*
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.deldia.web.BluetoothPrint
//import com.github.barteksc.pdfviewer.PDFView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
//import com.mazenrashed.printooth.Printooth
//import com.mazenrashed.printooth.data.printable.Printable
//import com.mazenrashed.printooth.data.printable.RawPrintable
//import com.mazenrashed.printooth.data.printable.TextPrintable
//import com.mazenrashed.printooth.data.printer.DefaultPrinter
//import com.mazenrashed.printooth.ui.ScanningActivity
//import com.mazenrashed.printooth.utilities.Printing
//import com.mazenrashed.printooth.utilities.PrintingCallback
import kotlinx.android.synthetic.main.activity_view.*
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.util.*

class ViewActivity : AppCompatActivity() {

//    private lateinit var pdf_view : PDFView
    private lateinit var toolbar : Toolbar
    private lateinit var fabPrint: FloatingActionButton
//    private var printing : Printing? = null
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothSocket: BluetoothSocket? = null
//    private var bluetoothAdapter: BluetoothAdapter? = null
    private var outputStream: OutputStream? = null

    /*private val bluetoothScanBroadcastReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == BluetoothDevice.ACTION_FOUND) {
                val bluetoothDevice = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                if (bluetoothDevice != null) {
                    if (ContextCompat.checkSelfPermission(applicationContext,BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(applicationContext as Activity, arrayOf(ACCESS_COARSE_LOCATION), 200)
                    }
                    if (bluetoothDevice.name == "MTP-II") {
                        val uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

                        bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(uuid)
                        bluetoothAdapter!!.cancelDiscovery()
                        try {
                            bluetoothSocket!!.connect()
                            outputStream = bluetoothSocket!!.outputStream
                        } catch (exception: IOException) {
                            Log.e("SignInActivity", "IOException: $exception")
                        }
                    }
                }
            }
        }
    }*/

    /*override fun onResume() {
        super.onResume()
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiver(bluetoothScanBroadcastReceiver, filter)
    }

    override fun onPause() {
        super.onPause()
        super.onPause()
        unregisterReceiver(bluetoothScanBroadcastReceiver)
    }*/

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view)
        supportActionBar?.hide()
//        pdf_view = findViewById(R.id.pdfView)
        toolbar = findViewById(R.id.toolbar)

        fabPrint = findViewById(R.id.fabPrint)
        fabPrint.setOnClickListener {
//            if (outputStream != null) {
//                outputStream!!.write("hello, works".toByteArray())
//            }
        }

        /*val bluetoothManager = applicationContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        // Checks if Bluetooth Adapter is present
        if (bluetoothAdapter != null) {

            if (bluetoothAdapter!!.isEnabled) {
                if (ContextCompat.checkSelfPermission(this,  BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(applicationContext as Activity, arrayOf(BLUETOOTH_SCAN), 200)
                }
                bluetoothAdapter!!.startDiscovery()
                // Listen for broadcast
                val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
                registerReceiver(bluetoothScanBroadcastReceiver, filter)
            } else {
                // Enable Bluetooth
                val intentBluetooth = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(intentBluetooth, 200)
            }
        } else {
//            Snackbar.make(binding.root, "Bluetooth Not Supported", Snackbar.LENGTH_SHORT).show()
            Toast.makeText(applicationContext, "Bluetooth Not Supported", Toast.LENGTH_LONG).show()

        }*/
//        if (Printooth.hasPairedPrinter())
//            printing = Printooth.printer()

//        initListeners()
        // Printooth.init(this)



        if(intent != null){
            val selectedPDF = Uri.parse(intent.getStringExtra("FileUri"))

            Log.d("MIKE", intent.getStringExtra("FileUri").toString())

            val path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            val dir = File(path, "ExampleITextPDF")
            val file = File(dir, "operations.pdf")

//            pdfView.fromFile(file)
//                .password(null)
//                .defaultPage(0)
//                .enableSwipe(true)
//                .swipeHorizontal(false)
//                .onDraw{ canvas, pageWitdth, pageHeight, diplayedPage ->}
//                .onDrawAll{ canvas, pageWitdth, pageHeight, diplayedPage ->}
//                .onPageChange {page, pageCount ->}
//                .onPageError{page, t-> Toast.makeText(this, "Error $page", Toast.LENGTH_SHORT).show()}
//                .onTap{false}
//                .onRender{ nbPages, pageWidth, pageHeight -> pdf_view.fitToWidth()}
//                .enableAnnotationRendering(true)
//                .invalidPageColor(Color.RED)
//                .load()
        }
        init()

    }

//    private fun initListeners() {
//        fabPrint.setOnClickListener {
//
//            //Toast.makeText(applicationContext, "Print", Toast.LENGTH_LONG).show()
//
//            if (!Printooth.hasPairedPrinter())
//                resultLauncher.launch(
//                    Intent(
//                        applicationContext,
//                        ScanningActivity::class.java
//                    ),
//                )
//            else printDetails()
//
//
//        }
//
//
//        printing?.printingCallback = object : PrintingCallback{
//            override fun connectingWithPrinter() {
//                Toast.makeText(applicationContext, "Connecting with printer", Toast.LENGTH_SHORT).show()
//            }
//
//            override fun connectionFailed(error: String) {
//                Toast.makeText(applicationContext, "Failed to connect printer", Toast.LENGTH_SHORT).show()
//            }
//
//            override fun onError(error: String) {
//                Toast.makeText(applicationContext, error, Toast.LENGTH_SHORT).show()
//            }
//
//            override fun onMessage(message: String) {
//                Toast.makeText(applicationContext, "Message: $message", Toast.LENGTH_SHORT).show()
//            }
//
//            override fun printingOrderSentSuccessfully() {
//                Toast.makeText(applicationContext, "Order sent to printer", Toast.LENGTH_SHORT).show()
//            }
//        }
//
//
//    }
//
//    private fun printDetails() {
//        val printables = getSomePrintables()
//        printing?.print(printables)
//    }
//
//    /* Customize your printer here with text, logo and QR code */
//    private fun getSomePrintables() = ArrayList<Printable>().apply {
//
//        add(RawPrintable.Builder(byteArrayOf(27, 100, 4)).build()) // feed lines example in raw mode
//
//
//        //logo
////            add(ImagePrintable.Builder(R.drawable.bold, resources)
////                    .setAlignment(DefaultPrinter.ALIGNMENT_CENTER)
////                    .build())
//
//
//        add(
//            TextPrintable.Builder()
//                .setText("Printer")
//                .setLineSpacing(DefaultPrinter.LINE_SPACING_60)
//                .setAlignment(DefaultPrinter.ALIGNMENT_CENTER)
//                .setFontSize(DefaultPrinter.FONT_SIZE_LARGE)
//                .setEmphasizedMode(DefaultPrinter.EMPHASIZED_MODE_BOLD)
//                .setUnderlined(DefaultPrinter.UNDERLINED_MODE_OFF)
//                .setNewLinesAfter(1)
//                .build())
//
//
//        add(
//            TextPrintable.Builder()
//                .setText("TID: 1111123322" )
//                .setCharacterCode(DefaultPrinter.CHARCODE_PC1252)
//                .setNewLinesAfter(1)
//                .build())
//
//        add(
//            TextPrintable.Builder()
//                .setText("RRN: : 234566dfgg4456")
//                .setCharacterCode(DefaultPrinter.CHARCODE_PC1252)
//                .setNewLinesAfter(1)
//                .build())
//
//        add(
//            TextPrintable.Builder()
//                .setText("Amount: NGN$200,000")
//                .setCharacterCode(DefaultPrinter.CHARCODE_PC1252)
//                .setNewLinesAfter(2)
//                .build())
//
//
//        add(
//            TextPrintable.Builder()
//                .setText("APPROVED")
//                .setLineSpacing(DefaultPrinter.LINE_SPACING_60)
//                .setAlignment(DefaultPrinter.ALIGNMENT_CENTER)
//                .setFontSize(DefaultPrinter.FONT_SIZE_LARGE)
//                .setEmphasizedMode(DefaultPrinter.EMPHASIZED_MODE_BOLD)
//                .setUnderlined(DefaultPrinter.UNDERLINED_MODE_OFF)
//                .setNewLinesAfter(1)
//                .build())
//
//
//        add(
//            TextPrintable.Builder()
//                .setText("Transaction: Withdrawal")
//                .setCharacterCode(DefaultPrinter.CHARCODE_PC1252)
//                .setNewLinesAfter(1)
//                .build())
//
//
//        /*val qr: Bitmap = QRCode.from("RRN: : 234566dfgg4456\nAmount: NGN\$200,000\n")
//            .withSize(200, 200).bitmap()
//
//        add(
//            ImagePrintable.Builder(qr)
//                .setAlignment(DefaultPrinter.ALIGNMENT_CENTER)
//                .build())*/
//
//
//        add(TextPrintable.Builder()
//            .setText("Hello World")
//            .setLineSpacing(DefaultPrinter.LINE_SPACING_60)
//            .setAlignment(DefaultPrinter.ALIGNMENT_CENTER)
//            .setEmphasizedMode(DefaultPrinter.EMPHASIZED_MODE_BOLD)
//            .setUnderlined(DefaultPrinter.UNDERLINED_MODE_ON)
//            .setNewLinesAfter(1)
//            .build())
//
//        add(TextPrintable.Builder()
//            .setText("Hello World")
//            .setAlignment(DefaultPrinter.ALIGNMENT_RIGHT)
//            .setEmphasizedMode(DefaultPrinter.EMPHASIZED_MODE_BOLD)
//            .setUnderlined(DefaultPrinter.UNDERLINED_MODE_ON)
//            .setNewLinesAfter(1)
//            .build())
//
//        add(RawPrintable.Builder(byteArrayOf(27, 100, 4)).build())
//
//    }

    private fun init(){
        toolbar.setNavigationOnClickListener(object : View.OnClickListener{
            override fun onClick(p0: View?) {
                finish()
            }
        })
    }

    /*override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ScanningActivity.SCANNING_FOR_PRINTER && resultCode == Activity.RESULT_OK)
        //Printer is ready now
    }*/

    /* Inbuilt activity to pair device with printer or select from list of pair bluetooth devices */
//    var resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
//        if (result.resultCode == ScanningActivity.SCANNING_FOR_PRINTER &&  result.resultCode == Activity.RESULT_OK) {
//            // There are no request codes
////            val intent = result.data
//            printDetails()
//        }
//    }
}