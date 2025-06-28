package com.sys4soft.deldia.fragments

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sys4soft.deldia.adapter.OperationDetailAdapter
import com.sys4soft.deldia.adapter.PersonAdapter
import com.sys4soft.deldia.localdatabase.Preference
import com.sys4soft.deldia.models.*
import com.sys4soft.deldia.retrofit.UserApiService
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.google.android.material.floatingactionbutton.FloatingActionButton
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*
import kotlin.collections.ArrayList
import com.sys4soft.deldia.R

class RouteFragment : Fragment() {
    private lateinit var fabNewStopping: FloatingActionButton

    private var globalContext: Context? = null
    private var vehicleID: Int = 0
    private var userID: Int = 0
    private var personList = arrayListOf<Person>()
    private var listOperations = arrayListOf<Operation>()
    private var user: User = User()
    private var person: Person = Person()
    private var operation: Operation = Operation()

    private lateinit var recyclerViewOperationDetail: RecyclerView
    private lateinit var operationDetailAdapter: OperationDetailAdapter
    private lateinit var textViewDialogTotal: TextView
    private lateinit var textViewDialogSelectedItems: TextView
    private lateinit var textViewDialogItems: TextView

    private lateinit var searchViewRoute: SearchView
    private lateinit var recyclerViewRoutes: RecyclerView
    private lateinit var routeAdapter: PersonAdapter

    private lateinit var preference: Preference
    private lateinit var layoutSalesList: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        globalContext = this.activity
        preference = Preference(globalContext)
        val bundle = arguments
        vehicleID = bundle!!.getInt("vehicleID")
        userID = bundle.getInt("userID")
        user.userID= userID
        user.gang.gangID = preference.getData("gangID").toInt()
        loadRoadList(user)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_route, container, false)
