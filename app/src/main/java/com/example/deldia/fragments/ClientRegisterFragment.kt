package com.example.deldia.fragments

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.deldia.R
import com.example.deldia.adapter.GangAdapter
import com.example.deldia.localdatabase.Preference
import com.example.deldia.models.Gang
import com.example.deldia.models.Person
import com.example.deldia.retrofit.UserApiService
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

class ClientRegisterFragment : Fragment(), OnMapReadyCallback {

    private lateinit var textInputEditTextPersonName: TextInputEditText
    private lateinit var textInputEditTextPersonFirstSurname: TextInputEditText
    private lateinit var textInputEditTextPersonSecondSurname: TextInputEditText
    private lateinit var textInputLayoutFirstSurname: TextInputLayout
    private lateinit var textInputLayoutSecondSurname: TextInputLayout
    private lateinit var textInputLayoutDocumentNumber: TextInputLayout
    private lateinit var textInputLayoutGang: TextInputLayout
    private lateinit var autoCompleteDocumentType: AutoCompleteTextView
    private lateinit var autoCompleteVisitDay: AutoCompleteTextView
    private lateinit var autoCompleteGang: AutoCompleteTextView
    private lateinit var textInputEditTextDocumentNumber: TextInputEditText
    private lateinit var progressBar: ProgressBar
    private lateinit var switchStatus: Switch
    private lateinit var btnSaveAndQuote: Button
    private lateinit var btnSaveAndSell: Button
    private lateinit var btnSaveAndGoToMap: Button

    private lateinit var textInputEditTextAddressName: TextInputEditText
    private lateinit var autoCompleteDistrict: AutoCompleteTextView
    private lateinit var autoCompleteCustomerType: AutoCompleteTextView
    private lateinit var autoCompleteShowcases: AutoCompleteTextView
    private lateinit var textInputEditTextLatitude: TextInputEditText
    private lateinit var textInputEditTextLongitude: TextInputEditText
    private lateinit var textInputEditTextComment: TextInputEditText
    private lateinit var textInputEditTextPersonObservation: TextInputEditText
    private lateinit var textInputEditTextPersonCellPhone: TextInputEditText

    private lateinit var mMap: GoogleMap
    var mapView: View? = null
    lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    private var person: Person = Person()
    private var searchPerson: Person = Person()
    private var listGangs = arrayListOf<Gang>()
    private var vehicleID: Int = 0
    private var userID: Int = 0
    private var gangID: Int = 0
    private var gangName: String = ""

    private var globalContext: Context? = null
    private lateinit var preference: Preference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        globalContext = this.activity
        preference = Preference(globalContext)
        val bundle = arguments
        vehicleID = preference.getData("vehicleID").toInt()
        userID = preference.getData("userID").toInt()
        gangID = preference.getData("gangID").toInt()
        gangName = preference.getData("gangName")
        loadDistributors()
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(globalContext!!)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_client_register, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        autoCompleteDocumentType = view.findViewById(R.id.autoCompleteDocumentType)
        autoCompleteVisitDay = view.findViewById(R.id.autoCompleteVisitDay)
        textInputLayoutGang = view.findViewById(R.id.textInputLayoutGang)
        autoCompleteGang = view.findViewById(R.id.autoCompleteGang)
        autoCompleteCustomerType = view.findViewById(R.id.autoCompleteCustomerType)
        autoCompleteShowcases = view.findViewById(R.id.autoCompleteShowcases)
        textInputEditTextDocumentNumber = view.findViewById(R.id.textInputEditTextDocumentNumber)
        progressBar = view.findViewById(R.id.progressBar)
        textInputLayoutDocumentNumber = view.findViewById(R.id.textInputLayoutDocumentNumber)
        textInputLayoutDocumentNumber.setEndIconDrawable(R.drawable.ic_baseline_search_24) // Usa un ícono de lupa o similar

