package com.sys4soft.deldia.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sys4soft.deldia.R
import com.sys4soft.deldia.adapter.GangAdapter
import com.sys4soft.deldia.adapter.OperationAdapter
import com.sys4soft.deldia.localdatabase.Preference
import com.sys4soft.deldia.models.Gang
import com.sys4soft.deldia.models.Operation
import com.sys4soft.deldia.retrofit.UserApiService
import com.google.android.material.textfield.TextInputEditText
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*


class ClientSaleRealizedFragment : Fragment() {

    private lateinit var preference: Preference

    private lateinit var textViewSaleTotal: TextView
    private lateinit var textViewValueClientFullName: TextView
    private lateinit var textViewValueClientAddress: TextView
    private lateinit var recyclerViewSales: RecyclerView
    private lateinit var editTextSearchStartDate: TextInputEditText
    private lateinit var editTextSearchEndDate: TextInputEditText
    private lateinit var radioGroupStatus: RadioGroup
    private lateinit var radioButtonStatusPending: RadioButton
    private lateinit var radioButtonStatusCompleted: RadioButton
    private lateinit var btnSearch: Button
    private lateinit var autoCompleteGang: AutoCompleteTextView

    private var globalContext: Context? = null
    private var operation: Operation = Operation()

    private var list = arrayListOf<Operation>()
    private var listGangs = arrayListOf<Gang>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        globalContext = this.activity
        preference = Preference(globalContext)

        val bundle = arguments
        operation.clientID = bundle!!.getInt("clientID")
        operation.clientFullName = bundle.getString("clientFullName").toString()
        operation.clientAddress = bundle.getString("clientAddress").toString()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_client_sale_realized, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerViewSales = view.findViewById(R.id.recyclerViewSales)

        textViewValueClientFullName = view.findViewById(R.id.textViewValueClientFullName)
        textViewValueClientAddress = view.findViewById(R.id.textViewValueClientAddress)
        textViewValueClientFullName.text = operation.clientFullName
        textViewValueClientAddress.text = operation.clientAddress
        autoCompleteGang = view.findViewById(R.id.autoCompleteGang)
//        autoCompleteGang.setText(preference.getData("gangName"))
        radioButtonStatusPending = view.findViewById(R.id.radioButtonStatusPending)
        radioButtonStatusCompleted = view.findViewById(R.id.radioButtonStatusCompleted)
        radioGroupStatus = view.findViewById(R.id.radioGroupStatus)
        radioGroupStatus.setOnCheckedChangeListener{ _, checkedId ->
            when (checkedId) {
                R.id.radioButtonStatusPending -> {
                    operation.operationStatus="01"
                }
                R.id.radioButtonStatusCompleted -> {
                    operation.operationStatus="02"
                }
                else -> {}
            }
        }
        autoCompleteGang.setOnClickListener {
            autoCompleteGang.showDropDown() // Muestra el menú desplegable al hacer clic
        }

        textViewSaleTotal = view.findViewById(R.id.textViewSaleTotal)
        editTextSearchStartDate = view.findViewById(R.id.editTextSearchStartDate)
        editTextSearchEndDate = view.findViewById(R.id.editTextSearchEndDate)


        val sdf2 = SimpleDateFormat("dd/MM/yyyy").format(Date())
        val sdf3 = SimpleDateFormat("yyyy-MM-dd").format(Date())

        operation.operationStartDate = sdf3
        operation.operationEndDate = sdf3
        operation.operationStatus="02"

        editTextSearchStartDate.setText(sdf2)
        editTextSearchEndDate.setText(sdf2)

        editTextSearchStartDate.setOnClickListener { showDatePickerDialog("startDate") }
        editTextSearchEndDate.setOnClickListener { showDatePickerDialog("endDate") }

        btnSearch = view.findViewById(R.id.btnSearch)
        btnSearch.setOnClickListener{
            if(operation.operationStartDate.isNotEmpty() && operation.operationEndDate.isNotEmpty()) {
                loadDispatches()
//                autoCompleteGang.setText("")

            }else
                Toast.makeText(globalContext, "Elija fecha.", Toast.LENGTH_SHORT).show()
        }
        loadDistributors()
    }

    private fun loadDistributors(){

        val apiInterface = UserApiService.create().getGangs()
        apiInterface.enqueue(object : Callback<ArrayList<Gang>> {
            override fun onResponse(call: Call<ArrayList<Gang>>, response: Response<ArrayList<Gang>>) {
                listGangs = response.body()!!
                autoCompleteGang.setAdapter(GangAdapter(globalContext!!, R.layout.item_gang_view, listGangs, object : GangAdapter.OnItemClickListener{
                    override fun onItemClick(model: Gang) {
                        autoCompleteGang.setText(model.name, false)
                        autoCompleteGang.dismissDropDown()
//                        val inputManager = context!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
//                        autoCompleteGang.closeKeyBoard(inputManager)
//                        autoCompleteGang.clearFocus()
                        operation.warehouseID = model.warehouse.warehouseID

                    }
                }))
                Log.d("MIKE", "loadDistributors ok: " + listGangs.size)
            }

            override fun onFailure(call: Call<ArrayList<Gang>>, t: Throwable) {
                Log.d("MIKE", "loadDistributors onFailure: " + t.message.toString())
            }
        })

    }

    private fun View.closeKeyBoard(inputMethodManager: InputMethodManager) {
        inputMethodManager.hideSoftInputFromWindow(windowToken, 0)
    }

    private fun showDatePickerDialog(controlShortName: String){
        val fm: FragmentManager = (activity as AppCompatActivity?)!!.supportFragmentManager
        val datePicker = DatePickerFragment {day, month, year -> onDateSelected(day, month, year, controlShortName) }
        datePicker.show(fm, "datePicker")
    }

    @SuppressLint("SimpleDateFormat")
    private fun onDateSelected(day:Int, month:Int, year:Int, controlShortName: String){
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.YEAR, year)
        calendar.set(Calendar.MONTH, month)
        calendar.set(Calendar.DAY_OF_MONTH, day)
        val sdf2 = SimpleDateFormat("dd/MM/yyyy").format(calendar.time)
        val sdf3 = SimpleDateFormat("yyyy-MM-dd").format(calendar.time)

        if (controlShortName == "startDate"){
            operation.operationStartDate = sdf3
            editTextSearchStartDate.setText(sdf2)
        }else{
            operation.operationEndDate = sdf3
            editTextSearchEndDate.setText(sdf2)
        }

    }

    private fun loadDispatches() {
        val apiInterface = UserApiService.create().getSalesByClientID(operation)
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
                            findNavController().navigate(R.id.action_clientSaleRealizedFragment_to_printActivity, bundle)
                        }

                        override fun cancelSale(model: Operation) {
                            Toast.makeText(globalContext, "Metodo no implementado", Toast.LENGTH_SHORT).show()

                        }
                        override fun cancelPresale(model: Operation) {
                            Toast.makeText(globalContext, "Metodo no implementado", Toast.LENGTH_SHORT).show()

                        }

                        override fun show(model: Operation) {
                            TODO("Not yet implemented")
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
}