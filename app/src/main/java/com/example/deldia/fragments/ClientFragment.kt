package com.example.deldia.fragments

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.deldia.R
import com.example.deldia.adapter.ClientAdapter
import com.example.deldia.adapter.GangAdapter
import com.example.deldia.localdatabase.Preference
import com.example.deldia.models.*
import com.example.deldia.retrofit.UserApiService
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class ClientFragment : Fragment() {
    private lateinit var recyclerViewClient: RecyclerView
    private lateinit var btnSearchClient: Button
    private lateinit var btnNewClient: Button
    private lateinit var radioGroup: RadioGroup
    private lateinit var radioButtonName: RadioButton
    private lateinit var radioButtonDni: RadioButton
    private lateinit var radioButtonRuc: RadioButton
    private lateinit var textInputEditTextSuggest: TextInputEditText

    private lateinit var textInputEditTextPersonName: TextInputEditText
    private lateinit var textInputEditTextPersonFirstSurname: TextInputEditText
    private lateinit var textInputEditTextPersonSecondSurname: TextInputEditText
    private lateinit var textInputEditTextPersonFiscalAddress: TextInputEditText

    private lateinit var textInputLayoutFirstSurname: TextInputLayout
    private lateinit var textInputLayoutSecondSurname: TextInputLayout
    private lateinit var textInputLayoutFiscalAddress: TextInputLayout

    private var globalContext: Context? = null
    private var vehicleID: Int = 0
    private var userID: Int = 0
    private var vehicleLicensePlate: String = ""

    private var requestFilterPerson: RequestFilterPerson = RequestFilterPerson()
    private var person: Person = Person()
    private var listGangs = arrayListOf<Gang>()

    private lateinit var preference: Preference

//    private lateinit var autoCompleteDocumentType: AutoCompleteTextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        globalContext = this.activity
        preference = Preference(globalContext)
        val bundle = arguments
        vehicleID = bundle!!.getInt("vehicleID")
        userID = bundle!!.getInt("userID")
        vehicleLicensePlate = bundle.getString("vehicleLicensePlate").toString()
        loadDistributors()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_client, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerViewClient = view.findViewById(R.id.recyclerViewClient)
        btnSearchClient = view.findViewById(R.id.btnSearchClient)
        btnNewClient = view.findViewById(R.id.btnNewClient)
        radioButtonName = view.findViewById(R.id.radioButtonName)
        radioButtonDni = view.findViewById(R.id.radioButtonDni)
        radioButtonRuc = view.findViewById(R.id.radioButtonRuc)
        radioGroup = view.findViewById(R.id.radioGroup)
        textInputEditTextSuggest = view.findViewById(R.id.textInputEditTextSuggest)
        radioGroup.setOnCheckedChangeListener(onCheckedChangeListener)

        textInputEditTextSuggest.addTextChangedListener(object: TextWatcher {

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun afterTextChanged(p0: Editable?) {
                requestFilterPerson.suggest= p0.toString()
            }
        })

        btnSearchClient.setOnClickListener {
            loadClients(requestFilterPerson)
        }
        btnNewClient.setOnClickListener {
            val bundle = arguments
            bundle!!.putInt("userID", userID)
            bundle.putInt("vehicleID", vehicleID)
            findNavController().navigate(R.id.action_clientFragment_to_clientRegisterFragment, bundle)
        }

    }

    private fun addInfo() {
        val inflater = LayoutInflater.from(globalContext)
        val v = inflater.inflate(R.layout.dialog_new_person, null)
        val autoCompleteDocumentType = v.findViewById<AutoCompleteTextView>(R.id.autoCompleteDocumentType)
        val textInputEditTextDocumentNumber = v.findViewById<TextInputEditText>(R.id.textInputEditTextDocumentNumber)
        textInputEditTextPersonName = v.findViewById(R.id.textInputEditTextPersonName)
        textInputEditTextPersonFirstSurname = v.findViewById(R.id.textInputEditTextPersonFirstSurname)
        textInputEditTextPersonSecondSurname = v.findViewById(R.id.textInputEditTextPersonSecondSurname)
        textInputEditTextPersonFiscalAddress = v.findViewById(R.id.textInputEditTextPersonFiscalAddress)
        textInputLayoutFirstSurname = v.findViewById(R.id.textInputLayoutFirstSurname)
        textInputLayoutSecondSurname = v.findViewById(R.id.textInputLayoutSecondSurname)
        textInputLayoutFiscalAddress = v.findViewById(R.id.textInputLayoutFiscalAddress)
        val dialog_save_client_data = v.findViewById<Button>(R.id.dialog_save_client_data)
        val dialog_close = v.findViewById<Button>(R.id.dialog_close)

        val autoCompleteVisitDay = v.findViewById<AutoCompleteTextView>(R.id.autoCompleteVisitDay)
        val autoCompleteGang = v.findViewById<AutoCompleteTextView>(R.id.autoCompleteGang)
        val btnCleanGang = v.findViewById<Button>(R.id.btnCleanGang)
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
        btnCleanGang.setOnClickListener{
            autoCompleteGang.setText("")
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
            autoCompleteVisitDay.adapter.getItem(0).toString(),
            false
        )

        val btnQuery = v.findViewById<Button>(R.id.btnQuery)
        btnQuery.setOnClickListener {
            if (textInputEditTextDocumentNumber.text!!.isNotEmpty()){

                val itemSelected = autoCompleteDocumentType.text.toString()
                if (itemSelected=="DNI"){person.documentType = "01"}
                else if (itemSelected=="RUC"){person.documentType = "06"}
                person.documentNumber = textInputEditTextDocumentNumber.text.toString()
                person.isClient = true
                fetchedPerson()
            }

        }
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
                textInputLayoutFiscalAddress.isVisible = false
            }else{
                person.documentType = "06"
                textInputLayoutFirstSurname.isVisible = false
                textInputLayoutSecondSurname.isVisible = false
                textInputLayoutFiscalAddress.isVisible = true
            }

        }

        val addDialog = AlertDialog.Builder(globalContext)
        addDialog.setView(v)
        addDialog.setTitle("NUEVO CLIENTE")
        val dialog: AlertDialog = addDialog.create()
        dialog.show()

        dialog_save_client_data.setOnClickListener {
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
            if (textInputEditTextDocumentNumber.text.toString().isNotEmpty()){
                if (textInputEditTextPersonName.text.toString().isNotEmpty()){
                    person.documentNumber = textInputEditTextDocumentNumber.text.toString()
                    person.name = textInputEditTextPersonName.text.toString()
                    person.firstSurname = textInputEditTextPersonFirstSurname.text.toString()
                    person.secondSurname = textInputEditTextPersonSecondSurname.text.toString()
                    person.fiscalAddress = textInputEditTextPersonFiscalAddress.text.toString()
                    person.isClient = true
//                    person.distributorID = userID
                    person.visitDay = selectedVisitDay
                    sendClient()

                }else Toast.makeText(globalContext, "Verificar nombre", Toast.LENGTH_SHORT).show()
            }else Toast.makeText(globalContext, "Verificar nro documento", Toast.LENGTH_SHORT).show()


            dialog.dismiss()
        }

        dialog_close.setOnClickListener{
            dialog.dismiss()
        }
