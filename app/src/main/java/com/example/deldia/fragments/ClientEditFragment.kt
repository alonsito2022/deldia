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

    private lateinit var textInputEditTextPersonName: TextInputEditText
    private lateinit var textInputEditTextPersonFirstSurname: TextInputEditText
    private lateinit var textInputEditTextPersonSecondSurname: TextInputEditText
    private lateinit var textInputLayoutFirstSurname: TextInputLayout
    private lateinit var textInputLayoutSecondSurname: TextInputLayout

    private lateinit var autoCompleteDocumentType: AutoCompleteTextView
    private lateinit var autoCompleteVisitDay: AutoCompleteTextView
    private lateinit var autoCompleteGang: AutoCompleteTextView
    private lateinit var textInputEditTextDocumentNumber: TextInputEditText
    private lateinit var btnCleanGang: Button
    private lateinit var btnSearchApi: Button

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

    private var globalContext: Context? = null
    private lateinit var preference: Preference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        globalContext = this.activity
        preference = Preference(globalContext)
        val bundle = arguments
        searchPerson.personID = bundle!!.getInt("personID")
        loadDistributors()
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(globalContext!!)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_client_edit, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        autoCompleteDocumentType = view.findViewById(R.id.autoCompleteDocumentType)
        autoCompleteVisitDay = view.findViewById(R.id.autoCompleteVisitDay)
        autoCompleteGang = view.findViewById(R.id.autoCompleteGang)
        autoCompleteCustomerType = view.findViewById(R.id.autoCompleteCustomerType)
        autoCompleteShowcases = view.findViewById(R.id.autoCompleteShowcases)
        textInputEditTextDocumentNumber = view.findViewById(R.id.textInputEditTextDocumentNumber)
        textInputEditTextPersonName = view.findViewById(R.id.textInputEditTextPersonName)
        textInputEditTextPersonFirstSurname = view.findViewById(R.id.textInputEditTextPersonFirstSurname)
        textInputEditTextPersonSecondSurname = view.findViewById(R.id.textInputEditTextPersonSecondSurname)
        textInputLayoutFirstSurname = view.findViewById(R.id.textInputLayoutFirstSurname)
        textInputLayoutSecondSurname = view.findViewById(R.id.textInputLayoutSecondSurname)
        textInputEditTextComment = view.findViewById(R.id.textInputEditTextComment)
        textInputEditTextPersonObservation = view.findViewById(R.id.textInputEditTextPersonObservation)
        textInputEditTextPersonCellPhone = view.findViewById(R.id.textInputEditTextPersonCellPhone)
        btnCleanGang = view.findViewById(R.id.btnCleanGang)
        btnSaveAndQuote = view.findViewById(R.id.btnSaveAndQuote)
        btnSaveAndSell = view.findViewById(R.id.btnSaveAndSell)
        btnSaveAndGoToMap = view.findViewById(R.id.btnSaveAndGoToMap)
        btnSearchApi = view.findViewById(R.id.btnSearchApi)

        textInputEditTextAddressName = view.findViewById(R.id.textInputEditTextAddressName)
        autoCompleteDistrict = view.findViewById(R.id.autoCompleteDistrict)
        textInputEditTextLatitude = view.findViewById(R.id.textInputEditTextLatitude)
        textInputEditTextLongitude = view.findViewById(R.id.textInputEditTextLongitude)
