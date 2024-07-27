package com.example.deldia.fragments

import android.annotation.SuppressLint
import android.app.AlertDialog
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
import androidx.core.view.isGone
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.deldia.R
import com.example.deldia.adapter.*
import com.example.deldia.localdatabase.Preference
import com.example.deldia.models.*
import com.example.deldia.retrofit.UserApiService
import com.google.android.material.textfield.TextInputEditText
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class CashFragment : Fragment() {

    private var globalContext: Context? = null
    private var vehicleID: Int = 0
    private var userID: Int = 0

    private var listCashes = arrayListOf<Cash>()
    private var listCashFlows = arrayListOf<CashFlow>()
    private var cash: Cash = Cash()
    private var cashFlow: CashFlow = CashFlow()
    private var user: User = User()

    private lateinit var recyclerViewCashes: RecyclerView
    private lateinit var recyclerViewCashFlow: RecyclerView
    private lateinit var autoCompleteCash: AutoCompleteTextView
    private lateinit var editTextSearchDate: TextInputEditText
    private lateinit var btnCleanCash: Button
    private lateinit var btnSearchCashFlow: Button
    private lateinit var btnNewCashFlow: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        globalContext = this.activity
        val bundle = arguments
        vehicleID = bundle!!.getInt("vehicleID")
        userID = bundle.getInt("userID")
        user.userID = userID
        loadCashes()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_cash, container, false)
        autoCompleteCash = view.findViewById(R.id.autoCompleteCash)

        editTextSearchDate = view.findViewById(R.id.editTextSearchDate)


        val sdf2 = SimpleDateFormat("dd/MM/yyyy").format(Date())
        val sdf3 = SimpleDateFormat("yyyy-MM-dd").format(Date())
        cashFlow.transactionDate = sdf3
        editTextSearchDate.setText(sdf2)

        editTextSearchDate.setOnClickListener { showDatePickerDialog() }

        btnNewCashFlow = view.findViewById(R.id.btnNewCashFlow)
        btnNewCashFlow.setOnClickListener{

            if(cashFlow.cashID > 0){
                val inflater = LayoutInflater.from(globalContext)
                val v = inflater.inflate(R.layout.dialog_new_cash_flow, null)

                val textViewCashName = v.findViewById<TextView>(R.id.textViewCashName)
                val textViewCashBalance = v.findViewById<TextView>(R.id.textViewCashBalance)
                val radioGroupTypeOperation = v.findViewById<RadioGroup>(R.id.radioGroupTypeOperation)
                val radioButtonEntrance = v.findViewById<RadioButton>(R.id.radioButtonEntrance)
                val radioButtonDeparture = v.findViewById<RadioButton>(R.id.radioButtonDeparture)
                val radioButtonDeposit = v.findViewById<RadioButton>(R.id.radioButtonDeposit)
                val radioButtonRetire = v.findViewById<RadioButton>(R.id.radioButtonRetire)
                val radioButtonDepositYape = v.findViewById<RadioButton>(R.id.radioButtonDepositYape)
                val radioButtonRetireYape = v.findViewById<RadioButton>(R.id.radioButtonRetireYape)
                val editTextDescription = v.findViewById<EditText>(R.id.editTextDescription)
                val editTextTotal = v.findViewById<EditText>(R.id.editTextTotal)

                textViewCashName.text = cashFlow.cashName
                textViewCashBalance.text = cashFlow.cashBalance.toString()

                if (cashFlow.cashType == "C"){
                    radioButtonEntrance.visibility = View.VISIBLE
                    radioButtonDeparture.visibility = View.VISIBLE

                    radioButtonDeposit.visibility = View.GONE
                    radioButtonRetire.visibility = View.GONE
                    radioButtonDepositYape.visibility = View.GONE
                    radioButtonRetireYape.visibility = View.GONE
                    cashFlow.type="E"
                    radioButtonEntrance.isChecked = true
                    radioButtonDeposit.isChecked = false
                }else{
                    radioButtonEntrance.visibility = View.GONE
                    radioButtonDeparture.visibility = View.GONE

                    radioButtonDeposit.visibility = View.VISIBLE
                    radioButtonRetire.visibility = View.VISIBLE
                    radioButtonDepositYape.visibility = View.VISIBLE
                    radioButtonRetireYape.visibility = View.VISIBLE
                    cashFlow.type="D"
                    radioButtonEntrance.isChecked = false
                    radioButtonDeposit.isChecked = true
                }

                radioGroupTypeOperation.setOnCheckedChangeListener{ _, checkedId ->
                    when (checkedId) {
                        R.id.radioButtonEntrance -> {
                            cashFlow.type="E"
                        }
                        R.id.radioButtonDeparture -> {
                            cashFlow.type="S"
                        }
                        R.id.radioButtonDeposit -> {
                            cashFlow.type="D"
                        }
                        R.id.radioButtonRetire -> {
                            cashFlow.type="R"
                        }
                        R.id.radioButtonDepositYape -> {
                            cashFlow.type="DY"
                        }
                        R.id.radioButtonRetireYape -> {
                            cashFlow.type="RY"
                        }
                        else -> {}
                    }
                }

                val dialogClose = v.findViewById<Button>(R.id.dialog_close)
                val dialogSave = v.findViewById<Button>(R.id.dialog_save)
                val addDialog = AlertDialog.Builder(globalContext)
                addDialog.setView(v)
                addDialog.setTitle("NUEVA OPERACION")

                val dialog: AlertDialog = addDialog.create()
                dialog.show()
                dialogClose.setOnClickListener{
                    dialog.dismiss()
                }
                dialogSave.setOnClickListener{

                    cashFlow.total = editTextTotal.text.toString().toDouble()
                    cashFlow.description = editTextDescription.text.toString().uppercase()

                    if(editTextTotal.text.isNotEmpty()){
                        if(editTextDescription.text.isNotEmpty()){
                            saveCashFlow()
                        }else Toast.makeText(globalContext, "Verificar descripcion.", Toast.LENGTH_SHORT).show()
                    }else Toast.makeText(globalContext, "Verificar pago.", Toast.LENGTH_SHORT).show()

                    dialog.dismiss()
                }
            }else{
                Toast.makeText(globalContext, "Elija caja.", Toast.LENGTH_SHORT).show()

            }

        }

        btnCleanCash = view.findViewById(R.id.btnCleanCash)
        btnCleanCash.setOnClickListener{
            autoCompleteCash.setText("")
            loadCashes()
        }
        btnSearchCashFlow = view.findViewById(R.id.btnSearchCashFlow)
        btnSearchCashFlow.setOnClickListener{
            if(cashFlow.cashID > 0)
                loadCashFlows()
            else
                Toast.makeText(globalContext, "Elija caja.", Toast.LENGTH_SHORT).show()
        }

        recyclerViewCashFlow = view.findViewById(R.id.recyclerViewCashFlow)
        recyclerViewCashes = view.findViewById(R.id.recyclerViewCashes)
        return view
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
        cashFlow.transactionDate = sdf3
        editTextSearchDate.setText(sdf2)
    }


    private fun loadCashes(){

        val apiInterface = UserApiService.create().getCashes(user)
        apiInterface.enqueue(object : Callback<ArrayList<Cash>> {
            override fun onResponse(call: Call<ArrayList<Cash>>, response: Response<ArrayList<Cash>>) {
                listCashes = response.body()!!

                val layoutManager = LinearLayoutManager(globalContext)
                layoutManager.orientation = LinearLayoutManager.HORIZONTAL
                recyclerViewCashes.layoutManager = layoutManager
                recyclerViewCashes.setHasFixedSize(true)
                recyclerViewCashes.adapter= CashBalanceAdapter( listCashes,  object:CashBalanceAdapter.OnItemClickListener{
                    override fun onItemClick(model: Cash) {
                        Toast.makeText(globalContext, "CASH: ${model.name}", Toast.LENGTH_SHORT).show()

                    }
                } )


                autoCompleteCash.setAdapter(CashAdapter(globalContext!!, R.layout.item_cash_view, listCashes, object : CashAdapter.OnItemClickListener{
                    override fun onItemClick(model: Cash) {
                        autoCompleteCash.setText(model.name)
                        autoCompleteCash.dismissDropDown()
                        val inputManager = context!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        autoCompleteCash.closeKeyBoard(inputManager)
                        autoCompleteCash.clearFocus()
                        cashFlow.cashID = model.cashID
                        cashFlow.cashName = model.name
                        cashFlow.cashType = model.type
                        cashFlow.cashBalance = model.balance

                    }
                }))
                Log.d("MIKE", "listCashes ok: " + listCashes.size)
            }

            override fun onFailure(call: Call<ArrayList<Cash>>, t: Throwable) {
                Log.d("MIKE", "loadCashes onFailure: " + t.message.toString())
            }
        })

    }

    private fun loadCashFlows() {
        val apiInterface = UserApiService.create().getCashFlow(cashFlow)
        apiInterface.enqueue(object : Callback<ArrayList<CashFlow>> {
            override fun onResponse(
                call: Call<ArrayList<CashFlow>>,
                response: Response<ArrayList<CashFlow>>
            ) {
                if (response.body() != null) {
                    listCashFlows = response.body()!!
                    recyclerViewCashFlow.layoutManager = LinearLayoutManager(globalContext)
                    recyclerViewCashFlow.setHasFixedSize(true)
                    recyclerViewCashFlow.adapter= CashFlowAdapter( listCashFlows )
                }
            }
            override fun onFailure(call: Call<ArrayList<CashFlow>>, t: Throwable) {
                Log.d("MIKE", "loadCashFlows. Algo salio mal..." + t.message.toString())
            }
        })
    }

    private fun saveCashFlow() {
        val apiInterface = UserApiService.create().saveCashFlow(cashFlow)
        apiInterface.enqueue(object : Callback<ArrayList<CashFlow>> {
            override fun onResponse(
                call: Call<ArrayList<CashFlow>>,
                response: Response<ArrayList<CashFlow>>
            ) {
                if (response.body() != null) {
                    listCashFlows = response.body()!!
                    recyclerViewCashFlow.layoutManager = LinearLayoutManager(globalContext)
                    recyclerViewCashFlow.setHasFixedSize(true)
                    recyclerViewCashFlow.adapter= CashFlowAdapter( listCashFlows )
                }
            }
            override fun onFailure(call: Call<ArrayList<CashFlow>>, t: Throwable) {
                Log.d("MIKE", "loadCashFlows. Algo salio mal..." + t.message.toString())
            }
        })
    }

    private fun View.closeKeyBoard(inputMethodManager: InputMethodManager) {
        inputMethodManager.hideSoftInputFromWindow(windowToken, 0)
    }
}