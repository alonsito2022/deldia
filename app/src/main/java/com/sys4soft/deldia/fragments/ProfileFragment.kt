package com.sys4soft.deldia.fragments

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import com.sys4soft.deldia.BluetoothActivity
import com.sys4soft.deldia.R
import com.sys4soft.deldia.localdatabase.Preference

class ProfileFragment : Fragment() {
    private var globalContext: Context? = null
    lateinit var preference: Preference

    private lateinit var btnSettingBluetooth: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        globalContext = this.activity
        preference = Preference(globalContext)


    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        var editTextUserName = view.findViewById<EditText>(R.id.editTextUserName)
        var editTextUserEmail = view.findViewById<EditText>(R.id.editTextUserEmail)
        var editTextGangName = view.findViewById<EditText>(R.id.editTextGangName)
        var editTextVehicle = view.findViewById<EditText>(R.id.editTextVehicle)

        editTextUserName.setText(preference.getData("userName"))
        editTextUserEmail.setText(preference.getData("userEmail"))
        editTextGangName.setText(preference.getData("gangName"))
        editTextVehicle.setText(preference.getData("vehicleLicensePlate"))

        btnSettingBluetooth = view.findViewById(R.id.btnSettingBluetooth)
        btnSettingBluetooth.setOnClickListener {
            val intent = Intent(globalContext as Activity, BluetoothActivity::class.java)
            startActivity(intent)
        }

    }

}