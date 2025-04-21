package com.example.deldia.fragments

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.deldia.R
import com.example.deldia.adapter.ClientAdapter
import com.example.deldia.adapter.GangAdapter
import com.example.deldia.localdatabase.Preference
import com.example.deldia.models.Gang
import com.example.deldia.models.Person
import com.example.deldia.models.RequestFilterPerson
import com.example.deldia.retrofit.UserApiService
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ClientEditFragment : Fragment(), OnMapReadyCallback {

    // UI Components
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

    // Map Related
    private lateinit var mMap: GoogleMap
    var mapView: View? = null
    lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    // Data Models
    private var person: Person = Person()
    private var searchPerson: Person = Person()
    private var listGangs = arrayListOf<Gang>()

    // Context and Preferences
    private var globalContext: Context? = null
    private lateinit var preference: Preference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        globalContext = this.activity
        preference = Preference(globalContext)
        arguments?.let { searchPerson.personID = it.getInt("personID") }
        loadDistributors()
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(globalContext!!)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_client_edit, container, false)
    }

    private fun initViews(view: View) {
        autoCompleteDocumentType = view.findViewById(R.id.autoCompleteDocumentType)
        autoCompleteVisitDay = view.findViewById(R.id.autoCompleteVisitDay)
        textInputLayoutGang = view.findViewById(R.id.textInputLayoutGang)
        autoCompleteGang = view.findViewById(R.id.autoCompleteGang)
        autoCompleteCustomerType = view.findViewById(R.id.autoCompleteCustomerType)
        autoCompleteShowcases = view.findViewById(R.id.autoCompleteShowcases)
        textInputEditTextDocumentNumber = view.findViewById(R.id.textInputEditTextDocumentNumber)
        progressBar = view.findViewById(R.id.progressBar)
        textInputLayoutDocumentNumber = view.findViewById(R.id.textInputLayoutDocumentNumber)
        textInputLayoutDocumentNumber.setEndIconDrawable(R.drawable.ic_baseline_search_24)
        textInputEditTextPersonName = view.findViewById(R.id.textInputEditTextPersonName)
        textInputEditTextPersonFirstSurname = view.findViewById(R.id.textInputEditTextPersonFirstSurname)
        textInputEditTextPersonSecondSurname = view.findViewById(R.id.textInputEditTextPersonSecondSurname)
        textInputLayoutFirstSurname = view.findViewById(R.id.textInputLayoutFirstSurname)
        textInputLayoutSecondSurname = view.findViewById(R.id.textInputLayoutSecondSurname)
        textInputEditTextComment = view.findViewById(R.id.textInputEditTextComment)
        switchStatus = view.findViewById(R.id.switchStatus)
        btnSaveAndGoToMap = view.findViewById(R.id.btnSaveAndGoToMap)
        textInputEditTextPersonObservation = view.findViewById(R.id.textInputEditTextPersonObservation)
        textInputEditTextPersonCellPhone = view.findViewById(R.id.textInputEditTextPersonCellPhone)
        textInputEditTextAddressName = view.findViewById(R.id.textInputEditTextAddressName)
        autoCompleteDistrict = view.findViewById(R.id.autoCompleteDistrict)
        textInputEditTextLatitude = view.findViewById(R.id.textInputEditTextLatitude)
        textInputEditTextLongitude = view.findViewById(R.id.textInputEditTextLongitude)
    }

    private fun clearPersonFields() {
        textInputEditTextPersonName.setText("")
        textInputEditTextPersonFirstSurname.setText("")
        textInputEditTextPersonSecondSurname.setText("")
        textInputEditTextAddressName.setText("")
    }

    private fun setupAutoCompleteTextView(adapter: ArrayAdapter<String>, autoCompleteTextView: AutoCompleteTextView) {
        autoCompleteTextView.keyListener = null
        autoCompleteTextView.setAdapter(adapter)
    }

    private fun setTextWithAdapter(autoCompleteTextView: AutoCompleteTextView, value: String) {
        autoCompleteTextView.setText(autoCompleteTextView.adapter.getItem(value.indexOf(value)).toString(), false)
    }


    private fun validateDocumentNumber(documentType: String, documentNumber: String): Boolean {
        return when (documentType) {
            "DNI" -> documentNumber.length == 8
            "RUC" -> documentNumber.length == 11
            else -> false
        }
    }


    private fun updatePersonUI(personTemp: Person) {
        textInputEditTextPersonName.setText(personTemp.name)
        textInputEditTextPersonFirstSurname.setText(personTemp.firstSurname)
        textInputEditTextPersonSecondSurname.setText(personTemp.secondSurname)
        person.apply {
            name = personTemp.name
            firstSurname = personTemp.firstSurname
            secondSurname = personTemp.secondSurname
            if (documentType == "06") {
                textInputEditTextAddressName.setText(personTemp.address)
                address = personTemp.address
            }
        }
    }

    private fun fetchPersonData() {
        UserApiService.create().getDocumentConsultation(person).enqueue(object : Callback<Person> {
            override fun onResponse(call: Call<Person>, response: Response<Person>) {
                response.body()?.let { personTemp ->
                    updatePersonUI(personTemp)
                }
            }
            override fun onFailure(call: Call<Person>, t: Throwable) {
                Log.d("MIKE", "Algo salió mal... ${t.message}")
            }
        })
    }

    private fun setupListeners() {
        textInputLayoutDocumentNumber.setEndIconOnClickListener {
            val itemSelected = autoCompleteDocumentType.text.toString()
            val documentNumber = textInputEditTextDocumentNumber.text.toString()
            if (validateDocumentNumber(itemSelected, documentNumber)) {
                person.documentType = when (itemSelected) {
                    "DNI" -> "01"
                    "RUC" -> "06"
                    else -> return@setEndIconOnClickListener
                }
                person.documentNumber = documentNumber
                person.isClient = true
                fetchPersonData()
            } else {
                Toast.makeText(requireContext(), "Número de documento inválido", Toast.LENGTH_SHORT).show()
                clearPersonFields()
            }
        }

        autoCompleteGang.setOnClickListener {
            autoCompleteGang.showDropDown()
        }

        switchStatus.setOnCheckedChangeListener { _, isChecked ->
            person.isEnabled = isChecked
            switchStatus.text = if (isChecked) "ACTIVO" else "INACTIVO"
        }

        btnSaveAndGoToMap.setOnClickListener { handleSaveAndSend("N") }
    }

    private fun setupMap() {
        val mapFragment = childFragmentManager.findFragmentById(R.id.frg) as SupportMapFragment?
        mapView = mapFragment!!.view
        mapFragment.getMapAsync(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        setupListeners()
        setupMap()
    }

    /**
     * Carga los distribuidores desde la API y actualiza la lista de gangs.
     */
    private fun loadDistributors() {
        UserApiService.create().getGangs().enqueue(object : Callback<java.util.ArrayList<Gang>> {
            override fun onResponse(call: Call<java.util.ArrayList<Gang>>, response: Response<java.util.ArrayList<Gang>>) {
                listGangs = response.body()!!
                val gangNames = listGangs.map { it.name }
                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, gangNames)
                autoCompleteGang.setAdapter(adapter)
                autoCompleteGang.setOnItemClickListener { _, _, position, _ ->
                    val selectedGang = listGangs[position]
                    autoCompleteGang.setText(selectedGang.name, false)
                    person.gangID = selectedGang.gangID
                    person.gangName = selectedGang.name
                }
                loadClient(searchPerson)
            }
            override fun onFailure(call: Call<java.util.ArrayList<Gang>>, t: Throwable) {
                Log.d("MIKE", "fetchApiDistributors onFailure: ${t.message}")
            }
        })
    }

