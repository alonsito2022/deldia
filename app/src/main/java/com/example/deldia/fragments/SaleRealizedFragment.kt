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

        autoCompleteGang = view.findViewById(R.id.autoCompleteGang)
        autoCompleteUser = view.findViewById(R.id.autoCompleteUser)
        autoCompleteSaleType = view.findViewById(R.id.autoCompleteSaleType)
        autoCompleteDailyRouteStatus = view.findViewById(R.id.autoCompleteDailyRouteStatus)
        autoCompleteOperationStatus = view.findViewById(R.id.autoCompleteOperationStatus)

        textViewSaleTotal = view.findViewById(R.id.textViewSaleTotal)
        editTextSearchDate = view.findViewById(R.id.editTextSearchDate)
        textInputLayoutDailyRouteStatus = view.findViewById(R.id.textInputLayoutDailyRouteStatus)
        textInputLayoutOperationStatus = view.findViewById(R.id.textInputLayoutOperationStatus)

        val listSaleType = listOf("VENTA", "PREVENTA")
        val adapterSaleType = ArrayAdapter(
            globalContext!!,
            android.R.layout.simple_spinner_dropdown_item,
            listSaleType
        )
        autoCompleteSaleType.keyListener = null
        autoCompleteSaleType.setAdapter(adapterSaleType)
        autoCompleteSaleType.setText(
            autoCompleteSaleType.adapter.getItem(0).toString(),
            false
        )
        autoCompleteSaleType.setOnItemClickListener ( AdapterView.OnItemClickListener { parent, view, position, id ->

            when(autoCompleteSaleType.adapter.getItem(position).toString()){
                "VENTA" -> {
                    textInputLayoutDailyRouteStatus.visibility = View.GONE
                    textInputLayoutOperationStatus.visibility = View.VISIBLE
                }
                "PREVENTA" -> {
                    textInputLayoutDailyRouteStatus.visibility = View.VISIBLE
                    textInputLayoutOperationStatus.visibility = View.GONE
                }
            }

        })
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

        val sdf2 = SimpleDateFormat("dd/MM/yyyy").format(Date())
        val sdf3 = SimpleDateFormat("yyyy-MM-dd").format(Date())

        operation.operationDate = sdf3
        operation.operationStatus="02"

        editTextSearchDate.setText(sdf2)

        editTextSearchDate.setOnClickListener { showDatePickerDialog() }

        btnSearch = view.findViewById(R.id.btnSearch)
        btnSearch.setOnClickListener{
            if (autoCompleteGang.text.toString().isNotEmpty()){
                if (autoCompleteUser.text.toString().isNotEmpty()){
                    if(operation.operationDate.isNotEmpty()) {
                        val valueSaleType = autoCompleteSaleType.text.toString()
                        var selectedSaleType = "02"
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
                        autoCompleteGang.setText("")
                        autoCompleteUser.setText("")

                    }else
                        Toast.makeText(globalContext, "Elija fecha.", Toast.LENGTH_SHORT).show()
                }else
                    Toast.makeText(globalContext, "Elija Usuario.", Toast.LENGTH_SHORT).show()

            }else
                Toast.makeText(globalContext, "Elija Ruta.", Toast.LENGTH_SHORT).show()
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
                        autoCompleteGang.setText(model.name)
                        autoCompleteGang.dismissDropDown()
                        val inputManager = context!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        autoCompleteGang.closeKeyBoard(inputManager)
                        autoCompleteGang.clearFocus()
                        operation.warehouseID = model.warehouse.warehouseID
                        operation.gangID = model.gangID

//                        loadUsers(model)
                    }
                })
                autoCompleteGang.setAdapter(gangAdapter)
                Log.d("MIKE", "gangs ok: " + listGangs.size)
            }

            override fun onFailure(call: Call<ArrayList<Gang>>, t: Throwable) {
                Log.d("MIKE", "gangs onFailure: " + t.message.toString())
            }
        })

    }

    private fun loadUsers(g: Gang){

        val apiInterface = UserApiService.create().getUsersByGang(g)
        apiInterface.enqueue(object : Callback<ArrayList<User>> {
            override fun onResponse(call: Call<ArrayList<User>>, response: Response<ArrayList<User>>) {
                listUsers = response.body()!!
                val u: User = User()
                u.userID = 0
                u.fullName = "TODOS"
                listUsers.add(u)
                userAdapter = UserAdapter(globalContext!!, R.layout.item_user_view, listUsers, object : UserAdapter.OnItemClickListener{
                    override fun onItemClick(model: User) {
                        autoCompleteUser.setText(model.fullName)
                        autoCompleteUser.dismissDropDown()
                        val inputManager = context!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        autoCompleteUser.closeKeyBoard(inputManager)
                        autoCompleteUser.clearFocus()
                        operation.userID = model.userID
                    }
                })
                autoCompleteUser.setAdapter(userAdapter)
                Log.d("MIKE", "loadUsers ok: " + listUsers.size)
            }

            override fun onFailure(call: Call<ArrayList<User>>, t: Throwable) {
                Log.d("MIKE", "loadUsersByGang onFailure: " + t.message.toString())
            }
        })
    }
    private fun loadAllUsers(){

        val apiInterface = UserApiService.create().getAllSellers()
        apiInterface.enqueue(object : Callback<ArrayList<User>> {
            override fun onResponse(call: Call<ArrayList<User>>, response: Response<ArrayList<User>>) {
                listUsers = response.body()!!
                val u: User = User()
                u.userID = 0
                u.fullName = "TODOS"
                listUsers.add(u)
                userAdapter = UserAdapter(globalContext!!, R.layout.item_user_view, listUsers, object : UserAdapter.OnItemClickListener{
                    override fun onItemClick(model: User) {
                        autoCompleteUser.setText(model.fullName)
                        autoCompleteUser.dismissDropDown()
                        val inputManager = context!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        autoCompleteUser.closeKeyBoard(inputManager)
                        autoCompleteUser.clearFocus()
                        operation.userID = model.userID
                    }
                })
                autoCompleteUser.setAdapter(userAdapter)
                Log.d("MIKE", "loadAllUsers ok: " + listUsers.size)
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


//    private fun loadRecoverUnsavedSales() {
//        val list2 = arrayListOf<Operation>()
//        val db: SQLiteDatabase = dbHelper.readableDatabase
//        val cursor = db.rawQuery("SELECT * FROM operation", null)
//        if(cursor.moveToFirst()){
//            do{
//                val op: Operation = Operation()
//                op.operationID = cursor.getInt(0)
//                op.clientID = cursor.getInt(1)
//                op.warehouseID = cursor.getInt(2)
//                op.userID = cursor.getInt(3)
//                op.documentType = cursor.getString(4)
//
//                op.operationDate = cursor.getString(5)
//                op.userFullName = cursor.getString(6)
//                op.clientFullName = cursor.getString(7)
//                op.clientDocumentType = cursor.getString(8)
//                op.clientDocumentNumber = cursor.getString(9)
//                op.clientVisitDayDisplay = cursor.getString(10)
//                op.gangName = cursor.getString(11)
//                op.total = cursor.getDouble(12)
//
//                val cursor2 = db.rawQuery("SELECT * FROM operation_detail WHERE operation_id=${op.operationID}", null)
//                if(cursor2.moveToFirst()){
//                    do{
//                        val od: Operation.OperationDetail = Operation.OperationDetail()
//                        od.quantity = cursor2.getInt(2)
//                        od.price = cursor2.getDouble(3)
//                        od.productTariffID = cursor2.getInt(4)
//                        od.productSaleName = cursor2.getString(5)
//                        od.subtotal = cursor2.getDouble(6)
//                        op.details.add(od)
//                    } while (cursor2.moveToNext())
//                }
//                list2.add(op)
//            } while (cursor.moveToNext())
//
//        }
//
//
//        recyclerViewSales.layoutManager = LinearLayoutManager(globalContext)
//        recyclerViewSales.setHasFixedSize(true)
//        recyclerViewSales.adapter= OperationAdapter(globalContext!!, list2, object : OperationAdapter.OnMenuItemClickListener{
//            override fun print(model: Operation) {
//                TODO("Not yet implemented")
//            }
//
//            override fun cancel(model: Operation) {
//                TODO("Not yet implemented")
//            }
//
//            override fun show(model: Operation) {
//                TODO("Not yet implemented")
//            }
//
//            override fun generateQuotation(model: Operation) {
//                model.documentType = "05"
//                sendApiQuotation(model)
//            }
//
//            override fun generateDispatch(model: Operation) {
////                    if(model.clientDocumentType=="01")
////                        model.documentType = "01"
////                    else
////                        model.documentType = "03"
////                    sendApiDispatch(model)
//            }
//
//            override fun showDetail(model: Operation) {
//                showInfo(model)
//            }
//
//            override fun deleteOperation(model: Operation) {
//                Toast.makeText(globalContext, "Se elimino la venta no recuperada", Toast.LENGTH_LONG).show()
//                dbHelper.deleteOperation(model.operationID)
//                btnSearch.callOnClick()
//            }
//        })
//    }

    private fun sendApiQuotation(o: Operation){
        val apiInterface = UserApiService.create().sendQuotationData(o)
        apiInterface.enqueue(object : Callback<Operation>{
            override fun onResponse(call: Call<Operation>, response: Response<Operation>) {
                if (response.body() != null) {
                    Toast.makeText(globalContext, "Se genero una cotizacion", Toast.LENGTH_LONG).show()
                    dbHelper.deleteOperation(o.operationID)
                    btnSearch.callOnClick()
                }
            }
            override fun onFailure(call: Call<Operation>, t: Throwable) {
                Log.d("MIKE", "sendApiQuotation onFailure: " + t.message.toString())
            }
        })
    }

    private fun sendApiDispatch(o: Operation){

        val apiInterface = UserApiService.create().sendDispatchData(o)
        apiInterface.enqueue(object : Callback<Operation> {
            override fun onResponse(call: Call<Operation>, response: Response<Operation>) {
                if (response.body() != null) {
                    Toast.makeText(globalContext, "Se genero una venta", Toast.LENGTH_LONG).show()
                    dbHelper.deleteOperation(o.operationID)
                    btnSearch.callOnClick()
                }

            }

            override fun onFailure(call: Call<Operation>, t: Throwable) {
                Log.d("MIKE", "sendApiQuotation onFailure: " + t.message.toString())
            }

        })

    }

    private fun showInfo(o: Operation){
        val inflater = LayoutInflater.from(globalContext)
        val v = inflater.inflate(R.layout.dialog_firebase_picking, null)
        val recyclerViewPickingDetail = v.findViewById<RecyclerView>(R.id.recyclerViewPickingDetail)
        val btnClose = v.findViewById<Button>(R.id.dialog_close)
        recyclerViewPickingDetail.layoutManager = LinearLayoutManager(activity)
        recyclerViewPickingDetail.setHasFixedSize(true)

        val firebasePickingDetailAdapter = OperationDetailUnsavedAdapter(o.details)
        recyclerViewPickingDetail.adapter = firebasePickingDetailAdapter

        val addDialog = AlertDialog.Builder(globalContext)
        addDialog.setView(v)
        addDialog.create()
        val dialog: AlertDialog = addDialog.create()
        dialog.show()
        btnClose.setOnClickListener{
            dialog.dismiss()
        }
    }

    private fun loadDispatches() {
        val apiInterface = UserApiService.create().getSalesInWarehouse(operation)
        apiInterface.enqueue(object : Callback<ArrayList<Operation>> {

            override fun onResponse(
                call: Call<ArrayList<Operation>>,
                response: Response<ArrayList<Operation>>
            ) {
//                var list = arrayListOf<Operation>()

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
                Log.d("MIKE", "loadDispatches. Algo salio mal..." + t.message.toString())
            }
        })
    }

    private fun loadOperation(o: Operation) {
        val apiInterface = UserApiService.create().getSaleByID(o)
        apiInterface.enqueue(object : Callback<Operation> {
            override fun onResponse(call: Call<Operation>, response: Response<Operation>) {

                if (response.body() != null) {
                    val tk = Ticket(globalContext!!, response.body()!!)
                    tk.generatePDF()
                    val intent = Intent(globalContext as Activity, ViewActivity::class.java)
                    intent.putExtra("FileUri", tk.getFilePathDirectory())
                    startActivity(intent)
                }
            }

            override fun onFailure(call: Call<Operation>, t: Throwable) {
                Log.d("MIKE", "loadOperation. Algo salio mal..." + t.message.toString())
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

    private fun replaceFragment(fragment: Fragment, o: Operation){

        val fragmentTransaction = activity?.supportFragmentManager?.beginTransaction()
        val bundle = Bundle()
        bundle.putInt("userID", operation.userID)
//        bundle.putInt("vehicleID", warehouse.warehouseID)
        bundle.putInt("operationID", o.operationID)
        fragment.arguments = bundle
        fragmentTransaction?.replace(R.id.fragmentContainerView, fragment)
        fragmentTransaction?.commit()

    }

}