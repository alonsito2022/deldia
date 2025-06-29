package com.sys4soft.deldia.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sys4soft.deldia.adapter.GangAdapter
import com.sys4soft.deldia.adapter.PersonAdapter
import com.sys4soft.deldia.localdatabase.Preference
import com.sys4soft.deldia.models.*
import com.sys4soft.deldia.retrofit.UserApiService
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
//import io.reactivex.rxjava3.core.Observable
//import io.reactivex.rxjava3.core.SingleObserver
//import io.reactivex.rxjava3.disposables.Disposable
//import io.reactivex.rxjava3.schedulers.Schedulers
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*
import com.sys4soft.deldia.R

class MapFragment : Fragment(), OnMapReadyCallback {
    private lateinit var fabNewStopping: FloatingActionButton
    private lateinit var fabToggleSearching: FloatingActionButton
    private lateinit var cardViewSearch: CardView
    private lateinit var loadingLayout: FrameLayout

    private var globalContext: Context? = null
    private var vehicleID: Int = 0
    private var personList = arrayListOf<Person>()
    private var route: Route = Route()
    private var user: User = User()
    private var operation: Operation = Operation()

    private var listDistributors = arrayListOf<User>()
    private var listGangs = arrayListOf<Gang>()
    private var locationGPS = arrayListOf<LocationGps>()
    private var listOperations = arrayListOf<Operation>()

    private lateinit var textInputLayoutGang: TextInputLayout
    private lateinit var autoCompleteVisitDay: AutoCompleteTextView
    private lateinit var autoCompleteGang: AutoCompleteTextView
    private lateinit var btnCleanGang: Button
    private lateinit var textViewNumberVisits: TextView
    private lateinit var btnExpand: Button
    private lateinit var cardViewResult: CardView
    private lateinit var editTextSearchDate: TextInputEditText
    private lateinit var recyclerViewPerson: RecyclerView
    private lateinit var searchViewRoute: android.widget.SearchView
    private lateinit var routeAdapter: PersonAdapter
    private lateinit var layoutSalesList: LinearLayout
    private lateinit var preference: Preference

    private lateinit var mMap: GoogleMap
    var mapView: View? = null
    lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    private val allMarkersMap: MutableMap<Marker, Person> = mutableMapOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        globalContext = this.activity
        preference = Preference(globalContext)
        val bundle = arguments
        vehicleID = bundle!!.getInt("vehicleID")
        user.userID= bundle.getInt("userID")
        user.gang.gangID = preference.getData("gangID").toInt()

        route.gangID = preference.getData("gangID").toInt()

        loadDistributors()

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(globalContext!!)
//        fetchLocation()


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
                if (mapView != null){
                    val locationButton= (mapView?.findViewById<View>(Integer.parseInt("1"))?.parent as View).findViewById<View>(Integer.parseInt("2"))
                    val rlp =  locationButton.layoutParams as RelativeLayout.LayoutParams
                    rlp.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0)
                    rlp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE)
                    rlp.setMargins(0, 0, 30, 30)
                }


            }
        }

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        searchViewRoute = view.findViewById(R.id.searchViewRoute)
        searchViewRoute.setOnQueryTextListener(object : SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                val filterList: ArrayList<Person> = ArrayList()

                newText?.let {
                    personList.forEachIndexed{ _, model ->
                        if(model.fullName.lowercase().contains(newText.lowercase())){
                            filterList.add(model)
                        }
                    }
                }
                if(filterList.isEmpty()){
                    Toast.makeText(globalContext, "No data found", Toast.LENGTH_LONG).show()
                }else{
                    routeAdapter.getFilter(filterList)
                }
                return true
            }
        })
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val rootView = inflater.inflate(R.layout.fragment_map, container, false)

        cardViewSearch = rootView.findViewById(R.id.cardViewSearch)

        fabToggleSearching = rootView.findViewById(R.id.fabToggleSearching)
        fabToggleSearching.setOnClickListener {
            if(cardViewSearch.visibility == View.GONE){
                cardViewSearch.visibility = View.VISIBLE
            }else{
                cardViewSearch.visibility = View.GONE
            }

        }

        fabNewStopping = rootView.findViewById(R.id.fabNewStopping)
        fabNewStopping.setOnClickListener {
//            Toast.makeText(globalContext, "Nueva parada", Toast.LENGTH_LONG).show()
            val bundle = Bundle()

            bundle.putInt("userID", user.userID)
            bundle.putInt("vehicleID", vehicleID)
            findNavController().navigate(R.id.action_mapFragment_to_clientRegisterFragment, bundle)

        }


        textInputLayoutGang = rootView.findViewById(R.id.textInputLayoutGang)
