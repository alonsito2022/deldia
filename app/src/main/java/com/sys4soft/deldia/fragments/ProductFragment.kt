package com.sys4soft.deldia.fragments

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
import com.sys4soft.deldia.R
import com.sys4soft.deldia.adapter.ProductAdapter
import com.sys4soft.deldia.localdatabase.Preference
import com.sys4soft.deldia.models.*
import com.sys4soft.deldia.retrofit.UserApiService
import com.squareup.picasso.Picasso
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.text.SimpleDateFormat
import java.util.*


class ProductFragment : Fragment() {

    private lateinit var recyclerViewStock: RecyclerView
    private lateinit var searchViewProduct: SearchView
    private lateinit var productAdapter: ProductAdapter
    private lateinit var autoCompleteWarehouse: AutoCompleteTextView
    private lateinit var btnRefreshStock: com.google.android.material.button.MaterialButton

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
        
        val userRoleName = preference.getData("userRoleName")
        
        if (!userRoleName.equals("ADMINISTRADOR", ignoreCase = true)) {
            // Agregar almacén actual a la lista
            warehouseList.add(warehouse)
        }
        
        // Agregar almacén central (para todos)
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
        
        val userRoleName = preference.getData("userRoleName")
        if (userRoleName.equals("ADMINISTRADOR", ignoreCase = true)) {
            loadWarehousesBySubsidiary()
        } else {
            // Cargar productos del almacén inicial
            loadProductStoreInWarehouse(warehouse.warehouseID)
        }
    }

    private fun loadWarehousesBySubsidiary() {
        val subsidiaryIdStr = preference.getData("subsidiaryID")
        val subsidiaryID = if (subsidiaryIdStr.isNotEmpty()) subsidiaryIdStr.toInt() else 1
        
        val warehouseParams = Warehouse().apply {
            this.subsidiaryID = subsidiaryID
        }
        
        UserApiService.create(requireContext()).getWarehousesBySubsidiary(warehouseParams)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ responseData ->
                if (responseData != null) {
                    warehouseList.clear()
                    
                    // Almacén central (siempre incluir para administradores)
                    val centralWarehouse = Warehouse().apply {
                        warehouseID = 3
                        warehouseName = "A-1 Almacen Central"
                    }
                    warehouseList.add(centralWarehouse)
                    
                    // Filtrar por gangStatus == true y evitar duplicado del central
                    val filteredWarehouses = responseData.filter { it.gang?.gangStatus == true && it.warehouseID != 3 }
                    warehouseList.addAll(filteredWarehouses)
                    
                    // Actualizar el adapter del AutoCompleteTextView
                    updateWarehouseAdapter()
                }
            }, { error ->
                Log.e("ProductFragment", "Error loading warehouses: ${error.message}")
            })
    }

    private fun updateWarehouseAdapter() {
        if (::autoCompleteWarehouse.isInitialized) {
            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                warehouseList.map { it.warehouseName }
            )
            autoCompleteWarehouse.setAdapter(adapter)
            
            // Si es ADMINISTRADOR y la lista no está vacía, y no tenemos un almacén válido aún
            if (warehouseList.isNotEmpty()) {
                val userRoleName = preference.getData("userRoleName")
                if (userRoleName.equals("ADMINISTRADOR", ignoreCase = true)) {
                    // Si el almacén actual no está en la lista (ID es 0 o similar), seleccionar el primero
                    if (!warehouseList.any { it.warehouseID == warehouse.warehouseID }) {
                        warehouse = warehouseList[0]
                        autoCompleteWarehouse.setText(warehouse.warehouseName, false)
                    }
                    // Siempre cargar productos después de actualizar la lista si es ADMINISTRADOR
                    loadProductStoreInWarehouse(warehouse.warehouseID)
                }
            }
        }
    }

    private fun setupWarehouseSelector(view: View) {
        autoCompleteWarehouse = view.findViewById(R.id.autoCompleteWarehouse)
        btnRefreshStock = view.findViewById(R.id.btnRefreshStock)
        
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
            warehouse = selectedWarehouse // Actualizar el almacén seleccionado
            loadProductStoreInWarehouse(selectedWarehouse.warehouseID)
        }
        
        // Configurar botón de actualizar
        btnRefreshStock.setOnClickListener {
            // Recargar productos del almacén actualmente seleccionado
            loadProductStoreInWarehouse(warehouse.warehouseID)
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
        
        val apiInterface = UserApiService.create(requireContext()).getStockInWarehouse(warehouseToLoad)
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