//    private fun fetchedPerson(){
//        val apiInterface = UserApiService.create().getDocumentConsultation(person)
//        apiInterface.enqueue(object : Callback<Person> {
//            override fun onResponse(
//                call: Call<Person>,
//                response: Response<Person>
//            ) {
//                if (response.body() != null) {
//                    val personTemp = response.body()!!
//                    textInputEditTextPersonName.setText(personTemp.name)
//                    textInputEditTextPersonFirstSurname.setText(personTemp.firstSurname)
//                    textInputEditTextPersonSecondSurname.setText(personTemp.secondSurname)
//
//                    person.name = personTemp.name
//                    person.firstSurname = personTemp.firstSurname
//                    person.secondSurname = personTemp.secondSurname
//
//                    if (person.documentType == "06") {
//                        textInputEditTextAddressName.setText(personTemp.address)
//                        person.address = personTemp.address
//                    }
//
//                }
//            }
//
//            override fun onFailure(call: Call<Person>, t: Throwable) {
//                Log.d("MIKE", "Algo salio mal..." + t.message.toString())
//            }
//        })
//
//    }

    private fun loadClient(p: Person) {
        val apiInterface = UserApiService.create().getPersonByID(p)
        apiInterface.enqueue(object : Callback<Person> {
            override fun onResponse(call: Call<Person>, response: Response<Person>) {
                if (response.body() != null) {
                    person = response.body()!!
                    switchStatus.isChecked = person.isEnabled
                    textInputEditTextPersonName.setText(person.name)
                    textInputEditTextPersonFirstSurname.setText(person.firstSurname)
                    textInputEditTextPersonSecondSurname.setText(person.secondSurname)
                    textInputEditTextDocumentNumber.setText(person.documentNumber)
                    textInputEditTextComment.setText(person.comment)
                    textInputEditTextPersonObservation.setText(person.observation)
                    textInputEditTextPersonCellPhone.setText(person.cellphone)
                    val listCustomerTypes = listOf("PEPSICO", "PROPIO")
                    val adapterCustomerTypes = ArrayAdapter(
                        globalContext!!,
                        android.R.layout.simple_spinner_dropdown_item,
                        listCustomerTypes
                    )
                    autoCompleteCustomerType.keyListener = null
                    autoCompleteCustomerType.setAdapter(adapterCustomerTypes)
                    autoCompleteCustomerType.setText(
                        autoCompleteCustomerType.adapter.getItem(listCustomerTypes.indexOf(person.customerTypeDisplay)).toString(),
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
                        autoCompleteShowcases.adapter.getItem(listShowcases.indexOf(person.showcasesDisplay)).toString(),
                        false
                    )

                    val listDay = listOf("LUNES", "MARTES", "MIERCOLES", "JUEVES", "VIERNES", "SABADO", "DOMINGO")
                    val adapterDay = ArrayAdapter(
                        globalContext!!,
                        android.R.layout.simple_spinner_dropdown_item,
                        listDay
                    )
                    autoCompleteVisitDay.keyListener = null
                    autoCompleteVisitDay.setAdapter(adapterDay)
                    autoCompleteVisitDay.setText(
                        autoCompleteVisitDay.adapter.getItem(listDay.indexOf(person.visitDayDisplay)).toString(),
                        false
                    )

                    val valueDocumentType = person.documentType
                    var selectedDocumentType = 0
                    when (valueDocumentType){
                        "01" -> {selectedDocumentType = 0}
                        "06" -> {selectedDocumentType = 1}
                    }

                    val listMethod = listOf("DNI", "RUC")

                    val adapterDocumentType = ArrayAdapter(
                        globalContext!!,
                        android.R.layout.simple_spinner_dropdown_item,
                        listMethod
                    )

                    autoCompleteDocumentType.keyListener = null
                    autoCompleteDocumentType.setAdapter(adapterDocumentType)
                    autoCompleteDocumentType.setText(
                        autoCompleteDocumentType.adapter.getItem(selectedDocumentType).toString(),
                        false
                    )

                    autoCompleteDocumentType.setOnItemClickListener { adapterView, view, position, l ->

                        val itemSelected = adapterDocumentType.getItem(position).toString()
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

                    autoCompleteGang.setText(person.gangName, false)

                    textInputEditTextAddressName.setText(person.address)
                    textInputEditTextLatitude.setText(person.latitude.toString())
                    textInputEditTextLongitude.setText(person.longitude.toString())


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
                    if(person.district.isEmpty()){
                        autoCompleteDistrict.setText(
                            autoCompleteDistrict.adapter.getItem(0).toString(),
                            false
                        )
                    }else{
                        autoCompleteDistrict.setText(
                            autoCompleteDistrict.adapter.getItem(listDistrict.indexOf(person.districtDisplay)).toString(),
                            false
                        )
                    }

                    btnSaveAndGoToMap.setOnClickListener { handleSaveAndSend("N") }

                    val markerMap =  MarkerOptions().position(LatLng(person.latitude, person.longitude))
                        .title(person.fullName).draggable(true)
                    mMap.addMarker(markerMap)
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

                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(person.latitude, person.longitude), 15.0f))
                }
            }

            override fun onFailure(call: Call<Person>, t: Throwable) {
                Log.d("MIKE", "loadClient. Algo salio mal..." + t.message.toString())
            }
        })
    }

    private fun getSelectedShowcases(): Int {
        val valueShowcases = autoCompleteShowcases.text.toString()
        return when (valueShowcases) {
            "NO CUENTA" -> 0
            "AEREO" -> 1
            "2 NIVELES" -> 2
            "5 NIVELES" -> 3
            "6X8 NIVELES" -> 4
            else -> -1
        }
    }

    private fun getSelectedCustomerType(): Int {
        val valueCustomerTypes = autoCompleteCustomerType.text.toString()
        return when (valueCustomerTypes) {
            "PEPSICO" -> 0
            "PROPIO" -> 1
            else -> -1
        }
    }

    private fun getSelectedDistrict(): String {
        val valueDistrict = autoCompleteDistrict.text.toString()
        return when (valueDistrict) {
            "JOSE LUIS BUSTAMANTE Y RIVERO" -> "040129"
            "SOCABAYA" -> "040119"
            "SABANDIA" -> "040113"
            "CHARACATO" -> "040104"
            "QUEQUEÑA" -> "040112"
            "YARABAMBA" -> "040124"
            "MOLLEBAYA" -> "040108"
            else -> ""
        }
    }
    private fun getSelectedVisitDay(): Int {
        val selectedDay = autoCompleteVisitDay.text.toString()
        return when (selectedDay) {
            "LUNES" -> 0
            "MARTES" -> 1
            "MIERCOLES" -> 2
            "JUEVES" -> 3
            "VIERNES" -> 4
            "SABADO" -> 5
            "DOMINGO" -> 6
            else -> -1 // Valor por defecto o error
        }
    }

    private fun validateRequiredFields(): Boolean {
        if (person.gangID == 0) {
            Toast.makeText(globalContext, "Verifique RUTA", Toast.LENGTH_SHORT).show()
            return false
        }
        val docNumber = textInputEditTextDocumentNumber.text.toString()
        if (person.documentType == "01" && docNumber.length != 8) {  // DNI
            Toast.makeText(globalContext, "Verifique DNI", Toast.LENGTH_SHORT).show()
            return false
        }
        if (person.documentType == "06" && docNumber.length != 11) {  // RUC
            Toast.makeText(globalContext, "Verifique RUC", Toast.LENGTH_SHORT).show()
            return false
        }
        if (textInputEditTextPersonName.text.isNullOrEmpty()) {
            Toast.makeText(globalContext, "Verifique nombre", Toast.LENGTH_SHORT).show()
            return false
        }
        if (textInputEditTextLatitude.text.isNullOrEmpty()) {
            Toast.makeText(globalContext, "Verifique posición", Toast.LENGTH_SHORT).show()
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
        return true
    }

//    private fun saveData(): Boolean {
//
//        if (person.gangID == 0) {  // RUTA
//            Toast.makeText(globalContext, "Verifique RUTA", Toast.LENGTH_SHORT).show()
//            return false
//        }
//
//        val valueCustomerTypes = autoCompleteCustomerType.text.toString()
//        val selectedCustomerTypes = when (valueCustomerTypes) {
//            "PEPSICO" -> 0
//            "PROPIO" -> 1
//            else -> -1
//        }
//
//        val valueShowcases = autoCompleteShowcases.text.toString()
//        val selectedShowcases = when (valueShowcases) {
//            "NO CUENTA" -> 0
//            "AEREO" -> 1
//            "2 NIVELES" -> 2
//            "5 NIVELES" -> 3
//            "6X8 NIVELES" -> 4
//            else -> -1
//        }
//
//        val valueVisitDay = autoCompleteVisitDay.text.toString()
//        val selectedVisitDay = when (valueVisitDay) {
//            "LUNES" -> 0
//            "MARTES" -> 1
//            "MIERCOLES" -> 2
//            "JUEVES" -> 3
//            "VIERNES" -> 4
//            "SABADO" -> 5
//            "DOMINGO" -> 6
//            else -> -1
//        }
//
//        // Validaciones de DNI y RUC antes de asignar
//        val docNumber = textInputEditTextDocumentNumber.text.toString()
//        if (person.documentType == "01" && docNumber.length != 8) {  // DNI
//            Toast.makeText(globalContext, "Verifique DNI", Toast.LENGTH_SHORT).show()
//            return false
//        }
//        if (person.documentType == "06" && docNumber.length != 11) {  // RUC
//            Toast.makeText(globalContext, "Verifique RUC", Toast.LENGTH_SHORT).show()
//            return false
//        }
//
//        // Validación de ubicación antes de asignar
//        val latitudeText = textInputEditTextLatitude.text.toString()
//        if (latitudeText.isEmpty()) {
//            Toast.makeText(globalContext, "Verifique posición", Toast.LENGTH_SHORT).show()
//            return false
//        }
//
//        val nameText = textInputEditTextPersonName.text.toString()
//        if (nameText.isEmpty()) {
//            Toast.makeText(globalContext, "Verifique nombre", Toast.LENGTH_SHORT).show()
//            return false
//        }
//
//        val addressText = textInputEditTextAddressName.text.toString()
//        if (addressText.isEmpty() && person.documentType == "06") {
//            Toast.makeText(globalContext, "Verifique dirección", Toast.LENGTH_SHORT).show()
//            return false
//        }
//
//        person.documentNumber = docNumber
//        person.name = textInputEditTextPersonName.text.toString()
//        person.firstSurname = textInputEditTextPersonFirstSurname.text.toString()
//        person.secondSurname = textInputEditTextPersonSecondSurname.text.toString()
//        person.comment = textInputEditTextComment.text.toString()
//        person.isClient = true
//
//        person.visitDay = selectedVisitDay
//        person.showcases = selectedShowcases
//        person.customerType = selectedCustomerTypes
//        person.observation =textInputEditTextPersonObservation.text.toString()
//        person.cellphone =textInputEditTextPersonCellPhone.text.toString()
//
//        val valueDistrict = autoCompleteDistrict.text.toString()
//        val selectedDistrict = when (valueDistrict) {
//            "JOSE LUIS BUSTAMANTE Y RIVERO" -> "040129"
//            "SOCABAYA" -> "040119"
//            "SABANDIA" -> "040113"
//            "CHARACATO" -> "040104"
//            "QUEQUEÑA" -> "040112"
//            "YARABAMBA" -> "040124"
//            "MOLLEBAYA" -> "040108"
//            else -> ""
//        }
//
//        person.district = selectedDistrict
//        person.address = addressText
//        person.latitude = latitudeText.toDouble()
//        person.longitude = textInputEditTextLongitude.text.toString().toDouble()
//
//        return true
//    }

    private fun savePersonData(): Boolean {
        if (!validateRequiredFields()) return false

        person.apply {
            documentNumber = textInputEditTextDocumentNumber.text.toString()
            name = textInputEditTextPersonName.text.toString()
            firstSurname = textInputEditTextPersonFirstSurname.text.toString()
            secondSurname = textInputEditTextPersonSecondSurname.text.toString()
            comment = textInputEditTextComment.text.toString()
            isClient = true
            visitDay = getSelectedVisitDay()
            showcases = getSelectedShowcases()
            customerType = getSelectedCustomerType()
            observation = textInputEditTextPersonObservation.text.toString()
            cellphone = textInputEditTextPersonCellPhone.text.toString()
            district = getSelectedDistrict()
            address = textInputEditTextAddressName.text.toString()
            latitude = textInputEditTextLatitude.text.toString().toDouble()
            longitude = textInputEditTextLongitude.text.toString().toDouble()
        }
        return true
    }

    private fun setButtonsEnabled(enabled: Boolean) {
        btnSaveAndGoToMap.isEnabled = enabled
    }

    private fun handleSaveAndSend(action: String) {
        if (savePersonData()) {
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

                   if(act == "N")
                        goToMap()
                }
            }

            override fun onFailure(call: Call<Person>, t: Throwable) {
                Log.d("MIKE", "sendClientAddress. Algo salio mal..." + t.message.toString())
            }
        })

    }

    private fun goToMap(){

        val bundle = Bundle()

        bundle.putInt("userID", preference.getData("userID").toInt())
        bundle.putInt("vehicleID", preference.getData("vehicleID").toInt())

        findNavController().navigate(R.id.action_clientEditFragment_to_mapFragment, bundle)

    }

    private fun fetchLocation() {
        val task = fusedLocationProviderClient.lastLocation
        if (ActivityCompat.checkSelfPermission(globalContext!!, android.Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(globalContext!!, android.Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(globalContext as Activity, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), 101)
            return
        }
        task.addOnSuccessListener {
            if (it != null) {
                mMap.isMyLocationEnabled = true
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.mapType = GoogleMap.MAP_TYPE_NORMAL
        mMap.uiSettings.isZoomControlsEnabled = true
        fetchLocation()
    }


}