        textInputEditTextPersonName = view.findViewById(R.id.textInputEditTextPersonName)
        textInputEditTextPersonFirstSurname = view.findViewById(R.id.textInputEditTextPersonFirstSurname)
        textInputEditTextPersonSecondSurname = view.findViewById(R.id.textInputEditTextPersonSecondSurname)
        textInputLayoutFirstSurname = view.findViewById(R.id.textInputLayoutFirstSurname)
        textInputLayoutSecondSurname = view.findViewById(R.id.textInputLayoutSecondSurname)
        textInputEditTextComment = view.findViewById(R.id.textInputEditTextComment)
        btnSaveAndQuote = view.findViewById(R.id.btnSaveAndQuote)
        btnSaveAndSell = view.findViewById(R.id.btnSaveAndSell)
        btnSaveAndGoToMap = view.findViewById(R.id.btnSaveAndGoToMap)

        textInputEditTextPersonObservation = view.findViewById(R.id.textInputEditTextPersonObservation)
        textInputEditTextPersonCellPhone = view.findViewById(R.id.textInputEditTextPersonCellPhone)

        textInputEditTextAddressName = view.findViewById(R.id.textInputEditTextAddressName)
        autoCompleteDistrict = view.findViewById(R.id.autoCompleteDistrict)
        textInputEditTextLatitude = view.findViewById(R.id.textInputEditTextLatitude)
        textInputEditTextLongitude = view.findViewById(R.id.textInputEditTextLongitude)
        textInputEditTextLatitude.isFocusable = false
        textInputEditTextLongitude.isFocusable = false

        textInputLayoutDocumentNumber.setEndIconOnClickListener {
            val itemSelected = autoCompleteDocumentType.text.toString()
            val documentNumber = textInputEditTextDocumentNumber.text.toString()

            val isValid = when (itemSelected) {
                "DNI" -> documentNumber.length == 8
                "RUC" -> documentNumber.length == 11
                else -> false
            }

            if (isValid) {
                person.documentType = when (itemSelected) {
                    "DNI" -> "01"
                    "RUC" -> "06"
                    else -> return@setEndIconOnClickListener
                }
                person.documentNumber = documentNumber
                person.isClient = true
                fetchedPerson()
            } else {
                Toast.makeText(requireContext(), "Número de documento inválido", Toast.LENGTH_SHORT).show()
                textInputEditTextPersonName.setText("")
                textInputEditTextPersonFirstSurname.setText("")
                textInputEditTextPersonSecondSurname.setText("")
                textInputEditTextAddressName.setText("")
            }
        }

        autoCompleteGang.setOnClickListener {
            autoCompleteGang.showDropDown() // Muestra el menú desplegable al hacer clic
        }

        switchStatus = view.findViewById(R.id.switchStatus)
        switchStatus.setOnCheckedChangeListener { _, isChecked ->
            person.isEnabled = isChecked
            switchStatus.text = if (isChecked) "ACTIVO" else "INACTIVO"
        }
        person.isEnabled = true

