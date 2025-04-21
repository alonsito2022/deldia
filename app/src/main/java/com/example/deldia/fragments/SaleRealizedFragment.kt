package com.example.deldia.fragments

import android.Manifest.permission.*
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.deldia.PrintActivity
import com.example.deldia.R
import com.example.deldia.ViewActivity
import com.example.deldia.adapter.*
import com.example.deldia.localdatabase.MySQLiteHelper
import com.example.deldia.localdatabase.Preference
import com.example.deldia.models.*
import com.example.deldia.pdf.Ticket
import com.example.deldia.retrofit.UserApiService
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


class SaleRealizedFragment : Fragment() {
    private lateinit var preference: Preference

    private lateinit var textViewSaleTotal: TextView
    private lateinit var recyclerViewSales: RecyclerView
    private lateinit var editTextSearchDate: TextInputEditText
    private lateinit var loadingLayout: FrameLayout

    private lateinit var btnSearch: Button
    private lateinit var autoCompleteGang: AutoCompleteTextView
    private lateinit var autoCompleteUser: AutoCompleteTextView
    private lateinit var autoCompleteSaleType: AutoCompleteTextView
    private lateinit var autoCompleteDailyRouteStatus: AutoCompleteTextView
    private lateinit var autoCompleteOperationStatus: AutoCompleteTextView
    private lateinit var textInputLayoutDailyRouteStatus: TextInputLayout
    private lateinit var textInputLayoutOperationStatus: TextInputLayout
    private lateinit var gangAdapter: GangAdapter
    private lateinit var userAdapter: UserAdapter
    private var globalContext: Context? = null
    private var operation: Operation = Operation()
    private var gang: Gang = Gang()

    private var list = arrayListOf<Operation>()
    private var listGangs = arrayListOf<Gang>()
    private var listUsers = arrayListOf<User>()

    lateinit var dbHelper: MySQLiteHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        globalContext = this.activity
        preference = Preference(globalContext)
        dbHelper = MySQLiteHelper(globalContext!!)
        val bundle = arguments
        operation.warehouseID = bundle!!.getInt("vehicleID")
        operation.userID = bundle.getInt("userID")
        operation.gangID = preference.getData("gangID").toInt()
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_sale_realized, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerViewSales = view.findViewById(R.id.recyclerViewSales)
        loadingLayout = view.findViewById(R.id.loadingLayout)

        autoCompleteGang = view.findViewById(R.id.autoCompleteGang)
        autoCompleteUser = view.findViewById(R.id.autoCompleteUser)
        autoCompleteSaleType = view.findViewById(R.id.autoCompleteSaleType)
        autoCompleteDailyRouteStatus = view.findViewById(R.id.autoCompleteDailyRouteStatus)
        autoCompleteOperationStatus = view.findViewById(R.id.autoCompleteOperationStatus)

        textViewSaleTotal = view.findViewById(R.id.textViewSaleTotal)
        editTextSearchDate = view.findViewById(R.id.editTextSearchDate)
        textInputLayoutDailyRouteStatus = view.findViewById(R.id.textInputLayoutDailyRouteStatus)
        textInputLayoutOperationStatus = view.findViewById(R.id.textInputLayoutOperationStatus)

        val listSaleType = listOf("PREVENTA", "VENTA")
        val adapterSaleType = ArrayAdapter(
            globalContext!!,
            android.R.layout.simple_spinner_dropdown_item,
            listSaleType
        )
        autoCompleteSaleType.keyListener = null
        autoCompleteSaleType.setAdapter(adapterSaleType)
        
        // Configurar visibilidad inicial para PREVENTA
        textInputLayoutDailyRouteStatus.visibility = View.VISIBLE
        textInputLayoutOperationStatus.visibility = View.GONE
        
        autoCompleteSaleType.setText(
            autoCompleteSaleType.adapter.getItem(0).toString(),
            false
        )