//        if (user.userID == 2){
//            textInputLayoutGang.visibility = View.VISIBLE
//        }else{
//            textInputLayoutGang.visibility = View.GONE
//        }


        autoCompleteVisitDay = rootView.findViewById(R.id.autoCompleteVisitDay)
        autoCompleteVisitDay.isEnabled = false
        autoCompleteGang = rootView.findViewById(R.id.autoCompleteGang)
        btnExpand = rootView.findViewById(R.id.btnExpand)
        cardViewResult = rootView.findViewById(R.id.cardViewResult)
        btnExpand.setOnClickListener {
            if(cardViewResult.visibility == View.GONE){
                TransitionManager.beginDelayedTransition(cardViewResult, AutoTransition())
                cardViewResult.visibility = View.VISIBLE
                btnExpand.text = "OCULTAR"
            }else{
                TransitionManager.beginDelayedTransition(cardViewResult, AutoTransition())
                cardViewResult.visibility = View.GONE
                btnExpand.text = "ENLISTAR"
            }
        }
        btnCleanGang = rootView.findViewById(R.id.btnCleanGang)
        textViewNumberVisits = rootView.findViewById(R.id.textViewNumberVisits)
        recyclerViewPerson = rootView.findViewById(R.id.recyclerViewPerson)
        recyclerViewPerson.layoutManager = LinearLayoutManager(globalContext)
        recyclerViewPerson.setHasFixedSize(true)
        routeAdapter = PersonAdapter(personList, object : PersonAdapter.OnItemClickListener {
            override fun onItemClick(model: Person) {
                operation.clientID = model.personID
                when(model.routeStatus){
                    "01"-> {
                        showDialogPerson(model)
                        reviewOrderHistoryByClient()
                    }
                    "02"-> {
                        showDialogPerson(model)
                        reviewOrderHistoryByClient()
                    }
                    "03"-> {
                        showDialogPerson(model)
                        reviewOrderHistoryByClient()
                    }
                    "04"-> {
                        showDialogPerson(model)
                        reviewOrderHistoryByClient()
                    }
                    "05"-> {
                        showDialogPerson(model)
                        reviewOrderHistoryByClient()
                    }
                    "06"-> {
                        showDialogPerson(model)
                        reviewOrderHistoryByClient()
                    }
                    "07"-> {
                        showDialogPerson(model)
                        reviewOrderHistoryByClient()
                    }
                    "08"-> {
                        showDialogPerson(model)
                        reviewOrderHistoryByClient()
                    }
                }
            }
        })
        recyclerViewPerson.adapter = routeAdapter
        autoCompleteGang.setText(preference.getData("gangName"))

        editTextSearchDate = rootView.findViewById(R.id.editTextSearchDate)
        val sdf2 = SimpleDateFormat("dd/MM/yyyy").format(Date())
        val sdf3 = SimpleDateFormat("yyyy-MM-dd").format(Date())
        route.routeDate = sdf3
        editTextSearchDate.setText(sdf2)
        editTextSearchDate.setOnClickListener { showDatePickerDialog() }

        loadingLayout = rootView.findViewById(R.id.loadingLayout)

        btnCleanGang.setOnClickListener{
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
                "TODOS" -> {selectedVisitDay = 7}
            }
            route.visitDay = selectedVisitDay

            // Ocultar cardViewSearch
            cardViewSearch.visibility = View.GONE
            // Mostrar loader y enviar ruta
            showLoading()
            sendRoute()
        }

        val listDay = listOf("LUNES", "MARTES", "MIERCOLES", "JUEVES", "VIERNES", "SABADO", "DOMINGO", "TODOS")
        val adapterDay = ArrayAdapter(
            globalContext!!,
            android.R.layout.simple_spinner_dropdown_item,
            listDay
        )

        val week = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        var indexDay = 0
        when (week){
            2 -> {indexDay = 0}
            3 -> {indexDay = 1}
            4 -> {indexDay = 2}
            5 -> {indexDay = 3}
            6 -> {indexDay = 4}
            7 -> {indexDay = 5}
            1 -> {indexDay = 6}
        }
        autoCompleteVisitDay.keyListener = null
        autoCompleteVisitDay.setAdapter(adapterDay)
        autoCompleteVisitDay.setText(
            autoCompleteVisitDay.adapter.getItem(indexDay).toString(),
            false
        )

        val mapFragment = childFragmentManager.findFragmentById(R.id.frg) as SupportMapFragment?
        //use SupportMapFragment for using in fragment instead of activity  MapFragment = activity   SupportMapFragment = fragment
        mapView = mapFragment!!.view
        mapFragment.getMapAsync(this)


        return rootView
    }

    private fun showDatePickerDialog(){
        val fm: FragmentManager = (activity as AppCompatActivity?)!!.supportFragmentManager
        val datePicker = DatePickerFragment {day, month, year -> onDateSelected(day, month, year) }
        datePicker.show(fm, "datePicker")
    }

    @SuppressLint("SimpleDateFormat")
    private fun onDateSelected(day:Int, month:Int, year:Int){
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.YEAR, year)
        calendar.set(Calendar.MONTH, month)
        calendar.set(Calendar.DAY_OF_MONTH, day)
        val sdf2 = SimpleDateFormat("dd/MM/yyyy").format(calendar.time)
        val sdf3 = SimpleDateFormat("yyyy-MM-dd").format(calendar.time)

        var indexDay = 0
        when (calendar.get(Calendar.DAY_OF_WEEK)){
            1 -> {indexDay = 6}
            2 -> {indexDay = 0}
            3 -> {indexDay = 1}
            4 -> {indexDay = 2}
            5 -> {indexDay = 3}
            6 -> {indexDay = 4}
            7 -> {indexDay = 5}
        }
        autoCompleteVisitDay.setText(
            autoCompleteVisitDay.adapter.getItem(indexDay).toString(),
            false
        )
        route.visitDay = indexDay
        route.routeDate = sdf3
        editTextSearchDate.setText(sdf2)
    }

    private fun showLoading() {
        loadingLayout.visibility = View.VISIBLE
    }

    private fun hideLoading() {
        loadingLayout.visibility = View.GONE
    }

    private fun sendRoute(){
        val apiInterface = UserApiService.create().getRoute(route)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ responseData ->
                hideLoading() // Ocultar loader cuando la respuesta llegue
                Log.d("Mike", responseData?.size.toString())
                personList.clear()
                mMap.clear()
                allMarkersMap.clear()
                if(responseData != null && responseData.isNotEmpty()){
                    personList = responseData
                    routeAdapter.getFilter(personList)
                    textViewNumberVisits.text = "Se encontraron ${personList.size} resultados"
                    for (i in 0 until personList.size){
                        val person = personList[i]
                        val inflater = LayoutInflater.from(globalContext)
                        val viewMarker = inflater.inflate(R.layout.custom_marker_layout, null)
                        val textViewNum = viewMarker.findViewById<TextView>(R.id.textViewNum)
                        val imageViewMarker = viewMarker.findViewById<ImageView>(R.id.imageViewMarker)
                        val imageViewIcon = viewMarker.findViewById<ImageView>(R.id.imageViewIcon)

                        when(person.routeStatus){
                            "01"-> {
                                if (!person.isEnabled) {
                                    imageViewMarker.setImageResource(R.drawable.ic_marker_gray)
                                    textViewNum.setTextColor(ColorStateList.valueOf(ContextCompat.getColor(globalContext!!,R.color.black)))
                                }
                                else{
//                                    if(person.customerType == 0) {
                                    imageViewMarker.setImageResource(R.drawable.ic_marker_blue)

//                                    }
//                                    else {
//                                        imageViewMarker.setImageResource(R.drawable.ic_marker_orange)
//                                        textViewNum.setTextColor(ColorStateList.valueOf(ContextCompat.getColor(globalContext!!,R.color.black)))
//                                    }

                                }
                                imageViewIcon.visibility = View.GONE
                            }
                            "02"-> {
                                imageViewMarker.setImageResource(R.drawable.ic_marker_green)
//                                imageViewIcon.setImageResource(R.drawable.ic_baseline_lock_24)
                            }
                            "03"-> {
                                imageViewMarker.setImageResource(R.drawable.ic_marker_red)
//                                imageViewIcon.setImageResource(R.drawable.ic_baseline_lock_24)
                            }
                            "04"-> {
                                imageViewMarker.setImageResource(R.drawable.ic_marker_yellow)
//                                imageViewIcon.setImageResource(R.drawable.ic_baseline_watch_later_24)
                            }
                            "05"-> {
                                imageViewMarker.setImageResource(R.drawable.ic_marker_sky_blue)
//                                imageViewIcon.setImageResource(R.drawable.ic_baseline_watch_later_24)

                            }
                            "06"-> {
                                imageViewMarker.setImageResource(R.drawable.ic_marker_dark_green)
//                                imageViewIcon.setImageResource(R.drawable.ic_baseline_watch_later_24)
                            }
                            "07"-> {
                                imageViewMarker.setImageResource(R.drawable.ic_marker_fuchsia)
//                                imageViewIcon.setImageResource(R.drawable.ic_baseline_watch_later_24)
                            }
                            "08"-> {
                                imageViewMarker.setImageResource(R.drawable.ic_marker_black)
//                                imageViewIcon.setImageResource(R.drawable.ic_baseline_watch_later_24)
                            }
                        }

                        if (person.totalBalance > 0){
                            imageViewIcon.setImageResource(R.drawable.sol)
                            imageViewIcon.visibility = View.VISIBLE

                        }else{
                            imageViewIcon.visibility = View.GONE
                        }
                        textViewNum.text = person.observation

                        val markerMap =  MarkerOptions().position(LatLng(person.latitude, person.longitude))
                            .title(person.fullName)
                            .icon(createDrawableFromView(globalContext!!, viewMarker))

                        val miMarker = mMap.addMarker(markerMap)
                        allMarkersMap[miMarker!!] = person
                        mMap.setOnMarkerClickListener { marker ->showAlert(marker)}
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(person.latitude, person.longitude), 15.0f))
                    }
                }
            }, { error ->
                hideLoading() // Ocultar loader en caso de error
                Log.d("Mike", error.toString())
                Toast.makeText(globalContext, "Error al cargar la ruta: ${error.message}", Toast.LENGTH_LONG).show()
            })
    }

    private fun bitmapDescriptorFromVector(context: Context, vectorResId: Int, color: Int): BitmapDescriptor {
        val vectorDrawable = ContextCompat.getDrawable(context, vectorResId)
        vectorDrawable!!.setBounds(0, 0, vectorDrawable.intrinsicWidth, vectorDrawable.intrinsicHeight)
        val bitmap = Bitmap.createBitmap(vectorDrawable.intrinsicWidth, vectorDrawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
//        vectorDrawable.setBounds(0, 0, canvas.width, canvas.height)
        DrawableCompat.setTint(vectorDrawable, color)
        vectorDrawable.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    private fun createDrawableFromView(context: Context, markerView: View): BitmapDescriptor{
        val displayMetrics = DisplayMetrics()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            val display = (context as Activity).display
            display?.getRealMetrics(displayMetrics)
        } else {
            @Suppress("DEPRECATION")
            val display = (context as Activity).windowManager.defaultDisplay
            @Suppress("DEPRECATION")
            display.getMetrics(displayMetrics)
        }

        markerView.measure(displayMetrics.widthPixels, displayMetrics.heightPixels)
        markerView.layout(0, 0, displayMetrics.widthPixels, displayMetrics.heightPixels)
        markerView.buildDrawingCache()
        val bitmap = Bitmap.createBitmap(
            markerView.measuredWidth,
            markerView.measuredHeight, Bitmap.Config.ARGB_8888
        )

        val canvas = Canvas(bitmap)
        markerView.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    private fun loadDistributors(){
        val gangAll = Gang()
        gangAll.gangID = 0
        gangAll.name = "TODOS"

        val apiInterface = UserApiService.create().getGangs()
        apiInterface.enqueue(object : Callback<ArrayList<Gang>> {
            override fun onResponse(call: Call<ArrayList<Gang>>, response: Response<ArrayList<Gang>>) {

                listGangs = response.body()!!
                listGangs.add(gangAll)
                autoCompleteGang.setAdapter(GangAdapter(globalContext!!, R.layout.item_gang_view, listGangs, object : GangAdapter.OnItemClickListener{
                    override fun onItemClick(model: Gang) {
                        autoCompleteGang.setText(model.name, false)
                        autoCompleteGang.dismissDropDown()
                        val inputManager = context!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        autoCompleteGang.closeKeyBoard(inputManager)
                        autoCompleteGang.clearFocus()
                        route.gangID = model.gangID
                    }
                }))
                // Asegúrate de que el dropdown se vuelve a abrir cuando el AutoCompleteTextView recupera el foco
                autoCompleteGang.setOnFocusChangeListener { _, hasFocus ->
                    if (hasFocus) {
                        autoCompleteGang.showDropDown()
                    }
                }
                Log.d("MIKE", "loadDistributors ok: " + listDistributors.size)
            }

            override fun onFailure(call: Call<ArrayList<Gang>>, t: Throwable) {
                Log.d("MIKE", "loadDistributors onFailure: " + t.message.toString())
            }
        })

    }

    private fun View.closeKeyBoard(inputMethodManager: InputMethodManager) {
        inputMethodManager.hideSoftInputFromWindow(windowToken, 0)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.mapType = GoogleMap.MAP_TYPE_NORMAL

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(-16.418401, -71.539085), 15f))
        fetchLocation()
    }

    private fun showAlert(marker: Marker): Boolean {

        val p = allMarkersMap[marker]
        showDialogPerson(p!!)
        operation.clientID = p.personID
        reviewOrderHistoryByClient()
        return true
    }

    private fun showDialogPerson(p: Person){
        val inflater = LayoutInflater.from(globalContext)
        val v = inflater.inflate(R.layout.dialog_person_marker, null)
        val buttonCallPhone = v.findViewById<Button>(R.id.buttonCallPhone)
        val dialogClose = v.findViewById<Button>(R.id.dialog_close)
        val dialogCancelPresale = v.findViewById<Button>(R.id.dialog_cancel_presale)
        val dialogCancelSale = v.findViewById<Button>(R.id.dialog_cancel_sale)
        val dialogPresaleDelivered = v.findViewById<Button>(R.id.dialog_presale_delivered)
        val dialogPresaleNoDelivered = v.findViewById<Button>(R.id.dialog_presale_no_delivered)
        val dialogEditClient = v.findViewById<Button>(R.id.dialog_edit_client)
        val dialogWithoutDispatch = v.findViewById<Button>(R.id.dialog_without_dispatch)
        val dialogSaveClientData = v.findViewById<Button>(R.id.dialog_save_client_data)
        val dialogNewSale = v.findViewById<Button>(R.id.dialog_new_sale)
        val dialogNewDispatch = v.findViewById<Button>(R.id.dialog_new_dispatch)
        val dialogPutInPending = v.findViewById<Button>(R.id.dialog_put_in_pending)
        val dialogGenerateCreditNote = v.findViewById<Button>(R.id.dialog_generate_credit_note)
        val dialogDeclined = v.findViewById<Button>(R.id.dialog_declined)
        val editTextObs = v.findViewById<EditText>(R.id.editTextObs)
        val editTextAddress = v.findViewById<EditText>(R.id.editTextAddress)
        val textViewPersonFullName = v.findViewById<TextView>(R.id.textViewPersonFullName)
        val textViewLatitude = v.findViewById<TextView>(R.id.textViewLatitude)
        val textViewLongitude = v.findViewById<TextView>(R.id.textViewLongitude)
        val textViewDocumentTypeDisplay = v.findViewById<TextView>(R.id.textViewDocumentTypeDisplay)
        val textViewDocumentNumber = v.findViewById<TextView>(R.id.textViewDocumentNumber)
        val textViewAddress = v.findViewById<TextView>(R.id.textViewAddress)
        val textViewUser = v.findViewById<TextView>(R.id.textViewUser)
        val textViewCellphone = v.findViewById<TextView>(R.id.textViewCellphone)
        val textViewPhysicalDistribution = v.findViewById<TextView>(R.id.textViewPhysicalDistribution)
//        val autoCompleteDailyObservation = v.findViewById<AutoCompleteTextView>(R.id.autoCompleteDailyObservation)
        val textViewComment = v.findViewById<TextView>(R.id.textViewComment)
        val textViewPurchaseVolume = v.findViewById<TextView>(R.id.textViewPurchaseVolume)
        val textViewRouteObservation = v.findViewById<TextView>(R.id.textViewRouteObservation)
        val textViewTotalBalance = v.findViewById<TextView>(R.id.textViewTotalBalance)
        layoutSalesList = v.findViewById(R.id.layoutSalesList)

        val operation : Operation = Operation()
        operation.userID = user.userID
        editTextObs.setText(p.observation)
        editTextAddress.setText(p.address)
        textViewPersonFullName.text = p.fullName
        textViewLatitude.text =p.latitude.toString()
        textViewLongitude.text =p.latitude.toString()

        textViewDocumentTypeDisplay.text ="${ p.documentTypeDisplay }: "
        textViewDocumentNumber.text = p.documentNumber
        textViewAddress.text = p.address
        textViewUser.text = "${p.gangName} ${p.visitDayDisplay.subSequence(0,2)}-${p.observation}"
        textViewCellphone.text = p.cellphone
        textViewPhysicalDistribution.text = p.physicalDistributionDisplaySaved
        textViewComment.text = p.comment
        textViewPurchaseVolume.text = p.purchaseVolume
        val total_pending_adjusted = String.format("%.2f", p.totalBalance).toDouble()

        if(total_pending_adjusted > 0){
            textViewTotalBalance.text = "S/ ${total_pending_adjusted}"
//            textViewTotalBalance.text = "S/ ${p.totalBalance}"
            textViewTotalBalance.setTextColor(ContextCompat.getColor(globalContext!!, R.color.red))
            dialogNewSale.visibility = View.GONE
            dialogNewDispatch.visibility = View.GONE

        }else{
            textViewTotalBalance.text = "S/ 0.00"
            textViewTotalBalance.setTextColor(ContextCompat.getColor(globalContext!!, R.color.purple_200))
            dialogNewSale.visibility = View.VISIBLE
            dialogNewDispatch.visibility = View.VISIBLE
        }

        if (p.routeObservation.isNotEmpty()){
            textViewRouteObservation.text = "VISITA DIA ${p.routeDate} — OBS: ${p.routeObservation.uppercase()}"
            textViewRouteObservation.visibility = View.VISIBLE
        }else{
            textViewRouteObservation.visibility = View.GONE
        }

        val googleMap = v.findViewById<MapView>(R.id.googleMap)
        MapsInitializer.initialize(globalContext!!)

        val addDialog = AlertDialog.Builder(globalContext)
        addDialog.setView(v)
        addDialog.setTitle("CLIENTE")
        val dialog: AlertDialog = addDialog.create()
        dialog.show()
        buttonCallPhone.setOnClickListener {
            val permission = ContextCompat.checkSelfPermission(globalContext!!, Manifest.permission.CALL_PHONE)
            if(permission != PackageManager.PERMISSION_GRANTED){
                Toast.makeText(globalContext, "No tiene permisos de llamada", Toast.LENGTH_SHORT).show()
                ActivityCompat.requestPermissions(globalContext as Activity, arrayOf(Manifest.permission.CALL_PHONE), 255)
            }else{
                val init = "tel:${p.cellphone}"
                val i:Intent = Intent(Intent.ACTION_CALL)
                i.data = Uri.parse(init)
                startActivity(i)
            }
        }
        dialogGenerateCreditNote.setOnClickListener{
            val bundle = Bundle()
            bundle.putInt("userID", user.userID)
            bundle.putInt("operationID", p.routeDispatchID)
            findNavController().navigate(R.id.action_mapFragment_to_creditNoteFragment, bundle)
            dialog.dismiss()
        }
        dialogEditClient.setOnClickListener{
            val bundle = Bundle()
            bundle.putInt("userID", user.userID)
            bundle.putInt("vehicleID", vehicleID)
            bundle.putInt("personID", p.personID)
            findNavController().navigate(R.id.action_mapFragment_to_clientEditFragment, bundle)
            dialog.dismiss()
        }
        if (!p.isEnabled) {
            dialogNewSale.visibility = View.GONE
            dialogNewDispatch.visibility = View.GONE
            dialogWithoutDispatch.visibility = View.GONE
            dialogPresaleDelivered.visibility = View.GONE
            dialogPresaleNoDelivered.visibility = View.GONE
            dialogCancelPresale.visibility = View.GONE
            dialogCancelSale.visibility = View.GONE
            dialogPutInPending.visibility = View.GONE
            dialogGenerateCreditNote.visibility = View.GONE
        }else{
            when(p.routeStatus){
                "01"-> {
                    dialogPresaleDelivered.visibility = View.GONE
                    dialogPresaleNoDelivered.visibility = View.GONE
                    dialogCancelPresale.visibility = View.GONE
                    dialogCancelSale.visibility = View.GONE
                    dialogPutInPending.visibility = View.GONE
                    dialogGenerateCreditNote.visibility = View.GONE
                    dialogDeclined.visibility = View.GONE
                }
                "02"-> {
                    dialogWithoutDispatch.visibility = View.GONE
                    dialogPresaleDelivered.visibility = View.GONE
                    dialogPresaleNoDelivered.visibility = View.GONE
                    dialogCancelPresale.visibility = View.GONE
                    dialogPutInPending.visibility = View.GONE
                    dialogGenerateCreditNote.visibility = View.GONE
                    dialogDeclined.visibility = View.GONE
                }
                "03"-> {
                    dialogWithoutDispatch.visibility = View.GONE
                    dialogPresaleDelivered.visibility = View.GONE
                    dialogPresaleNoDelivered.visibility = View.GONE
                    dialogCancelPresale.visibility = View.GONE
                    dialogCancelSale.visibility = View.GONE
                    dialogPutInPending.visibility = View.GONE
                    dialogGenerateCreditNote.visibility = View.GONE
                    dialogDeclined.visibility = View.GONE
                }
                "04"-> {
                    dialogNewSale.visibility = View.GONE
                    dialogNewDispatch.visibility = View.GONE
                    dialogWithoutDispatch.visibility = View.GONE
                    dialogPresaleDelivered.visibility = View.GONE
                    dialogPresaleNoDelivered.visibility = View.GONE
                    dialogCancelSale.visibility = View.GONE
                    dialogPutInPending.visibility = View.GONE
                    dialogGenerateCreditNote.visibility = View.GONE
                    dialogDeclined.visibility = View.GONE
                }
                "05"-> {
                    dialogNewSale.visibility = View.GONE
                    dialogNewDispatch.visibility = View.GONE
                    dialogWithoutDispatch.visibility = View.GONE
                    dialogCancelPresale.visibility = View.GONE
                    dialogCancelSale.visibility = View.GONE
                    dialogPutInPending.visibility = View.GONE
                }
                "06"-> {
                    dialogWithoutDispatch.visibility = View.GONE
                    dialogPresaleDelivered.visibility = View.GONE
                    dialogPresaleNoDelivered.visibility = View.GONE
                    dialogCancelPresale.visibility = View.GONE
                    dialogCancelSale.visibility = View.GONE
                    dialogPutInPending.visibility = View.GONE
                    dialogGenerateCreditNote.visibility = View.GONE
                    dialogDeclined.visibility = View.GONE

                }
                "07"-> {
                    dialogNewSale.visibility = View.GONE
                    dialogNewDispatch.visibility = View.GONE
                    dialogWithoutDispatch.visibility = View.GONE
                    dialogPresaleNoDelivered.visibility = View.GONE
                    dialogCancelPresale.visibility = View.GONE
                    dialogCancelSale.visibility = View.GONE
                    dialogPutInPending.visibility = View.GONE
                }
                "08"-> {
                    dialogWithoutDispatch.visibility = View.GONE
                    dialogPresaleDelivered.visibility = View.GONE
                    dialogPresaleNoDelivered.visibility = View.GONE
                    dialogCancelPresale.visibility = View.GONE
                    dialogCancelSale.visibility = View.GONE
                    dialogPutInPending.visibility = View.GONE
                    dialogGenerateCreditNote.visibility = View.GONE
                    dialogDeclined.visibility = View.GONE
                }
            }

        }
        dialogPutInPending.setOnClickListener{
            operation.operationID = p.routeDispatchID
            putDispatchInPending(operation)
            dialog.dismiss()
        }
        dialogClose.setOnClickListener{
            dialog.dismiss()
        }
        dialogNewSale.setOnClickListener{
            goToSale(p!!)
            dialog.dismiss()
        }
        dialogNewDispatch.setOnClickListener{
            goToQuotation(p!!)
            dialog.dismiss()
        }
        dialogSaveClientData.setOnClickListener{
            p.observation=editTextObs.text.toString()
            p.address=editTextAddress.text.toString()
            sendClientAddress(p)
            dialog.dismiss()
        }
        dialogDeclined.setOnClickListener{
            val inflater = LayoutInflater.from(globalContext)
            val view = inflater.inflate(R.layout.dialog_declined_sale, null)
            
            val autoCompleteObservation = view.findViewById<AutoCompleteTextView>(R.id.autoCompleteObservation)
            var dailyObsSelected = "N.A."
            if (p.routeObservation.isNotEmpty()){
                dailyObsSelected = p.routeObservation
            }
            val listDailyObs = listOf(
                "Sin Dinero",
                "No pidió",
                "Cerrado por viaje o salud",
                "No está la dueña"
            )
            val adapterDailyObs = ArrayAdapter(
                globalContext!!,
                android.R.layout.simple_dropdown_item_1line,
                listDailyObs
            )
            autoCompleteObservation.setAdapter(adapterDailyObs)
            autoCompleteObservation.setText(dailyObsSelected, false)

            val dialogObservation = AlertDialog.Builder(globalContext)
                .setTitle("Rechazar Venta")
                .setMessage("Selecciona el motivo del rechazo")
                .setView(view)
                .setPositiveButton("Confirmar") { dialogObs, _ ->
                    val userInput = autoCompleteObservation.text.toString()
                    if (userInput.isNotBlank()) {
                        p.routeObservation = userInput
                        p.userID = user.userID
                        sendDeclinedSale(p)
                    } else {
                        Toast.makeText(globalContext, "Por favor selecciona un motivo", Toast.LENGTH_SHORT).show()
                    }
                    dialogObs.dismiss()
                    dialog.dismiss()
                }
                .setNegativeButton("Cancelar") { dialogObs, _ ->
                    dialogObs.dismiss()
                }
                .create()

            dialogObservation.show()

            val positiveButton = dialogObservation.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setTextColor(ContextCompat.getColor(globalContext!!, R.color.white))
            positiveButton.textSize = 14f
            positiveButton.setBackgroundColor(ContextCompat.getColor(globalContext!!, R.color.black))

            val negativeButton = dialogObservation.getButton(AlertDialog.BUTTON_NEGATIVE)
            negativeButton.setTextColor(ContextCompat.getColor(globalContext!!, R.color.white))
            negativeButton.textSize = 14f
            negativeButton.setBackgroundColor(ContextCompat.getColor(globalContext!!, R.color.primary_dark))

        }
        dialogWithoutDispatch.setOnClickListener{
            val inflater = LayoutInflater.from(globalContext)
            val view = inflater.inflate(R.layout.dialog_without_dispatch, null)
            
            val autoCompleteObservation = view.findViewById<AutoCompleteTextView>(R.id.autoCompleteObservation)
            var dailyObsSelected = "N.A."
            if (p.routeObservation.isNotEmpty()){
                dailyObsSelected = p.routeObservation
            }
            val listDailyObs = listOf(
                "N.A.",
                "La tienda tiene stock",
                "La tienda se encuentra cerrada",
                "El dueño no tiene dinero",
                "El dueño no está presente"
            )
            val adapterDailyObs = ArrayAdapter(
                globalContext!!,
                android.R.layout.simple_dropdown_item_1line,
                listDailyObs
            )
            autoCompleteObservation.setAdapter(adapterDailyObs)
            autoCompleteObservation.setText(dailyObsSelected, false)

            val dialogObservation = AlertDialog.Builder(globalContext)
                .setView(view)
                .setPositiveButton("Confirmar") { dialogObs, _ ->
                    val userInput = autoCompleteObservation.text.toString()
                    if (userInput.isNotBlank()) {
                        p.routeObservation = userInput
                        sendVisitWithoutDispatch(p)
                    } else {
                        Toast.makeText(globalContext, "Por favor selecciona un motivo", Toast.LENGTH_SHORT).show()
                    }
                    dialogObs.dismiss()
                    dialog.dismiss()
                }
                .setNegativeButton("Cancelar") { dialogObs, _ ->
                    dialogObs.dismiss()
                }
                .create()

            dialogObservation.show()

            val positiveButton = dialogObservation.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setTextColor(ContextCompat.getColor(globalContext!!, R.color.white))
            positiveButton.textSize = 14f
            positiveButton.setBackgroundColor(ContextCompat.getColor(globalContext!!, R.color.black))

            val negativeButton = dialogObservation.getButton(AlertDialog.BUTTON_NEGATIVE)
            negativeButton.setTextColor(ContextCompat.getColor(globalContext!!, R.color.white))
            negativeButton.textSize = 14f
            negativeButton.setBackgroundColor(ContextCompat.getColor(globalContext!!, R.color.primary_dark))
        }

        dialogPresaleDelivered.setOnClickListener{

            val inflater2 = LayoutInflater.from(globalContext)
            val v2 = inflater2.inflate(R.layout.dialog_payment_of_delivered, null)
            val buttonAddPayment = v2.findViewById<Button>(R.id.buttonAddPayment)
            var paymentMethodList: MutableMap<String, Double> = mutableMapOf()
            val layoutPaymentList = v2.findViewById<LinearLayout>(R.id.layoutPaymentList)
            val textViewDialogTotal = v2.findViewById<TextView>(R.id.textViewDialogTotal)
            val textViewDialogSubtotal = v2.findViewById<TextView>(R.id.textViewDialogSubtotal)
            val editTextMethodPrice = v2.findViewById<TextInputEditText>(R.id.editTextMethodPrice)
            val autoCompleteMethodName = v2.findViewById<AutoCompleteTextView>(R.id.autoCompleteMethodName)
            val dialogButtonClose = v2.findViewById<Button>(R.id.dialog_close)
            val dialogButtonSave = v2.findViewById<Button>(R.id.dialog_terminate)
            val loadingLayout = v2.findViewById<FrameLayout>(R.id.loadingLayout)

            val listMethod = listOf("CONTADO", "YAPE", "CREDITO")
            val adapter = ArrayAdapter(
                globalContext!!,
                android.R.layout.simple_spinner_dropdown_item,
                listMethod
            )
            autoCompleteMethodName.keyListener = null
            autoCompleteMethodName.setAdapter(adapter)
            autoCompleteMethodName.setText(
                autoCompleteMethodName.adapter.getItem(0).toString(),
                false
            )
            val totalCharged:Double = String.format("%.2f", p.routeDispatchTotalSold).toDouble()
            textViewDialogTotal.text = totalCharged.toString()
            textViewDialogSubtotal.text = "0.00"
            
            if (paymentMethodList.isEmpty()){
                editTextMethodPrice.setText(totalCharged.toString())
            }

            // Función para actualizar el subtotal
            fun updateSubtotal() {
                var total = 0.0
                paymentMethodList.forEach { (_, value) ->
                    total += value
                }
                val totalF1:Double = String.format("%.2f", total).toDouble()
                textViewDialogSubtotal.text = totalF1.toString()
                
                // Actualizar estado del botón guardar
                dialogButtonSave.isEnabled = totalF1 == totalCharged
            }

            buttonAddPayment.setOnClickListener {
                if (editTextMethodPrice.text.toString().isNotEmpty()){
                    var selectedItem = "cash"
                    val value = autoCompleteMethodName.text.toString()
                    val amount = editTextMethodPrice.text.toString().toDouble()

                    // Validar que el monto no exceda el total
                    var currentTotal = 0.0
                    paymentMethodList.forEach { (_, value) -> currentTotal += value }
                    
                    if (amount + currentTotal > totalCharged) {
                        Toast.makeText(globalContext, "El monto total no puede superar S/ $totalCharged", Toast.LENGTH_LONG).show()
                        return@setOnClickListener
                    }

                    when (value) {
                        "CONTADO" -> {selectedItem = "cash"}
                        "YAPE" -> {selectedItem = "yape"}
                        "CREDITO" -> {selectedItem = "credit"}
                    }

                    val searchPaymentMethod = paymentMethodList.filter { (key, _) -> key == selectedItem }
                    if (searchPaymentMethod.isEmpty()) {
                        val inflaterMethod = LayoutInflater.from(globalContext)
                        val viewMethod = inflaterMethod.inflate(R.layout.item_payment_method, null)

                        val textViewPaymentMethodName = viewMethod.findViewById<TextView>(R.id.textViewPaymentMethodName)
                        val textViewPaymentMethodPrice = viewMethod.findViewById<TextView>(R.id.textViewPaymentMethodPrice)
                        val btnRemove = viewMethod.findViewById<Button>(R.id.buttonRemovePaymentMethodItem)

                        textViewPaymentMethodName.text = value
                        textViewPaymentMethodPrice.text = "S/ $amount"

                        paymentMethodList[selectedItem] = amount
                        updateSubtotal()

                        btnRemove.setOnClickListener {
                            layoutPaymentList.removeView(viewMethod)
                            paymentMethodList.remove(selectedItem)
                            updateSubtotal()
                        }

                        editTextMethodPrice.setText("")
                        val inputManager = globalContext?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        editTextMethodPrice.closeKeyBoard(inputManager)
                        editTextMethodPrice.clearFocus()
                        layoutPaymentList.addView(viewMethod)
                    }else {
                        autoCompleteMethodName.showDropDown()
                        Toast.makeText(globalContext, "Ya existe el método de pago seleccionado", Toast.LENGTH_LONG).show()
                    }
                }else {
                    Toast.makeText(globalContext, "Ingrese un monto válido", Toast.LENGTH_LONG).show()
                }
            }

            val addDialogPayment = AlertDialog.Builder(globalContext)
            addDialogPayment.setView(v2)
            addDialogPayment.setTitle("COBRANZA DE ENTREGA")
            addDialogPayment.setCancelable(false)

            val dialogPayment: AlertDialog = addDialogPayment.create()
            dialogPayment.show()
            
            dialogButtonClose.setOnClickListener{
                dialogPayment.dismiss()
            }
            
            dialogButtonSave.setOnClickListener{
                val payed = textViewDialogSubtotal.text.toString().toDouble()
                val totalSale = textViewDialogTotal.text.toString().toDouble()
                
                if (totalSale >= 0 && totalSale == payed) {
                    dialogButtonSave.isEnabled = false
                    dialogButtonClose.isEnabled = false
                    loadingLayout.visibility = View.VISIBLE
                    
                    operation.operationID = p.routeDispatchID
                    operation.paymentMethods = paymentMethodList
                    sendPresaleDelivered(operation)
                    Toast.makeText(globalContext, "Procesando pago...", Toast.LENGTH_SHORT).show()
                    
                    dialogPayment.dismiss()
                    dialog.dismiss()
                } else {
                    val remaining = totalSale - payed
                    val formattedRemaining = String.format("%.2f", remaining)
                    Toast.makeText(globalContext, "Falta registrar S/ $formattedRemaining", Toast.LENGTH_LONG).show()
                }
            }
        }

        dialogPresaleNoDelivered.setOnClickListener{
            operation.operationID = p.routeDispatchID
//            operation.observation = autoCompleteDailyObservation.text.toString()
            sendPresaleNoDelivered(operation)
            dialog.dismiss()
        }
        dialogCancelPresale.setOnClickListener{
            operation.operationID = p.routeDispatchID
//            operation.observation = autoCompleteDailyObservation.text.toString()
            sendCancelPresale(operation)
            dialog.dismiss()
        }
        dialogCancelSale.setOnClickListener{
            operation.operationID = p.routeDispatchID
            sendCancelSale(operation)
            dialog.dismiss()
        }
        googleMap.onCreate(dialog.onSaveInstanceState())
        googleMap.onResume()
        googleMap.getMapAsync { map ->
            map.mapType = GoogleMap.MAP_TYPE_NORMAL
            map.isBuildingsEnabled = false


            if(p.address != ""){
                map.addMarker(
                    MarkerOptions()
                        .position(LatLng(p.latitude, p.longitude))
                        .title("Dirección: ${ p.address }")
                        .snippet("Celular: ${ p.phone }").draggable(true)
                )
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(p.latitude, p.longitude), 15f))
            }else{
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(-16.418401, -71.539085), 15f))
            }
            map.setOnMapClickListener { latLng ->
                map.clear()
                map.addMarker(
                    MarkerOptions()
                        .position(latLng)
                        .title("No hay direccion").draggable(true)
                )
                textViewLatitude.text =latLng.latitude.toString()
                textViewLongitude.text =latLng.longitude.toString()
                p.latitude = latLng.latitude
                p.longitude = latLng.longitude
            }
            map.setOnMarkerDragListener(object : GoogleMap.OnMarkerDragListener{
                override fun onMarkerDrag(marker: Marker) {}

                override fun onMarkerDragEnd(marker: Marker) {
                    var lat = marker.position.latitude
                    var lon = marker.position.longitude
                    textViewLatitude.text =lat.toString()
                    textViewLongitude.text =lon.toString()
                    p.latitude = lat
                    p.longitude = lon
                    Toast.makeText(globalContext, "ON "+"Marker " + marker.getId() + " Draggable" + marker.getPosition(), Toast.LENGTH_SHORT).show()
                }

                override fun onMarkerDragStart(marker: Marker) {
                    var lat = marker.position.latitude
                    var lon = marker.position.longitude
                    textViewLatitude.text =lat.toString()
                    textViewLongitude.text =lon.toString()
                    p.latitude = lat
                    p.longitude = lon
                }
            })


        }
    }

    private fun reviewOrderHistoryByClient(){
        val apiInterface = UserApiService.create().getOrderHistoryByClientID(operation)
        apiInterface.enqueue(object : Callback<ArrayList<Operation>> {
            override fun onResponse(
                call: Call<ArrayList<Operation>>,
                response: Response<ArrayList<Operation>>
            ) {
                if (response.body() != null) {
                    var totalBalance: Double = 0.0
                    listOperations = response.body()!!
                    listOperations.forEach { o ->
                        val inflaterPaymentMethod = LayoutInflater.from(globalContext)
                        val viewPaymentMethod = inflaterPaymentMethod.inflate(R.layout.item_order_history, null)
                        val constraintLayoutOrder = viewPaymentMethod.findViewById<ConstraintLayout>(R.id.constraintLayoutOrder)
                        val textViewOperationDate = viewPaymentMethod.findViewById<TextView>(R.id.textViewOperationDate)
                        val textViewDocumentNumber = viewPaymentMethod.findViewById<TextView>(R.id.textViewDocumentNumber)
                        val textViewUserFullName = viewPaymentMethod.findViewById<TextView>(R.id.textViewUserFullName)
                        val textViewValueTotalPurchased = viewPaymentMethod.findViewById<TextView>(R.id.textViewValueTotalPurchased)
                        val textViewValueTotalPaid = viewPaymentMethod.findViewById<TextView>(R.id.textViewValueTotalPaid)
                        val textViewValueTotalPending = viewPaymentMethod.findViewById<TextView>(R.id.textViewValueTotalPending)
                        val textViewValueTotalReturned = viewPaymentMethod.findViewById<TextView>(R.id.textViewValueTotalReturned)
                        val buttonShowOrder = viewPaymentMethod.findViewById<Button>(R.id.buttonShowOrder)
                        textViewValueTotalPurchased.text = o.totalPurchased.toString()
                        textViewValueTotalPaid.text = o.totalPaid.toString()
                        textViewValueTotalPending.text = o.totalPending.toString()
                        textViewValueTotalReturned.text = o.totalReturned.toString()
                        if (o.totalPending > 0){
                            if (o.operationStatus == "02"){

                                constraintLayoutOrder.setBackgroundColor(ContextCompat.getColor(globalContext!!, R.color.purple_200))
                                totalBalance += o.totalPending
                            }else{
                                constraintLayoutOrder.setBackgroundColor(ContextCompat.getColor(globalContext!!, R.color.red_light))
                            }

                        }else{

                            constraintLayoutOrder.setBackgroundColor(ContextCompat.getColor(globalContext!!, R.color.primary_light))
                        }
                        textViewOperationDate.text = o.operationDate
                        textViewDocumentNumber.text = o.documentNumber
                        textViewUserFullName.text = o.userFullName.split(" ").getOrNull(0) ?: "Desconocido"

                        buttonShowOrder.setOnClickListener {
                            // Toast.makeText(globalContext, "Go to print", Toast.LENGTH_LONG).show()
                            val bundle = arguments
                            bundle!!.putString("operationID", o.operationID.toString())
                            findNavController().navigate(R.id.action_mapFragment_to_printActivity, bundle)
                        }
                        layoutSalesList.addView(viewPaymentMethod)
                    }

                }
            }

            override fun onFailure(call: Call<ArrayList<Operation>>, t: Throwable) {
                Log.d("MIKE", "reviewOrderHistoryByClient. Algo salio mal..." + t.message.toString())
            }
        })

    }

    private fun sendClientAddress(p: Person){
        val apiInterface = UserApiService.create().sendClientAddressData(p)
        apiInterface.enqueue(object : Callback<Person> {
            override fun onResponse(
                call: Call<Person>,
                response: Response<Person>
            ) {
                if (response.body() != null) {
                    Toast.makeText(globalContext, "Se guardarón los datos", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<Person>, t: Throwable) {
                Log.d("MIKE", "sendClientAddress. Algo salio mal..." + t.message.toString())
            }
        })

    }


    private fun sendVisitWithoutDispatch(p: Person){
        val apiInterface = UserApiService.create().saveVisitWithoutDispatch(p)
        apiInterface.enqueue(object : Callback<ResponseApi> {
            override fun onResponse(
                call: Call<ResponseApi>,
                response: Response<ResponseApi>
            ) {
                if (response.body() != null) {
                    Toast.makeText(globalContext, "Se registro cliente sin pedido", Toast.LENGTH_LONG).show()
                    sendRoute()
                }
            }

            override fun onFailure(call: Call<ResponseApi>, t: Throwable) {
                Log.d("MIKE", "sendVisitWithoutDispatch. Algo salio mal..." + t.message.toString())
            }
        })
    }

    private fun sendDeclinedSale(p: Person){
        val apiInterface = UserApiService.create().saveDeclinedSale(p)
        apiInterface.enqueue(object : Callback<ResponseApi> {
            override fun onResponse(
                call: Call<ResponseApi>,
                response: Response<ResponseApi>
            ) {
                if (response.body() != null) {
                    Toast.makeText(globalContext, "Se rechazo pedido", Toast.LENGTH_LONG).show()
                    sendRoute()
                }
            }

            override fun onFailure(call: Call<ResponseApi>, t: Throwable) {
                Log.d("MIKE", "sendDeclinedSale. Algo salio mal..." + t.message.toString())
            }
        })
    }

    private fun sendPresaleDelivered(o: Operation){
        val apiInterface = UserApiService.create().savePresaleDelivered(o)
        apiInterface.enqueue(object : Callback<Operation> {
            override fun onResponse(
                call: Call<Operation>,
                response: Response<Operation>
            ) {
                if (response.body() != null) {
                    Toast.makeText(globalContext, "Se entrego pedido", Toast.LENGTH_LONG).show()
                    sendRoute()
                }
            }
            override fun onFailure(call: Call<Operation>, t: Throwable) {
                Log.d("MIKE", "sendPresaleDelivered. Algo salio mal..." + t.message.toString())
            }
        })
    }

    private fun sendPresaleNoDelivered(o: Operation){
        val apiInterface = UserApiService.create().savePresaleNoDelivered(o)
        apiInterface.enqueue(object : Callback<Operation> {
            override fun onResponse(
                call: Call<Operation>,
                response: Response<Operation>
            ) {
                if (response.body() != null) {
                    Toast.makeText(globalContext, "Se registro pedido no entregado", Toast.LENGTH_LONG).show()
                    sendRoute()
                }
            }
            override fun onFailure(call: Call<Operation>, t: Throwable) {
                Log.d("MIKE", "sendPresaleNoDelivered. Algo salio mal..." + t.message.toString())
            }
        })
    }

    private fun sendCancelSale(o: Operation){
        val apiInterface = UserApiService.create().cancelDispatchData(o)
        apiInterface.enqueue(object : Callback<Operation> {
            override fun onResponse(
                call: Call<Operation>,
                response: Response<Operation>
            ) {
                if (response.body() != null) {
                    Toast.makeText(globalContext, "Se registro pedido no entregado", Toast.LENGTH_LONG).show()
                    sendRoute()
                }
            }
            override fun onFailure(call: Call<Operation>, t: Throwable) {
                Log.d("MIKE", "sendPresaleNoDelivered. Algo salio mal..." + t.message.toString())
            }
        })
    }

    private fun sendCancelPresale(o: Operation){
        val apiInterface = UserApiService.create().saveCancelPresale(o)
        apiInterface.enqueue(object : Callback<Operation> {
            override fun onResponse(
                call: Call<Operation>,
                response: Response<Operation>
            ) {
                if (response.body() != null) {
                    Toast.makeText(globalContext, "Se registro cancelacion del pedido", Toast.LENGTH_LONG).show()
                    sendRoute()
                }
            }
            override fun onFailure(call: Call<Operation>, t: Throwable) {
                Log.d("MIKE", "sendCancelPresale. Algo salio mal..." + t.message.toString())
            }
        })
    }

    private fun putDispatchInPending(o: Operation){
        val apiInterface = UserApiService.create().putInPending(o)
        apiInterface.enqueue(object : Callback<Operation> {
            override fun onResponse(
                call: Call<Operation>,
                response: Response<Operation>
            ) {
                if (response.body() != null) {
                    Toast.makeText(globalContext, "Se cambio estado del pedido a pendiente", Toast.LENGTH_LONG).show()
                    sendRoute()
                }
            }

            override fun onFailure(call: Call<Operation>, t: Throwable) {
                Log.d("MIKE", "putDispatchInPending. Algo salio mal..." + t.message.toString())
            }
        })
    }

    private fun loadLocationGps() {

        val apiInterface = UserApiService.create().getApiGPS()
        apiInterface.enqueue(object : Callback<ArrayList<LocationGps>> {
            override fun onResponse(call: Call<ArrayList<LocationGps>>, response: Response<ArrayList<LocationGps>>) {

                if (response.body() != null) {
                    locationGPS = response.body()!!

                    locationGPS.forEachIndexed{ _, gps ->

                        val inflater = LayoutInflater.from(globalContext)
                        val viewMarker = inflater.inflate(R.layout.custom_gps_marker_layout, null)
                        val textViewNum = viewMarker.findViewById<TextView>(R.id.textViewNum)
                        val imageViewMarker = viewMarker.findViewById<ImageView>(R.id.imageViewMarker)

                        imageViewMarker.setImageResource(R.drawable.ic_gps_cropped)

                        textViewNum.text = gps.licencePlate


                        val markerMap =  MarkerOptions().position(LatLng(gps.latitude.toDouble(), gps.longitude.toDouble()))
                            .title(gps.licencePlate)
                            .icon(createDrawableFromView(globalContext!!, viewMarker))

                        mMap.addMarker(markerMap)
                        // mMap.setOnMarkerClickListener { marker ->showAlert(marker)}
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(gps.latitude.toDouble(), gps.longitude.toDouble()), 15.0f))
                    }

                }

            }

            override fun onFailure(call: Call<ArrayList<LocationGps>>, t: Throwable) {
                Log.d("MIKE", "loadLocationGps. Algo salio mal..." + t.message.toString())
            }
        })
    }




    private fun goToQuotation(person: Person){

        val bundle = Bundle()

        bundle.putInt("userID", preference.getData("userID").toInt())
        bundle.putInt("vehicleID", vehicleID)
        bundle.putString("vehicleLicensePlate",  preference.getData("vehicleLicensePlate"))
        bundle.putInt("personID",person.personID)
        bundle.putString("personFullName",person.fullName)
        bundle.putString("personAddress",person.address)
        bundle.putString("personDocumentNumber",person.documentNumber)
        bundle.putString("personDocumentType",person.documentType)
        bundle.putString("physicalDistribution",person.physicalDistribution)
        bundle.putString("physicalDistributionDisplay",person.physicalDistributionDisplay)
        bundle.putString("routeDate",person.routeDate)

        findNavController().navigate(R.id.action_mapFragment_to_quotationFragment, bundle)


    }
    private fun goToSale(person: Person){

        val bundle = Bundle()

        bundle.putInt("userID", preference.getData("userID").toInt())
        bundle.putInt("vehicleID", vehicleID)
        bundle.putString("vehicleLicensePlate",  preference.getData("vehicleLicensePlate"))
        bundle.putInt("personID",person.personID)
        bundle.putString("personFullName",person.fullName)
        bundle.putString("personAddress",person.address)
        bundle.putString("personDocumentNumber",person.documentNumber)
        bundle.putString("personDocumentType",person.documentType)
        bundle.putString("physicalDistribution",person.physicalDistribution)
        bundle.putString("physicalDistributionDisplay",person.physicalDistributionDisplay)
        bundle.putString("routeDate",person.routeDate)

        findNavController().navigate(R.id.action_mapFragment_to_saleFragment, bundle)

    }
}