        textInputEditTextDocumentNumber.addTextChangedListener(object : TextWatcher {
            private var handler = Handler(Looper.getMainLooper())
            private var hideKeyboardRunnable: Runnable? = null

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                hideKeyboardRunnable?.let { handler.removeCallbacks(it) } // Cancela ocultar teclado si el usuario sigue escribiendo
            }

            override fun afterTextChanged(editable: Editable?) {
                if (!editable.isNullOrEmpty()) {
                    hideKeyboardRunnable = Runnable {
                        hideKeyboard(textInputEditTextDocumentNumber)
                    }
                    handler.postDelayed(hideKeyboardRunnable!!, 1000) // Ocultar teclado después de 1 segundo sin escribir
                }
            }
        })

        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm")
        val time = Calendar.getInstance().time
        val currentDate = sdf.format(time)
        var indexDayOfWeek = 0

        val c = Calendar.getInstance()
        val day = c.get(Calendar.DAY_OF_WEEK)
        Log.d("Mike", day.toString())
        when (day) {
            Calendar.SUNDAY -> {indexDayOfWeek = 6}
            Calendar.MONDAY -> {indexDayOfWeek = 0}
            Calendar.TUESDAY -> {indexDayOfWeek = 1}
            Calendar.WEDNESDAY -> {indexDayOfWeek = 2}
            Calendar.THURSDAY -> {indexDayOfWeek = 3}
            Calendar.FRIDAY -> {indexDayOfWeek = 4}
            Calendar.SATURDAY -> {indexDayOfWeek = 5}
        }


        val listDay = listOf("LUNES", "MARTES", "MIERCOLES", "JUEVES", "VIERNES", "SABADO", "DOMINGO")
        val adapterDay = ArrayAdapter(
            globalContext!!,
            android.R.layout.simple_spinner_dropdown_item,
            listDay
        )
        autoCompleteVisitDay.keyListener = null
        autoCompleteVisitDay.setAdapter(adapterDay)
        autoCompleteVisitDay.setText(
            autoCompleteVisitDay.adapter.getItem(indexDayOfWeek).toString(),
            false
        )

        val listMethod = listOf("DNI", "RUC")

        val adapter = ArrayAdapter(
            globalContext!!,
            android.R.layout.simple_spinner_dropdown_item,
            listMethod
        )
        autoCompleteDocumentType.keyListener = null
        autoCompleteDocumentType.setAdapter(adapter)
        autoCompleteDocumentType.setText(
            autoCompleteDocumentType.adapter.getItem(0).toString(),
            false
        )

        autoCompleteDocumentType.setOnItemClickListener { _, _, position, l ->
            val itemSelected = adapter.getItem(position).toString()
            if(itemSelected=="DNI"){
                person.documentType = "01"
                textInputLayoutFirstSurname.isVisible = true
                textInputLayoutSecondSurname.isVisible = true
            }else{
                person.documentType = "06"
                textInputLayoutFirstSurname.isVisible = false
                textInputLayoutSecondSurname.isVisible = false
            }
        }
        person.documentType = "01"

        val listCustomerTypes = listOf("PROPIO", "PEPSICO")
        val adapterCustomerTypes = ArrayAdapter(
            globalContext!!,
            android.R.layout.simple_spinner_dropdown_item,
            listCustomerTypes
        )
        autoCompleteCustomerType.keyListener = null
        autoCompleteCustomerType.setAdapter(adapterCustomerTypes)
        autoCompleteCustomerType.setText(
            autoCompleteCustomerType.adapter.getItem(0).toString(),
            false
        )

        val listShowcases = listOf("NO CUENTA", "AEREO", "2 NIVELES", "5 NIVELES", "6X8 NIVELES")
        val adapterShowcases = ArrayAdapter(
            globalContext!!,
            android.R.layout.simple_spinner_dropdown_item,
            listShowcases
        )
        autoCompleteShowcases.keyListener = null
        autoCompleteShowcases.setAdapter(adapterShowcases)
        autoCompleteShowcases.setText(
            autoCompleteShowcases.adapter.getItem(0).toString(),
            false
        )

        val listDistrict = listOf(
            "JOSE LUIS BUSTAMANTE Y RIVERO",
            "SOCABAYA",
            "SABANDIA",
            "CHARACATO",
            "QUEQUEÑA",
            "YARABAMBA",
            "MOLLEBAYA"
        )
        val adapterDistrict = ArrayAdapter(
            globalContext!!,
            android.R.layout.simple_spinner_dropdown_item,
            listDistrict
        )
        autoCompleteDistrict.keyListener = null
        autoCompleteDistrict.setAdapter(adapterDistrict)
        autoCompleteDistrict.setText(
            autoCompleteDistrict.adapter.getItem(0).toString(),
            false
        )

        btnSaveAndSell.setOnClickListener { handleSaveAndSend("S") }
        btnSaveAndQuote.setOnClickListener { handleSaveAndSend("Q") }
        btnSaveAndGoToMap.setOnClickListener { handleSaveAndSend("N") }

        val mapFragment = childFragmentManager.findFragmentById(R.id.frg) as SupportMapFragment?
        mapView = mapFragment!!.view
        mapFragment.getMapAsync(this)
    }
    private fun hideKeyboard(view: View) {
        val inputMethodManager = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }
    private fun saveData(): Boolean {

        if (person.gangID == 0) {  // RUTA
            Toast.makeText(globalContext, "Verifique RUTA", Toast.LENGTH_SHORT).show()
            return false
        }
        val valueCustomerTypes = autoCompleteCustomerType.text.toString()
        val selectedCustomerTypes = when (valueCustomerTypes) {
            "PEPSICO" -> 0
            "PROPIO" -> 1
            else -> -1
        }

        val valueShowcases = autoCompleteShowcases.text.toString()
        val selectedShowcases = when (valueShowcases) {
            "NO CUENTA" -> 0
            "AEREO" -> 1
            "2 NIVELES" -> 2
            "5 NIVELES" -> 3
            "6X8 NIVELES" -> 4
            else -> -1
        }

        val valueVisitDay = autoCompleteVisitDay.text.toString()
        val selectedVisitDay = when (valueVisitDay) {
            "LUNES" -> 0
            "MARTES" -> 1
            "MIERCOLES" -> 2
            "JUEVES" -> 3
            "VIERNES" -> 4
            "SABADO" -> 5
            "DOMINGO" -> 6
            else -> -1
        }

        // Validaciones de DNI y RUC antes de asignar
        val docNumber = textInputEditTextDocumentNumber.text.toString()
        if (person.documentType == "01" && docNumber.length != 8) {  // DNI
            Toast.makeText(globalContext, "Verifique DNI", Toast.LENGTH_SHORT).show()
            return false
        }
        if (person.documentType == "06" && docNumber.length != 11) {  // RUC
            Toast.makeText(globalContext, "Verifique RUC", Toast.LENGTH_SHORT).show()
            return false
        }

        // Validación de ubicación antes de asignar
        val latitudeText = textInputEditTextLatitude.text.toString()
        if (latitudeText.isEmpty()) {
            Toast.makeText(globalContext, "Verifique posición", Toast.LENGTH_SHORT).show()
            return false
        }

        val nameText = textInputEditTextPersonName.text.toString()
        if (nameText.isEmpty()) {
            Toast.makeText(globalContext, "Verifique nombre", Toast.LENGTH_SHORT).show()
            return false
        }

        val addressText = textInputEditTextAddressName.text.toString()
        if (addressText.isEmpty() && person.documentType == "06") {
            Toast.makeText(globalContext, "Verifique dirección", Toast.LENGTH_SHORT).show()
            return false
        }
        val codeText = textInputEditTextPersonObservation.text.toString()
        if (codeText.isEmpty()) {
            Toast.makeText(globalContext, "Verifique codigo", Toast.LENGTH_SHORT).show()
            return false
        }

        // Asignación de valores después de pasar las validaciones
        person.documentNumber = docNumber
        person.name = textInputEditTextPersonName.text.toString()
        person.firstSurname = textInputEditTextPersonFirstSurname.text.toString()
        person.secondSurname = textInputEditTextPersonSecondSurname.text.toString()
        person.comment = textInputEditTextComment.text.toString()
        person.isClient = true

        person.visitDay = selectedVisitDay
        person.showcases = selectedShowcases
        person.customerType = selectedCustomerTypes
        person.observation = textInputEditTextPersonObservation.text.toString()
        person.cellphone = textInputEditTextPersonCellPhone.text.toString()

        val valueDistrict = autoCompleteDistrict.text.toString()
        val selectedDistrict = when (valueDistrict) {
            "JOSE LUIS BUSTAMANTE Y RIVERO" -> "040129"
            "SOCABAYA" -> "040119"
            "SABANDIA" -> "040113"
            "CHARACATO" -> "040104"
            "QUEQUEÑA" -> "040112"
            "YARABAMBA" -> "040124"
            "MOLLEBAYA" -> "040108"
            else -> ""
        }

        person.district = selectedDistrict
        person.address = addressText
        person.latitude = latitudeText.toDouble()
        person.longitude = textInputEditTextLongitude.text.toString().toDouble()

        return true
    }

    private fun fetchedPerson(){
        progressBar.visibility = View.VISIBLE  // 🔴 Mostrar loader

        val apiInterface = UserApiService.create().getDocumentConsultation(person)
        apiInterface.enqueue(object : Callback<Person> {
            override fun onResponse(
                call: Call<Person>,
                response: Response<Person>
            ) {
                progressBar.visibility = View.GONE  // ✅ Ocultar loader

                if (response.body() != null) {
                    val personTemp = response.body()!!
                    textInputEditTextPersonName.setText(personTemp.name)
                    textInputEditTextPersonFirstSurname.setText(personTemp.firstSurname)
                    textInputEditTextPersonSecondSurname.setText(personTemp.secondSurname)

                    person.name = personTemp.name
                    person.firstSurname = personTemp.firstSurname
                    person.secondSurname = personTemp.secondSurname

                    if (person.documentType == "06") {
                        textInputEditTextAddressName.setText(personTemp.address)
                        person.address = personTemp.address
                    }

                }
            }

            override fun onFailure(call: Call<Person>, t: Throwable) {
                progressBar.visibility = View.GONE  // ✅ Ocultar loader incluso si hay error
                Log.d("MIKE", "Algo salio mal..." + t.message.toString())
            }
        })

    }
    private fun setButtonsEnabled(enabled: Boolean) {
        btnSaveAndSell.isEnabled = enabled
        btnSaveAndQuote.isEnabled = enabled
        btnSaveAndGoToMap.isEnabled = enabled
    }
    private fun handleSaveAndSend(action: String) {
        if (saveData()) {
            // Deshabilita los botones mientras se envían los datos
            setButtonsEnabled(false)
            sendClientData(action)
        }
    }
    private fun sendClientData(act: String = "N"){
        val apiInterface = UserApiService.create().sendUpdateClientData(person)
        apiInterface.enqueue(object : Callback<Person> {
            override fun onResponse(
                call: Call<Person>,
                response: Response<Person>
            ) {
                if (response.body() != null) {
                    person = response.body()!!

                    if(act == "S")
                        goToSale(person)
                    else if(act == "Q")
                        goToQuotation(person)
                    else if(act == "N")
                        goToMap()
                }
            }

            override fun onFailure(call: Call<Person>, t: Throwable) {
                Log.d("MIKE", "sendClientAddress. Algo salio mal..." + t.message.toString())
            }
        })

    }

    private fun goToQuotation(p: Person){

        val bundle = Bundle()

        bundle.putInt("userID", preference.getData("userID").toInt())
        bundle.putInt("vehicleID", preference.getData("vehicleID").toInt())
        bundle.putString("vehicleLicensePlate",  preference.getData("vehicleLicensePlate"))
        bundle.putInt("personID",p.personID)
        bundle.putString("personFullName",p.fullName)
        bundle.putString("personAddress",p.address)
        bundle.putString("personDocumentNumber",p.documentNumber)
        bundle.putString("personDocumentType",p.documentType)
        bundle.putString("physicalDistribution",p.physicalDistribution)
        bundle.putString("physicalDistributionDisplay",p.physicalDistributionDisplay)
        bundle.putString("routeDate",p.routeDate)

        findNavController().navigate(R.id.action_clientRegisterFragment_to_quotationFragment, bundle)

    }


    private fun goToSale(p: Person){

        val bundle = Bundle()

        bundle.putInt("userID", preference.getData("userID").toInt())
        bundle.putInt("vehicleID", preference.getData("vehicleID").toInt())
        bundle.putString("vehicleLicensePlate",  preference.getData("vehicleLicensePlate"))
        bundle.putInt("personID",p.personID)
        bundle.putString("personFullName",p.fullName)
        bundle.putString("personAddress",p.address)
        bundle.putString("personDocumentNumber",p.documentNumber)
        bundle.putString("personDocumentType",p.documentType)
        bundle.putString("physicalDistribution",p.physicalDistribution)
        bundle.putString("physicalDistributionDisplay",p.physicalDistributionDisplay)
        bundle.putString("routeDate",p.routeDate)

        findNavController().navigate(R.id.action_clientRegisterFragment_to_saleFragment, bundle)

    }
    private fun goToMap(){

        val bundle = Bundle()

        bundle.putInt("userID", preference.getData("userID").toInt())
        bundle.putInt("vehicleID", preference.getData("vehicleID").toInt())

        findNavController().navigate(R.id.action_clientRegisterFragment_to_mapFragment, bundle)

    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.mapType = GoogleMap.MAP_TYPE_NORMAL
        mMap.uiSettings.isZoomControlsEnabled = true

        mMap.setOnMapClickListener { latLng ->
            mMap.clear()
            mMap.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title(person.address).draggable(true)
            )
            textInputEditTextLatitude.setText(latLng.latitude.toString())
            textInputEditTextLongitude.setText(latLng.longitude.toString())
        }
        mMap.setOnMarkerDragListener(object : GoogleMap.OnMarkerDragListener{
            override fun onMarkerDrag(marker: Marker) {}

            override fun onMarkerDragEnd(marker: Marker) {
                var lat = marker.position.latitude
                var lon = marker.position.longitude
                textInputEditTextLatitude.setText(lat.toString())
                textInputEditTextLongitude.setText(lon.toString())
                Toast.makeText(globalContext, "ON "+"Marker " + marker.getId() + " Draggable" + marker.getPosition(), Toast.LENGTH_SHORT).show()
            }
            override fun onMarkerDragStart(marker: Marker) {}
        })

        fetchLocation()

    }

    private fun fetchLocation() {
        val task= fusedLocationProviderClient.lastLocation
        if(ActivityCompat.checkSelfPermission(globalContext!!, android.Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(globalContext!!, android.Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ){
            ActivityCompat.requestPermissions(globalContext as Activity, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), 101)
            return
        }
        task.addOnSuccessListener {
            if (it!=null){
                // Toast.makeText(globalContext, "lat ${it.latitude} ${it.longitude}", Toast.LENGTH_SHORT).show()
                person.latitude = it.latitude
                person.longitude = it.longitude
                mMap.isMyLocationEnabled = true
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(person.latitude, person.longitude), 15.0f))
                val markerMap =  MarkerOptions().position(LatLng(person.latitude, person.longitude))
                    .title("Nuevo cliente").draggable(true)
                mMap.addMarker(markerMap)

                textInputEditTextLatitude.setText(it.latitude.toString())
                textInputEditTextLongitude.setText(it.longitude.toString())

            }
        }
    }


    private fun loadDistributors(){

        val apiInterface = UserApiService.create().getGangs()
        apiInterface.enqueue(object : Callback<ArrayList<Gang>> {
            override fun onResponse(call: Call<ArrayList<Gang>>, response: Response<ArrayList<Gang>>) {
                listGangs = response.body()!!
                val gangNames = listGangs.map { it.name } // Extrae solo los nombres

                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, gangNames)
                autoCompleteGang.setAdapter(adapter)

                autoCompleteGang.setOnItemClickListener { _, _, position, _ ->
                    val selectedGang = listGangs[position]
                    autoCompleteGang.setText(selectedGang.name, false) // Establece el valor sin autocompletar
                    person.gangID = selectedGang.gangID
                    person.gangName = selectedGang.name
                }
            }

            override fun onFailure(call: Call<ArrayList<Gang>>, t: Throwable) {
                Log.d("MIKE", "fetchApiDistributors onFailure: ${t.message}")
            }
        })

    }

}