        // Configurar estados de ruta diaria
        val listDailyRouteStatus = listOf("ENVIO PENDIENTE", "ENVIADO", "ENTREGADO CONFORME", "ENVIO FALLIDO", "ENVIO ANULADO")
        val adapterDailyRouteStatus = ArrayAdapter(
            globalContext!!,
            android.R.layout.simple_spinner_dropdown_item,
            listDailyRouteStatus
        )
        autoCompleteDailyRouteStatus.keyListener = null
        autoCompleteDailyRouteStatus.setAdapter(adapterDailyRouteStatus)
        autoCompleteDailyRouteStatus.setText(
            autoCompleteDailyRouteStatus.adapter.getItem(0).toString(),
            false
        )

        // Configurar estados de operación
        val listOperationStatus = listOf("COMPLETADO", "ANULADO")
        val adapterOperationStatus = ArrayAdapter(
            globalContext!!,
            android.R.layout.simple_spinner_dropdown_item,
            listOperationStatus
        )
        autoCompleteOperationStatus.keyListener = null
        autoCompleteOperationStatus.setAdapter(adapterOperationStatus)
        autoCompleteOperationStatus.setText(
            autoCompleteOperationStatus.adapter.getItem(0).toString(),
            false
        )

        autoCompleteSaleType.setOnItemClickListener { parent, view, position, id ->
            when(autoCompleteSaleType.adapter.getItem(position).toString()){
                "VENTA" -> {
                    textInputLayoutDailyRouteStatus.visibility = View.GONE
                    textInputLayoutOperationStatus.visibility = View.VISIBLE
                    autoCompleteOperationStatus.setText(
                        autoCompleteOperationStatus.adapter.getItem(0).toString(),
                        false
                    )
                }
                "PREVENTA" -> {
                    textInputLayoutDailyRouteStatus.visibility = View.VISIBLE
                    textInputLayoutOperationStatus.visibility = View.GONE
                    autoCompleteDailyRouteStatus.setText(
                        autoCompleteDailyRouteStatus.adapter.getItem(0).toString(),
                        false
                    )
                }
            }
        }

        autoCompleteGang.setOnClickListener {
            autoCompleteGang.showDropDown()
        }

        autoCompleteUser.setOnClickListener {
            autoCompleteUser.showDropDown()
        }

        autoCompleteSaleType.setOnClickListener {
            autoCompleteSaleType.showDropDown()
        }

        autoCompleteDailyRouteStatus.setOnClickListener {
            autoCompleteDailyRouteStatus.showDropDown()
        }

        autoCompleteOperationStatus.setOnClickListener {
            autoCompleteOperationStatus.showDropDown()
        }

        val sdf2 = SimpleDateFormat("dd/MM/yyyy").format(Date())
        val sdf3 = SimpleDateFormat("yyyy-MM-dd").format(Date())

        operation.operationDate = sdf3
        operation.operationStatus="02"

        editTextSearchDate.setText(sdf2)

        editTextSearchDate.setOnClickListener { showDatePickerDialog() }

