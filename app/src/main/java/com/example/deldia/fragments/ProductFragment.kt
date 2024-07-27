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
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.deldia.R
import com.example.deldia.adapter.PersonAdapter
import com.example.deldia.adapter.ProductAdapter
import com.example.deldia.localdatabase.Preference
import com.example.deldia.models.*
import com.example.deldia.retrofit.UserApiService
import com.google.android.material.textfield.TextInputEditText
import com.squareup.picasso.Picasso
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*


class ProductFragment : Fragment() {

    private lateinit var recyclerViewStock: RecyclerView
    private lateinit var searchViewProduct: SearchView
    private lateinit var productAdapter: ProductAdapter
    private lateinit var editTextSearchDate: TextInputEditText
    private lateinit var btnSearch: Button
    private lateinit var btnSaveCheckStock: Button
    private lateinit var textViewGangName: TextView

    private var productList = arrayListOf<Product>()

    private var globalContext: Context? = null
    private var vehicleID: Int = 0
    private var vehicleLicensePlate: String = ""
    private var warehouse: Warehouse = Warehouse()
    private var operation: Operation = Operation()

    private lateinit var preference: Preference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        globalContext = this.activity
        preference = Preference(globalContext)
        val bundle = arguments
        vehicleID = bundle!!.getInt("vehicleID")
        vehicleLicensePlate = bundle.getString("vehicleLicensePlate").toString()
        warehouse.warehouseID = vehicleID
        warehouse.warehouseName = vehicleLicensePlate
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_product, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        textViewGangName = view.findViewById(R.id.textViewGangName)
        textViewGangName.text = "CUADRILLA:" + preference.getData("gangName")

        recyclerViewStock = view.findViewById(R.id.recyclerViewStock)
        recyclerViewStock.layoutManager = LinearLayoutManager(globalContext)
        recyclerViewStock.setHasFixedSize(true)
        productAdapter = ProductAdapter(productList, object : ProductAdapter.OnItemClickListener {
            override fun onItemClick(model: Product) {
                openModal(model)
//                            recovery.clientID = model.id!!
//                            recovery.clientName = model.names

            }
            override fun keyUp(model: Product, position: Int) {
//                            recyclerViewStock.adapter?.notifyItemChanged(position)
            }

        })
        recyclerViewStock.adapter = productAdapter
        searchViewProduct = view.findViewById(R.id.searchViewProduct)
        searchViewProduct.setOnQueryTextListener(object : SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                val filterList: ArrayList<Product> = ArrayList()

                newText?.let {
                    productList.forEachIndexed{ _, model ->
                        if(model.productSaleName.lowercase().contains(newText.lowercase())){
                            filterList.add(model)
                        }
                    }
                }
                if(filterList.isEmpty()){
                    Toast.makeText(globalContext, "No data found", Toast.LENGTH_LONG).show()
                }else{
                    productAdapter.getFilter(filterList)
                }
                return true
            }

        })
        btnSaveCheckStock = view.findViewById(R.id.btnSaveCheckStock)
        btnSaveCheckStock.setOnClickListener{
            btnSaveCheckStock.isEnabled = false
            operation.userID = preference.getData("userID").toInt()
            operation.warehouseID = warehouse.warehouseID
            operation.details.clear()

            productList.forEach {
                if(it.stockChecked>0){
                    val detail: Operation.OperationDetail = Operation.OperationDetail()
                    detail.productTariffID = it.productTariffID
                    detail.stockChecked = it.stockChecked
                    operation.details.add(detail)
                }
            }
            sendInventoryCheck()

        }

        btnSearch = view.findViewById(R.id.btnSearch)
        btnSearch.setOnClickListener{
            if(warehouse.otherDate.count() > 0){
                btnSearch.isEnabled = false
                loadStockInWarehouse(warehouse)
            }
            else
                Toast.makeText(globalContext, "Elija fecha.", Toast.LENGTH_SHORT).show()
        }
        editTextSearchDate = view.findViewById(R.id.editTextSearchDate)

        val sdf2 = SimpleDateFormat("dd/MM/yyyy").format(Date())
        val sdf3 = SimpleDateFormat("yyyy-MM-dd").format(Date())
        warehouse.otherDate = sdf3
        operation.operationDate = sdf3
        editTextSearchDate.setText(sdf2)
        editTextSearchDate.setOnClickListener { showDatePickerDialog() }

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
        warehouse.otherDate = sdf3
        operation.operationDate = sdf3
        editTextSearchDate.setText(sdf2)
    }

    private fun sendInventoryCheck(){
        val size = productList.size
        productList.clear()
        recyclerViewStock.adapter?.notifyItemRangeRemoved(0, size)

        val apiInterface = UserApiService.create().sendInventoryCheck(operation)
        apiInterface.enqueue(object : Callback<Operation> {
            override fun onResponse(call: Call<Operation>, response: Response<Operation>) {
                operation = response.body()!!
                Toast.makeText(globalContext, operation.messageStatus, Toast.LENGTH_LONG).show()
                loadStockInWarehouse(warehouse)
                btnSaveCheckStock.isEnabled = true
            }

            override fun onFailure(call: Call<Operation>, t: Throwable) {
                Log.d("MIKE", "sendInventoryCheck onFailure: " + t.message.toString())

            }
        })
    }

    private fun loadStockInWarehouse(w: Warehouse) {
        val apiInterface = UserApiService.create().getStockInWarehouse(w)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ responseData ->
                Log.d("Mike", responseData?.size.toString())
                if(responseData != null && responseData.isNotEmpty()){
                    productList = responseData
                    productAdapter.getFilter(productList)
                    btnSearch.isEnabled = true
                }
            }, { error ->
                Log.d("Mike", error.toString())
            })

    }

    private fun openModal(p: Product){
        val inflater = LayoutInflater.from(globalContext)
        val v = inflater.inflate(R.layout.dialog_show_image, null)
        val btnClose = v.findViewById<Button>(R.id.btnClose)
        val imageViewProduct = v.findViewById<ImageView>(R.id.imageViewProduct)
        Picasso.get().load(p.productPath).into(imageViewProduct)
        val addDialog = AlertDialog.Builder(globalContext)
        addDialog.setView(v)
        addDialog.setTitle(p.productSaleName)
        val dialog: AlertDialog = addDialog.create()
        dialog.show()
        btnClose.setOnClickListener{
            dialog.dismiss()
        }
    }
}