//        addDialog.setPositiveButton("GUARDAR") { dialog, _ -> }
//        addDialog.setNegativeButton("CERRAR") { dialog, _ -> }

    }

    private fun fetchedPerson(){
        val apiInterface = UserApiService.create().getDocumentConsultation(person)
        apiInterface.enqueue(object : Callback<Person> {
            override fun onResponse(
                call: Call<Person>,
                response: Response<Person>
            ) {
                if (response.body() != null) {
                    person = response.body()!!
                    textInputEditTextPersonName.setText(person.name)
                    textInputEditTextPersonFirstSurname.setText(person.firstSurname)
                    textInputEditTextPersonSecondSurname.setText(person.secondSurname)
                    textInputEditTextPersonFiscalAddress.setText(person.fiscalAddress)
                }
            }

            override fun onFailure(call: Call<Person>, t: Throwable) {
                Log.d("MIKE", "Algo salio mal..." + t.message.toString())
            }
        })

    }

    private fun sendClient(){
        val apiInterface = UserApiService.create().sendClientData(person)
        apiInterface.enqueue(object : Callback<Person> {
            override fun onResponse(
                call: Call<Person>,
                response: Response<Person>
            ) {
                if (response.body() != null) {
                    person = response.body()!!
                    btnSearchClient.callOnClick()
                }
            }

            override fun onFailure(call: Call<Person>, t: Throwable) {
                Log.d("MIKE", "Algo salio mal..." + t.message.toString())
            }
        })

    }

    private fun sendClientAddress(){
        val apiInterface = UserApiService.create().sendClientAddressData(person)
        apiInterface.enqueue(object : Callback<Person> {
            override fun onResponse(
                call: Call<Person>,
                response: Response<Person>
            ) {
                if (response.body() != null) {
                    person = response.body()!!
                    btnSearchClient.callOnClick()
                }
            }

            override fun onFailure(call: Call<Person>, t: Throwable) {
                Log.d("MIKE", "sendClientAddress. Algo salio mal..." + t.message.toString())
            }
        })

    }
    private fun setInputLength(maxLength: Int) {
        val filters = arrayOf<InputFilter>(InputFilter.LengthFilter(maxLength))
        textInputEditTextSuggest.filters = filters
    }
    private val onCheckedChangeListener =
        RadioGroup.OnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.radioButtonName -> {
                    requestFilterPerson.criteria="name"
//                    setInputLength(200)
                }
                R.id.radioButtonDni -> {
                    requestFilterPerson.criteria="dni"
//                    setInputLength(8)
                }
                R.id.radioButtonRuc -> {
                    requestFilterPerson.criteria="ruc"
//                    setInputLength(11)
                }
                else -> {
                }
            }
        }

    private fun loadClients(w: RequestFilterPerson) {
        val apiInterface = UserApiService.create().getFilterClient(w)
        apiInterface.enqueue(object : Callback<ResponseFilterPerson> { // Changed callback type
            override fun onResponse(
                call: Call<ResponseFilterPerson>, // Changed response type
                response: Response<ResponseFilterPerson> // Changed response type
            ) {
                if (response.code() == 200) {
                    val responseBody = response.body()
                    if (responseBody != null) {
                        if (responseBody.results?.size!! > 0) {
                            val list = responseBody.results
                            recyclerViewClient.layoutManager = LinearLayoutManager(globalContext)
                            recyclerViewClient.setHasFixedSize(true)
                            recyclerViewClient.adapter = ClientAdapter(list, object : ClientAdapter.OnItemClickListener {
                                override fun editClient(model: Person) {
                                    editInfo(model)
                                }

                                override fun dispatchClient(model: Person) {
                                    goToQuotation(model)
                                }

                                override fun searchSales(model: Person) {
                                    searchSalesInfo(model)
                                }
                            })
                        } else if (responseBody.message != null) {
                            // No clients found, display the message
                            Toast.makeText(globalContext, responseBody.message, Toast.LENGTH_LONG).show()
                        }
                    } else {
                        // Handle the case where responseBody is null
                        Toast.makeText(globalContext, "Error: Response body is null", Toast.LENGTH_LONG).show()
                    }
                } else {
                    // Handle unsuccessful response
                    Toast.makeText(globalContext, "Error: ${response.code()}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<ResponseFilterPerson>, t: Throwable) { // Changed callback type
                Log.d("MIKE", "getFilterClient. Algo salio mal..." + t.message.toString())
                Toast.makeText(globalContext, "Error: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun editInfo(p: Person) {
        val bundle = Bundle()

        bundle.putInt("userID", userID)
        bundle.putInt("vehicleID", vehicleID)
        bundle.putInt("personID", p.personID)
        findNavController().navigate(R.id.action_clientFragment_to_clientEditFragment, bundle)

    }
    private fun searchSalesInfo(p: Person) {
        val bundle = Bundle()

        bundle.putInt("userID", userID)
        bundle.putInt("vehicleID", vehicleID)
        bundle.putInt("clientID", p.personID)
        bundle.putString("clientFullName", p.fullName)
        bundle.putString("clientAddress", p.address)
        findNavController().navigate(R.id.action_clientFragment_to_clientSaleRealizedFragment, bundle)

    }

    private fun loadDistributors(){

        val apiInterface = UserApiService.create().getGangs()
        apiInterface.enqueue(object : Callback<java.util.ArrayList<Gang>>{
            override fun onResponse(call: Call<java.util.ArrayList<Gang>>, response: Response<java.util.ArrayList<Gang>>) {
                listGangs = response.body()!!
            }

            override fun onFailure(call: Call<java.util.ArrayList<Gang>>, t: Throwable) {
                Log.d("MIKE", "fetchApiDistributors onFailure: " + t.message.toString())
            }

        })

    }
    private fun goToQuotation(p: Person){

        val bundle = Bundle()

        bundle.putInt("userID", userID)
        bundle.putInt("vehicleID", vehicleID)
        bundle.putString("vehicleLicensePlate",  preference.getData("vehicleLicensePlate"))
        bundle.putInt("personID",p.personID)
        bundle.putString("personFullName",p.fullName)
        bundle.putString("personAddress",p.address)
        bundle.putString("personDocumentNumber",p.documentNumber)
        bundle.putString("personDocumentType",p.documentType)
        bundle.putString("routeDate",p.routeDate)

        findNavController().navigate(R.id.action_clientFragment_to_quotationFragment, bundle)

    }

    private fun View.closeKeyBoard(inputMethodManager: InputMethodManager) {

        inputMethodManager.hideSoftInputFromWindow(windowToken, 0)

    }
}