        btnSearch = view.findViewById(R.id.btnSearch)
        btnSearch.setOnClickListener{
            if (autoCompleteGang.text.toString().isEmpty()){
                Toast.makeText(globalContext, "Por favor seleccione una ruta", Toast.LENGTH_SHORT).show()
                autoCompleteGang.showDropDown()
                return@setOnClickListener
            }
            
            if (autoCompleteUser.text.toString().isEmpty()){
                Toast.makeText(globalContext, "Por favor seleccione un usuario", Toast.LENGTH_SHORT).show()
                autoCompleteUser.showDropDown()
                return@setOnClickListener
            }
            
            if(operation.operationDate.isEmpty()) {
                Toast.makeText(globalContext, "Por favor seleccione una fecha", Toast.LENGTH_SHORT).show()
                editTextSearchDate.performClick()
                return@setOnClickListener
            }

            val valueSaleType = autoCompleteSaleType.text.toString()
            var selectedSaleType = "09"
            when (valueSaleType){
                "VENTA" -> {selectedSaleType = "02"}
                "PREVENTA" -> {selectedSaleType = "09"}
            }
            operation.operationType = selectedSaleType

            val valueDailyRouteStatus = autoCompleteDailyRouteStatus.text.toString()
            var selectedDailyRouteStatus = "04"
            when (valueDailyRouteStatus){
                "ENVIO PENDIENTE" -> {selectedDailyRouteStatus = "04"}
                "ENVIADO" -> {selectedDailyRouteStatus = "05"}
                "ENTREGADO CONFORME" -> {selectedDailyRouteStatus = "06"}
                "ENVIO FALLIDO" -> {selectedDailyRouteStatus = "07"}
                "ENVIO ANULADO" -> {selectedDailyRouteStatus = "08"}
            }
            operation.routeStatus = selectedDailyRouteStatus

            val valueOperationStatus = autoCompleteOperationStatus.text.toString()
            var selectedOperationStatus = "02"
            when (valueOperationStatus){
                "COMPLETADO" -> {selectedOperationStatus = "02"}
                "ANULADO" -> {selectedOperationStatus = "03"}
            }
            operation.operationStatus = selectedOperationStatus

            loadDispatches()
        }
        loadGangs()
        loadAllUsers()

    }

    private fun loadGangs(){
        val apiInterface = UserApiService.create().getGangs()
        apiInterface.enqueue(object : Callback<ArrayList<Gang>> {
            override fun onResponse(call: Call<ArrayList<Gang>>, response: Response<ArrayList<Gang>>) {
                listGangs = response.body()!!

                gangAdapter = GangAdapter(globalContext!!, R.layout.item_gang_view, listGangs, object : GangAdapter.OnItemClickListener{
                    override fun onItemClick(model: Gang) {
                        autoCompleteGang.setText(model.name, false)
                        autoCompleteGang.dismissDropDown()
                        operation.warehouseID = model.warehouse.warehouseID
                        operation.gangID = model.gangID
                    }
                })
                autoCompleteGang.setAdapter(gangAdapter)

                // Preseleccionar la ruta basado en las preferencias
                val savedGangID = preference.getData("gangID").toInt()
                val savedGangName = preference.getData("gangName")
                if (savedGangID > 0 && savedGangName.isNotEmpty()) {
                    autoCompleteGang.setText(savedGangName, false)
                    operation.gangID = savedGangID
                }
            }

            override fun onFailure(call: Call<ArrayList<Gang>>, t: Throwable) {
                Log.d("MIKE", "gangs onFailure: " + t.message.toString())
            }
        })
    }

    private fun loadAllUsers(){
        val apiInterface = UserApiService.create().getAllSellers()
        apiInterface.enqueue(object : Callback<ArrayList<User>> {
            override fun onResponse(call: Call<ArrayList<User>>, response: Response<ArrayList<User>>) {
                listUsers = response.body()!!
                
                // Guardar el userID original
                val originalUserID = operation.userID
                
                // Agregar opción "TODOS"
                val allUsers = User().apply {
                    userID = -1  // Cambiamos a -1 para distinguir de IDs válidos
                    fullName = "TODOS"
                }
                listUsers.add(allUsers)
                
                userAdapter = UserAdapter(globalContext!!, R.layout.item_user_view, listUsers, object : UserAdapter.OnItemClickListener{
                    override fun onItemClick(model: User) {
                        autoCompleteUser.setText(model.fullName, false)
                        autoCompleteUser.dismissDropDown()
                        operation.userID = model.userID
                        Log.d("MIKE", "loadAllUsers userID... ${model.userID}")

                    }
                })
                autoCompleteUser.setAdapter(userAdapter)

                // Preseleccionar el usuario basado en el bundle y preferencias
                val savedUserName = preference.getData("userName")
                if (originalUserID > 0 && savedUserName.isNotEmpty()) {
                    autoCompleteUser.setText(savedUserName, false)
                    operation.userID = originalUserID
                }
            }

            override fun onFailure(call: Call<ArrayList<User>>, t: Throwable) {
                Log.d("MIKE", "loadAllUsers onFailure: " + t.message.toString())
            }
        })
    }

    private fun View.closeKeyBoard(inputMethodManager: InputMethodManager) {
        inputMethodManager.hideSoftInputFromWindow(windowToken, 0)
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
        operation.operationDate = sdf3
        editTextSearchDate.setText(sdf2)
    }


    private fun loadDispatches() {
        showLoading()
        Log.d("MIKE", "loadDispatches userID... ${operation.userID}")
        val apiInterface = UserApiService.create().getSalesInWarehouse(operation)
        apiInterface.enqueue(object : Callback<ArrayList<Operation>> {

            override fun onResponse(
                call: Call<ArrayList<Operation>>,
                response: Response<ArrayList<Operation>>
            ) {
                hideLoading()
                if (response.body() != null) {

                    list = response.body()!!
                    Log.d("MIKE", "loadDispatches... $list")
                    var total = 0.0
                    list.forEach{ operation ->
                        total += operation.total
                    }
                    val totalF3:Double = Math.round(total * 1000.0) / 1000.0
                    val totalF1:Double = Math.round(totalF3 * 100.0) / 100.0
//                    textViewSaleTotal.text = "TOTAL: ${String.format("%.2f", total.toString().replace(',', '.')).toDouble()}"
                    textViewSaleTotal.text = "TOTAL: $totalF1"

                    recyclerViewSales.layoutManager = LinearLayoutManager(globalContext)
                    recyclerViewSales.setHasFixedSize(true)
                    recyclerViewSales.adapter= OperationAdapter(globalContext!!, list, object : OperationAdapter.OnMenuItemClickListener{
                        override fun print(model: Operation) {
                            val bundle = arguments
                            bundle!!.putString("operationID", model.operationID.toString())
                            findNavController().navigate(R.id.action_saleRealizedFragment_to_printActivity, bundle)
                        }

                        override fun show(model: Operation) {
                            val bundle = Bundle()
                            bundle.putInt("userID", operation.userID)
                            bundle.putInt("operationID", model.operationID)
                            findNavController().navigate(R.id.action_saleRealizedFragment_to_dispatchFragment, bundle)

                        }

                        override fun cancelPresale(model: Operation) {
                            sendCancelPresale(model)
                            btnSearch.callOnClick()
                        }

                        override fun cancelSale(model: Operation) {
                            cancelApiDispatch(model)
                            btnSearch.callOnClick()
                        }


                        override fun generateQuotation(model: Operation) {
                            TODO("Not yet implemented")
                        }

                        override fun generateDispatch(model: Operation) {
                            TODO("Not yet implemented")
                        }

                        override fun showDetail(model: Operation) {
                            TODO("Not yet implemented")
                        }

                        override fun deleteOperation(model: Operation) {
                            TODO("Not yet implemented")
                        }
                    })

                }
            }

            override fun onFailure(call: Call<ArrayList<Operation>>, t: Throwable) {
                hideLoading()
                Log.d("MIKE", "loadDispatches. Algo salio mal..." + t.message.toString())
                Toast.makeText(globalContext, "Error al cargar los datos: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun cancelApiDispatch(o: Operation){

        val apiInterface = UserApiService.create().cancelDispatchData(o)
        apiInterface.enqueue(object : Callback<Operation>{
            override fun onResponse(call: Call<Operation>, response: Response<Operation>) {
                Toast.makeText(globalContext, "Se anulo el pedido.", Toast.LENGTH_SHORT).show()
            }
            override fun onFailure(call: Call<Operation>, t: Throwable) {
                Log.d("MIKE", "sendApiDispatch onFailure: " + t.message.toString())
            }
        })
    }
    private fun sendCancelPresale(o: Operation){
        val apiInterface = UserApiService.create().saveCancelPresale(o)
        apiInterface.enqueue(object : Callback<Operation> {
            override fun onResponse(call: Call<Operation>, response: Response<Operation>) {
                if (response.body() != null) {
                    Toast.makeText(globalContext, "Se registro cancelacion del pedido", Toast.LENGTH_LONG).show()
                }
            }
            override fun onFailure(call: Call<Operation>, t: Throwable) {
                Log.d("MIKE", "sendCancelPresale. Algo salio mal..." + t.message.toString())
            }
        })
    }

    private fun showLoading() {
        loadingLayout.visibility = View.VISIBLE
        btnSearch.isEnabled = false
    }

    private fun hideLoading() {
        loadingLayout.visibility = View.GONE
        btnSearch.isEnabled = true
    }

}