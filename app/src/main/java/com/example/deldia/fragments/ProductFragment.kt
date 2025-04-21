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
    private lateinit var autoCompleteWarehouse: AutoCompleteTextView

    private var productList = arrayListOf<Product>()
    private var warehouseList = arrayListOf<Warehouse>()
    private var globalContext: Context? = null
    private var warehouse: Warehouse = Warehouse()
    private var operation: Operation = Operation()

    private lateinit var preference: Preference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        globalContext = this.activity
        preference = Preference(globalContext)
        val bundle = arguments
        warehouse.warehouseID = bundle!!.getInt("vehicleID")
        warehouse.warehouseName = bundle.getString("vehicleLicensePlate").toString()
        
        // Agregar almacén actual a la lista
        warehouseList.add(warehouse)
        
        // Agregar almacén central
        val centralWarehouse = Warehouse()
        centralWarehouse.warehouseID = 3
        centralWarehouse.warehouseName = "A-1 Almacen Central"
        warehouseList.add(centralWarehouse)
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

        setupWarehouseSelector(view)
        setupRecyclerView(view)
        setupSearchView(view)

        val sdf3 = SimpleDateFormat("yyyy-MM-dd").format(Date())
        warehouse.otherDate = sdf3
        operation.operationDate = sdf3
        
        // Cargar productos del almacén inicial
        loadProductStoreInWarehouse(warehouse.warehouseID)
    }

    private fun setupWarehouseSelector(view: View) {
        autoCompleteWarehouse = view.findViewById(R.id.autoCompleteWarehouse)
        
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            warehouseList.map { it.warehouseName }
        )
        
        autoCompleteWarehouse.setAdapter(adapter)
        
        // Seleccionar el almacén actual por defecto
        autoCompleteWarehouse.setText(warehouse.warehouseName, false)
        
        autoCompleteWarehouse.setOnItemClickListener { _, _, position, _ ->
            val selectedWarehouse = warehouseList[position]
            loadProductStoreInWarehouse(selectedWarehouse.warehouseID)
        }
    }

    private fun setupRecyclerView(view: View) {
        recyclerViewStock = view.findViewById(R.id.recyclerViewStock)
        recyclerViewStock.layoutManager = LinearLayoutManager(globalContext)
        recyclerViewStock.setHasFixedSize(true)
        
        productAdapter = ProductAdapter(productList, object : ProductAdapter.OnItemClickListener {
            override fun onItemClick(model: Product) {
                openModal(model)
            }
            override fun keyUp(model: Product, position: Int) {
                // No necesitamos esta funcionalidad
            }
        })
        recyclerViewStock.adapter = productAdapter
    }

    private fun setupSearchView(view: View) {
        searchViewProduct = view.findViewById(R.id.searchViewProduct)
        searchViewProduct.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                val filterList = ArrayList<Product>()
                newText?.let {
                    productList.forEach { product ->
                        if (product.productSaleName.lowercase().contains(newText.lowercase()) ||
                            product.productCode.contains(newText)) {
                            filterList.add(product)
                        }
                    }
                }
                if (filterList.isEmpty()) {
                    Toast.makeText(globalContext, "No se encontraron productos", Toast.LENGTH_SHORT).show()
                }
                productAdapter.getFilter(filterList)
                return true
            }
        })
    }

    private fun loadProductStoreInWarehouse(warehouseId: Int) {
        val warehouseToLoad = Warehouse().apply {
            warehouseID = warehouseId
            otherDate = warehouse.otherDate
        }
        
        val apiInterface = UserApiService.create().getStockInWarehouse(warehouseToLoad)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ responseData ->
                if (responseData != null && responseData.isNotEmpty()) {
                    productList.clear()
                    productList.addAll(responseData)
                    productAdapter.getFilter(productList)
                } else {
                    Toast.makeText(globalContext, "No hay productos en este almacén", Toast.LENGTH_SHORT).show()
                }
            }, { error ->
                Log.e("ProductFragment", "Error loading products: ${error.message}")
                Toast.makeText(globalContext, "Error al cargar productos", Toast.LENGTH_SHORT).show()
            })
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
    }


    private fun openModal(p: Product){
        val inflater = LayoutInflater.from(globalContext)
        val v = inflater.inflate(R.layout.dialog_show_image, null)
        val btnClose = v.findViewById<Button>(R.id.btnClose)
        val imageViewProduct = v.findViewById<ImageView>(R.id.imageViewProduct)
        Picasso.get()
            .load(p.productPath)
            .placeholder(R.drawable.ic_baseline_reorder_24)
            .error(R.drawable.ic_baseline_cancel_24)
            .into(imageViewProduct)
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