//        val mapFragment = childFragmentManager.findFragmentById(R.id.frg) as SupportMapFragment?
        //use SupportMapFragment for using in fragment instead of activity  MapFragment = activity   SupportMapFragment = fragment


        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerViewRoutes = view.findViewById(R.id.recyclerViewRoutes)
        recyclerViewRoutes.layoutManager = LinearLayoutManager(globalContext)
        recyclerViewRoutes.setHasFixedSize(true)

        routeAdapter = PersonAdapter(personList, object : PersonAdapter.OnItemClickListener {
            override fun onItemClick(model: Person) {
                if(model.routeStatus == "04"){
                    showDialogDispatch(model)
                }else{
                    showDialogPerson(model)
                    operation.clientID = model.personID
                    reviewOrderHistoryByClient()
                }
            }
        })

        recyclerViewRoutes.adapter = routeAdapter

        fabNewStopping = view.findViewById(R.id.fabNewStopping)
        fabNewStopping.setOnClickListener {
//            Toast.makeText(globalContext, "Nueva parada", Toast.LENGTH_LONG).show()
            val bundle = Bundle()

            bundle.putInt("userID", userID)
            bundle.putInt("vehicleID", vehicleID)
            findNavController().navigate(R.id.action_routeFragment_to_clientRegisterFragment, bundle)

        }

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

    private fun loadRoadList(u: User) {
        val apiInterface = UserApiService.create().getDailyRoute(u)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ responseData ->
                Log.d("Mike", responseData?.size.toString())
                if(responseData != null && responseData.isNotEmpty()){
                    var sortedList = responseData.sortedWith(compareBy {it.observation.toDoubleOrNull()!!.toInt() })
                    personList = ArrayList(sortedList)
                    routeAdapter.getFilter(personList)
                }
            }, { error ->
                Log.d("Mike", error.toString())
            })


    }

    private fun showDialogDispatch(p: Person){
        operation.operationID = p.routeDispatchID

        val inflater = LayoutInflater.from(globalContext)
        val v = inflater.inflate(R.layout.dialog_show_dispatch, null)
        recyclerViewOperationDetail = v.findViewById(R.id.recyclerViewOperationDetail)
        textViewDialogItems = v.findViewById(R.id.textViewDialogItems)
        textViewDialogSelectedItems = v.findViewById(R.id.textViewDialogSelectedItems)
        textViewDialogTotal = v.findViewById(R.id.textViewDialogTotal)
        val btnClose = v.findViewById<Button>(R.id.btnClose)

        loadOperation(operation)

        val addDialog = AlertDialog.Builder(globalContext)
        addDialog.setView(v)
        addDialog.setTitle("PEDIDO")
        val dialog: AlertDialog = addDialog.create()
        dialog.show()
        btnClose.setOnClickListener{
            dialog.dismiss()
        }
    }

    private fun loadOperation(o: Operation) {

        val apiInterface = UserApiService.create().getSaleByID(o)
        apiInterface.enqueue(object : Callback<Operation> {
            override fun onResponse(call: Call<Operation>, response: Response<Operation>) {

                if (response.body() != null) {
                    operation = response.body()!!

                    recyclerViewOperationDetail.layoutManager = LinearLayoutManager(globalContext)
                    recyclerViewOperationDetail.setHasFixedSize(true)
                    operationDetailAdapter = OperationDetailAdapter(operation.details, object: OperationDetailAdapter.OnItemClickListener{
                        override fun onItemClick(model: Operation.OperationDetail, position: Int) {
                            model.enlisted = !model.enlisted
                            recyclerViewOperationDetail.adapter?.notifyItemChanged(position)
                            updateTotal()
                        }

                    })
                    recyclerViewOperationDetail.adapter = operationDetailAdapter

                    var counter = 0
                    var total = 0.0
                    operation.details.forEach {
                        counter += it.quantity
                        total += it.quantity * it.price
                    }
                    val totalF1:Double = String.format("%.2f", total).toDouble()
                    textViewDialogTotal.text = totalF1.toString()
                    textViewDialogItems.text = counter.toString()
                }
            }

            override fun onFailure(call: Call<Operation>, t: Throwable) {
                Log.d("MIKE", "loadOperation. Algo salio mal..." + t.message.toString())
            }
        })
    }

    private fun updateTotal() {
        var counter = 0
        operation.details.forEach {
            if (it.enlisted)
                counter += it.quantity
        }
        textViewDialogSelectedItems.text = counter.toString()
    }

    private fun showDialogPerson(p: Person){
        val inflater = LayoutInflater.from(globalContext)
        val v = inflater.inflate(R.layout.dialog_person_marker, null)
        val dialogClose = v.findViewById<Button>(R.id.dialog_close)
        val dialogCancelPresale = v.findViewById<Button>(R.id.dialog_cancel_presale)
        val dialogCancelSale = v.findViewById<Button>(R.id.dialog_cancel_sale)
        val dialogPresaleDelivered = v.findViewById<Button>(R.id.dialog_presale_delivered)
        val dialogPresaleNoDelivered = v.findViewById<Button>(R.id.dialog_presale_no_delivered)
        val dialogWithoutDispatch = v.findViewById<Button>(R.id.dialog_without_dispatch)
        val dialogSaveClientData = v.findViewById<Button>(R.id.dialog_save_client_data)
        val dialogEditClient = v.findViewById<Button>(R.id.dialog_edit_client)
        val dialogNewSale = v.findViewById<Button>(R.id.dialog_new_sale)
        val dialogNewDispatch = v.findViewById<Button>(R.id.dialog_new_dispatch)
        val dialogPutInPending = v.findViewById<Button>(R.id.dialog_put_in_pending)
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
        val autoCompleteDailyObservation = v.findViewById<AutoCompleteTextView>(R.id.autoCompleteDailyObservation)
        val textViewComment = v.findViewById<TextView>(R.id.textViewComment)
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

        var dailyObsSelected = "N.A."
        if (p.routeObservation.isNotEmpty()){
            dailyObsSelected = p.routeObservation
        }

        val listDailyObs = listOf(
            "N.A.",
            "La tienda tiene stock.",
            "La tienda se encuentra cerrada.",
            "El dueño no tiene dinero.",
            "El dueño no esta presente."
        )
        val adapterDailyObs = ArrayAdapter(
            globalContext!!,
            android.R.layout.simple_spinner_dropdown_item,
            listDailyObs
        )
        autoCompleteDailyObservation.keyListener = null
        autoCompleteDailyObservation.setAdapter(adapterDailyObs)
        autoCompleteDailyObservation.setText(
            autoCompleteDailyObservation.adapter.getItem(listDailyObs.indexOf(dailyObsSelected)).toString(),
            false
        )

        val googleMap = v.findViewById<MapView>(R.id.googleMap)
        MapsInitializer.initialize(globalContext!!)

        val addDialog = AlertDialog.Builder(globalContext)
        addDialog.setView(v)
        addDialog.setTitle("CLIENTE")
        val dialog: AlertDialog = addDialog.create()
        dialog.show()
        dialogEditClient.setOnClickListener{
            val bundle = Bundle()
            bundle.putInt("userID", userID)
            bundle.putInt("vehicleID", vehicleID)
            bundle.putInt("personID", p.personID)
            findNavController().navigate(R.id.action_routeFragment_to_clientEditFragment, bundle)
            dialog.dismiss()
        }
        if(!p.isEnabled){
            dialogNewSale.visibility = View.GONE
            dialogNewDispatch.visibility = View.GONE
            dialogWithoutDispatch.visibility = View.GONE
            dialogPresaleDelivered.visibility = View.GONE
            dialogPresaleNoDelivered.visibility = View.GONE
            dialogCancelPresale.visibility = View.GONE
            dialogCancelSale.visibility = View.GONE
            dialogPutInPending.visibility = View.GONE
        }else{
            when(p.routeStatus){
                "01"-> {
                    dialogPresaleDelivered.visibility = View.GONE
                    dialogPresaleNoDelivered.visibility = View.GONE
                    dialogCancelPresale.visibility = View.GONE
                    dialogCancelSale.visibility = View.GONE
                    dialogPutInPending.visibility = View.GONE
                }
                "02"-> {
                    dialogWithoutDispatch.visibility = View.GONE
                    dialogPresaleDelivered.visibility = View.GONE
                    dialogPresaleNoDelivered.visibility = View.GONE
                    dialogCancelPresale.visibility = View.GONE
                    dialogPutInPending.visibility = View.GONE
                }
                "03"-> {
                    dialogWithoutDispatch.visibility = View.GONE
                    dialogPresaleDelivered.visibility = View.GONE
                    dialogPresaleNoDelivered.visibility = View.GONE
                    dialogCancelPresale.visibility = View.GONE
                    dialogCancelSale.visibility = View.GONE
                    dialogPutInPending.visibility = View.GONE
                }
                "04"-> {
                    dialogNewSale.visibility = View.GONE
                    dialogNewDispatch.visibility = View.GONE
                    dialogWithoutDispatch.visibility = View.GONE
                    dialogPresaleDelivered.visibility = View.GONE
                    dialogPresaleNoDelivered.visibility = View.GONE
                    dialogCancelSale.visibility = View.GONE
                    dialogPutInPending.visibility = View.GONE
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
                }
                "07"-> {
                    dialogNewSale.visibility = View.GONE
                    dialogNewDispatch.visibility = View.GONE
                    dialogWithoutDispatch.visibility = View.GONE
                    dialogPresaleNoDelivered.visibility = View.GONE
                    dialogCancelPresale.visibility = View.GONE
                    dialogCancelSale.visibility = View.GONE
                }
                "08"-> {
                    dialogWithoutDispatch.visibility = View.GONE
                    dialogPresaleDelivered.visibility = View.GONE
                    dialogPresaleNoDelivered.visibility = View.GONE
                    dialogCancelPresale.visibility = View.GONE
                    dialogCancelSale.visibility = View.GONE
                    dialogPutInPending.visibility = View.GONE
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
            goToSale(p)
            dialog.dismiss()
        }
        dialogNewDispatch.setOnClickListener{
            goToQuotation(p)
            dialog.dismiss()
        }
        dialogSaveClientData.setOnClickListener{
            p.observation=editTextObs.text.toString()
            p.address=editTextAddress.text.toString()
            sendClientAddress(p)
            dialog.dismiss()
        }
        dialogWithoutDispatch.setOnClickListener{
            p.routeObservation = autoCompleteDailyObservation.text.toString()
            sendVisitWithoutDispatch(p)
            dialog.dismiss()
        }
        dialogPresaleDelivered.setOnClickListener{
            operation.operationID = p.routeDispatchID
            sendPresaleDelivered(operation)
            dialog.dismiss()
        }
        dialogPresaleNoDelivered.setOnClickListener{
            operation.operationID = p.routeDispatchID
            operation.observation = autoCompleteDailyObservation.text.toString()
            sendPresaleNoDelivered(operation)
            dialog.dismiss()
        }
        dialogCancelPresale.setOnClickListener{
            operation.operationID = p.routeDispatchID
            operation.observation = autoCompleteDailyObservation.text.toString()
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
//            map.setOnMapClickListener { latLng ->
//                map.clear()
//                map.addMarker(
//                    MarkerOptions()
//                        .position(latLng)
//                        .title("No hay direccion").draggable(true)
//                )
//                textViewLatitude.text =latLng.latitude.toString()
//                textViewLongitude.text =latLng.longitude.toString()
//                p.latitude = latLng.latitude
//                p.longitude = latLng.longitude
//            }
//            map.setOnMarkerDragListener(object : GoogleMap.OnMarkerDragListener{
//                override fun onMarkerDrag(marker: Marker) {}
//
//                override fun onMarkerDragEnd(marker: Marker) {
//                    var lat = marker.position.latitude
//                    var lon = marker.position.longitude
//                    textViewLatitude.text =lat.toString()
//                    textViewLongitude.text =lon.toString()
//                    p.latitude = lat
//                    p.longitude = lon
//                    Toast.makeText(globalContext, "ON "+"Marker " + marker.getId() + " Draggable" + marker.getPosition(), Toast.LENGTH_SHORT).show()
//                }
//
//                override fun onMarkerDragStart(marker: Marker) {
//                    var lat = marker.position.latitude
//                    var lon = marker.position.longitude
//                    textViewLatitude.text =lat.toString()
//                    textViewLongitude.text =lon.toString()
//                    p.latitude = lat
//                    p.longitude = lon
//                }
//            })


        }
    }

    private fun reviewOrderHistoryByClient(){
        val apiInterface = UserApiService.create().getOrderHistoryByClientID(operation)
        apiInterface.enqueue(object : Callback<java.util.ArrayList<Operation>> {
            override fun onResponse(
                call: Call<java.util.ArrayList<Operation>>,
                response: Response<java.util.ArrayList<Operation>>
            ) {
                if (response.body() != null) {
                    listOperations = response.body()!!
                    listOperations.forEach { o ->
                        val inflaterPaymentMethod = LayoutInflater.from(globalContext)
                        val viewPaymentMethod = inflaterPaymentMethod.inflate(R.layout.item_order_history, null)
                        val textViewOperationDate = viewPaymentMethod.findViewById<TextView>(R.id.textViewOperationDate)
                        val textViewDocumentNumber = viewPaymentMethod.findViewById<TextView>(R.id.textViewDocumentNumber)
                        val textViewTotal = viewPaymentMethod.findViewById<TextView>(R.id.textViewTotal)
                        val buttonShowOrder = viewPaymentMethod.findViewById<Button>(R.id.buttonShowOrder)
                        textViewOperationDate.text = o.operationDate
                        textViewDocumentNumber.text = o.documentNumber
                        textViewTotal.text = "S/ ${o.total}"

                        buttonShowOrder.setOnClickListener {
                            // Toast.makeText(globalContext, "Go to print", Toast.LENGTH_LONG).show()
                            val bundle = arguments
                            bundle!!.putString("operationID", o.operationID.toString())
                            findNavController().navigate(R.id.action_routeFragment_to_printActivity, bundle)
                        }
                        layoutSalesList.addView(viewPaymentMethod)
                    }

                }
            }

            override fun onFailure(call: Call<java.util.ArrayList<Operation>>, t: Throwable) {
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
                    loadRoadList(user)
                }
            }

            override fun onFailure(call: Call<ResponseApi>, t: Throwable) {
                Log.d("MIKE", "sendVisitWithoutDispatch. Algo salio mal..." + t.message.toString())
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
                    loadRoadList(user)
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
                    loadRoadList(user)
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
                    loadRoadList(user)
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
                    loadRoadList(user)
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
                    loadRoadList(user)
                }
            }

            override fun onFailure(call: Call<Operation>, t: Throwable) {
                Log.d("MIKE", "putDispatchInPending. Algo salio mal..." + t.message.toString())
            }
        })
    }
    private fun goToQuotation(person: Person){

        val bundle = Bundle()

        bundle.putInt("userID", userID)
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

        findNavController().navigate(R.id.action_routeFragment_to_quotationFragment, bundle)

    }


    private fun goToSale(person: Person){

        val bundle = Bundle()

        bundle.putInt("userID", userID)
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

        findNavController().navigate(R.id.action_routeFragment_to_saleFragment, bundle)

    }
}