//        textInputEditTextLatitude.isFocusable = false
//        textInputEditTextLongitude.isFocusable = false

        switchStatus = view.findViewById(R.id.switchStatus)
        switchStatus.setOnCheckedChangeListener { _, isChecked ->
            person.isEnabled = isChecked
        }

        btnCleanGang.setOnClickListener{
            autoCompleteGang.setText("")
        }

        btnSearchApi.setOnClickListener{
            if (textInputEditTextDocumentNumber.text!!.isNotEmpty()){
                val itemSelected = autoCompleteDocumentType.text.toString()
                if (itemSelected=="DNI"){person.documentType = "01"}
                else if (itemSelected=="RUC"){person.documentType = "06"}
                person.documentNumber = textInputEditTextDocumentNumber.text.toString()
                person.isClient = true
                fetchedPerson()
            }
            else
                Toast.makeText(globalContext, "Verificar nro documento", Toast.LENGTH_SHORT).show()

        }

        val mapFragment = childFragmentManager.findFragmentById(R.id.frg) as SupportMapFragment?
        mapView = mapFragment!!.view
        mapFragment.getMapAsync(this)


    }

    private fun fetchedPerson(){
        val apiInterface = UserApiService.create().getDocumentConsultation(person)
        apiInterface.enqueue(object : Callback<Person> {
            override fun onResponse(
                call: Call<Person>,
                response: Response<Person>
            ) {
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
                Log.d("MIKE", "Algo salio mal..." + t.message.toString())
            }
        })

    }

    private fun View.closeKeyBoard(inputMethodManager: InputMethodManager) {

        inputMethodManager.hideSoftInputFromWindow(windowToken, 0)

    }
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

                    autoCompleteGang.setText(person.gangName)

                    textInputEditTextAddressName.setText(person.address)
                    textInputEditTextLatitude.setText(person.latitude.toString())
                    textInputEditTextLongitude.setText(person.longitude.toString())


                    val listDistrict = listOf(
                        "JOSE LUIS BUSTAMANTE Y RIVERO",
                        "ALTO SELVA ALEGRE",
                        "JACOBO HUNTER",
                        "MARIANO MELGAR",
                        "YURA",
                        "YARABAMBA",
                        "YANAHUARA",
                        "VITOR",
                        "UCHUMAYO",
                        "TIABAYA",
                        "SOCABAYA",
                        "STA RITA DE SIGUAS",
                        "SANTA ISABEL DE SIGUAS",
                        "SAN JUAN DE TARUCANI",
                        "SAN JUAN DE SIGUAS",
                        "SACHACA",
                        "SABANDIA",
                        "QUEQUEÑA",
                        "POLOBAYA",
                        "POCSI",
                        "PAUCARPATA",
                        "MOLLEBAYA",
                        "MIRAFLORES",
                        "LA JOYA",
                        "CHIGUATA",
                        "CHARACATO",
                        "CERRO COLORADO",
                        "CAYMA",
                        "AREQUIPA"
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

                    btnSaveAndSell.setOnClickListener {
                        saveData()
                        sendClientData("S")
                    }
                    btnSaveAndQuote.setOnClickListener {
                        saveData()
                        sendClientData("Q")
                    }
                    btnSaveAndGoToMap.setOnClickListener {
                        saveData()
                        sendClientData("N")
                    }

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

    private fun saveData(){
        val valueCustomerTypes = autoCompleteCustomerType.text.toString()
        var selectedCustomerTypes = 0
        when (valueCustomerTypes){
            "PEPSICO" -> {selectedCustomerTypes = 0}
            "PROPIO" -> {selectedCustomerTypes = 1}
        }

        val valueShowcases = autoCompleteShowcases.text.toString()
        var selectedShowcases = 0
        when (valueShowcases){
            "NO CUENTA" -> {selectedShowcases = 0}
            "AEREO" -> {selectedShowcases = 1}
            "2 NIVELES" -> {selectedShowcases = 2}
            "5 NIVELES" -> {selectedShowcases = 3}
            "6X8 NIVELES" -> {selectedShowcases = 4}
        }

        val valueVisitDay = autoCompleteVisitDay.text.toString()
        var selectedVisitDay = 0
        when (valueVisitDay){
            "LUNES" -> {selectedVisitDay = 0}
            "MARTES" -> {selectedVisitDay = 1}
            "MIERCOLES" -> {selectedVisitDay = 2}
            "JUEVES" -> {selectedVisitDay = 3}
            "VIERNES" -> {selectedVisitDay = 4}
            "SABADO" -> {selectedVisitDay = 5}
            "DOMINGO" -> {selectedVisitDay = 6}
        }

        person.documentNumber = textInputEditTextDocumentNumber.text.toString()
        person.name = textInputEditTextPersonName.text.toString()
        person.firstSurname = textInputEditTextPersonFirstSurname.text.toString()
        person.secondSurname = textInputEditTextPersonSecondSurname.text.toString()
        person.comment = textInputEditTextComment.text.toString()
        person.isClient = true

        person.visitDay = selectedVisitDay
        person.showcases = selectedShowcases
        person.customerType = selectedCustomerTypes
        person.observation =textInputEditTextPersonObservation.text.toString()
        person.cellphone =textInputEditTextPersonCellPhone.text.toString()

        if(textInputEditTextLatitude.text !== null){
            if(textInputEditTextAddressName.text !== null){
                val valueDistrict = autoCompleteDistrict.text.toString()
                var selectedDistrict = ""
                when (valueDistrict) {
                    "JOSE LUIS BUSTAMANTE Y RIVERO" -> {selectedDistrict = "040129"}
                    "ALTO SELVA ALEGRE" -> {selectedDistrict = "040128"}
                    "JACOBO HUNTER" -> {selectedDistrict = "040127"}
                    "MARIANO MELGAR" -> {selectedDistrict = "040126"}
                    "YURA" -> {selectedDistrict = "040125"}
                    "YARABAMBA" -> {selectedDistrict = "040124"}
                    "YANAHUARA" -> {selectedDistrict = "040123"}
                    "VITOR" -> {selectedDistrict = "040122"}
                    "UCHUMAYO" -> {selectedDistrict = "040121"}
                    "TIABAYA" -> {selectedDistrict = "040120"}
                    "SOCABAYA" -> {selectedDistrict = "040119"}
                    "STA RITA DE SIGUAS" -> {selectedDistrict = "040118"}
                    "SANTA ISABEL DE SIGUAS" -> {selectedDistrict = "040117"}
                    "SAN JUAN DE TARUCANI" -> {selectedDistrict = "040116"}
                    "SAN JUAN DE SIGUAS" -> {selectedDistrict = "040115"}
                    "SACHACA" -> {selectedDistrict = "040114"}
                    "SABANDIA" -> {selectedDistrict = "040113"}
                    "QUEQUEÑA" -> {selectedDistrict = "040112"}
                    "POLOBAYA" -> {selectedDistrict = "040111"}
                    "POCSI" -> {selectedDistrict = "040110"}
                    "PAUCARPATA" -> {selectedDistrict = "040109"}
                    "MOLLEBAYA" -> {selectedDistrict = "040108"}
                    "MIRAFLORES" -> {selectedDistrict = "040107"}
                    "LA JOYA" -> {selectedDistrict = "040106"}
                    "CHIGUATA" -> {selectedDistrict = "040105"}
                    "CHARACATO" -> {selectedDistrict = "040104"}
                    "CERRO COLORADO" -> {selectedDistrict = "040103"}
                    "CAYMA" -> {selectedDistrict = "040102"}
                    "AREQUIPA" -> {selectedDistrict = "040101"}
                }

                person.district = selectedDistrict
                person.address = textInputEditTextAddressName.text.toString()
                person.latitude = textInputEditTextLatitude.text.toString().toDouble()
                person.longitude = textInputEditTextLongitude.text.toString().toDouble()
            }
            else
                Toast.makeText(globalContext, "Verifique direccion", Toast.LENGTH_SHORT).show()
        }
        else
            Toast.makeText(globalContext, "Verifique posicion", Toast.LENGTH_SHORT).show()
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

        findNavController().navigate(R.id.action_clientEditFragment_to_quotationFragment, bundle)

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

        findNavController().navigate(R.id.action_clientEditFragment_to_saleFragment, bundle)

    }
    private fun goToMap(){

        val bundle = Bundle()

        bundle.putInt("userID", preference.getData("userID").toInt())
        bundle.putInt("vehicleID", preference.getData("vehicleID").toInt())

        findNavController().navigate(R.id.action_clientEditFragment_to_mapFragment, bundle)

    }


    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.mapType = GoogleMap.MAP_TYPE_NORMAL
        mMap.uiSettings.isZoomControlsEnabled = true

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
                mMap.isMyLocationEnabled = true
                /*if (mapView != null){
                    val locationButton= (mapView?.findViewById<View>(Integer.parseInt("1"))?.parent as View).findViewById<View>(Integer.parseInt("2"))
                    val rlp =  locationButton.layoutParams as RelativeLayout.LayoutParams
                    rlp.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0)
                    rlp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE)
                    rlp.setMargins(0, 0, 30, 30)
                }*/


            }
        }

    }
    private fun loadDistributors(){

        val apiInterface = UserApiService.create().getGangs()
        apiInterface.enqueue(object : Callback<java.util.ArrayList<Gang>>{
            override fun onResponse(call: Call<java.util.ArrayList<Gang>>, response: Response<java.util.ArrayList<Gang>>) {
                listGangs = response.body()!!
                autoCompleteGang.setAdapter(GangAdapter(globalContext!!, R.layout.item_gang_view, listGangs, object : GangAdapter.OnItemClickListener{
                    override fun onItemClick(model: Gang) {
                        autoCompleteGang.setText(model.name)
                        autoCompleteGang.dismissDropDown()
                        val inputManager = context!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        autoCompleteGang.closeKeyBoard(inputManager)
                        autoCompleteGang.clearFocus()
                        person.gangID = model.gangID
                        person.gangName = model.name
                    }
                }))
                loadClient(searchPerson)
            }

            override fun onFailure(call: Call<java.util.ArrayList<Gang>>, t: Throwable) {
                Log.d("MIKE", "fetchApiDistributors onFailure: " + t.message.toString())
            }

        })

    }
}