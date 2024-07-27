package com.example.deldia.web

import android.app.Application
//import com.mazenrashed.printooth.Printooth
import kotlin.reflect.KParameter

class ApplicationClass: Application() {
    override fun onCreate() {
        super.onCreate()
//        Printooth.init(this)
//        startActivityForResult(Intent(this, ScanningActivity::class.java), ScanningActivity.SCANNING_FOR_PRINTER)

    }

}