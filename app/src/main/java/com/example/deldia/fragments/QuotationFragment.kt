package com.example.deldia.fragments

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.widget.SwitchCompat
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.deldia.R
import com.example.deldia.adapter.ProductQuotationAdapter
import com.example.deldia.localdatabase.Preference
import com.example.deldia.models.Operation
import com.example.deldia.models.Product
import com.example.deldia.models.Warehouse
import com.example.deldia.models.Person
import com.example.deldia.retrofit.UserApiService
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.squareup.picasso.Picasso
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


class QuotationFragment : Fragment() {

    private lateinit var textViewClientNames: TextView
    private lateinit var textViewClientAddress: TextView
    private lateinit var searchViewProduct: SearchView
    private lateinit var recyclerViewProductStore: RecyclerView
    private lateinit var productQuotationAdapter: ProductQuotationAdapter
    private lateinit var buttonSummary: MaterialButton

    private lateinit var textViewTotal: TextView
    private lateinit var textViewItems: TextView

    private lateinit var textViewDialogTotal: TextView
    private lateinit var textViewDialogItems: TextView
    private lateinit var autoCompletePhysicalDistribution: AutoCompleteTextView

    private lateinit var switchShowImages: SwitchCompat
    private lateinit var buttonRefresh: Button

    private var globalContext: Context? = null

    private var warehouse: Warehouse = Warehouse()
    private var person: Person = Person()
    private var operation: Operation = Operation()
    private var list = arrayListOf<Product>()
    private lateinit var preference: Preference

    private lateinit var layoutListItem: LinearLayout

    private lateinit var loadingLayout: FrameLayout
    private lateinit var currentDialog: AlertDialog
    private lateinit var dialogClose: Button
    private lateinit var dialogSave: Button
    private var isProcessing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        globalContext = this.activity
        preference = Preference(globalContext)

        initializeData(arguments)
        loadProductStoreInWarehouse()

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_quotation, container, false)
    }

    private fun setupUI(view: View) {
        textViewClientNames = view.findViewById(R.id.textViewClientNames)
        textViewClientAddress = view.findViewById(R.id.textViewClientAddress)
        textViewTotal = view.findViewById(R.id.textViewTotal)
        textViewItems = view.findViewById(R.id.textViewItems)
        buttonSummary = view.findViewById(R.id.buttonSummary)
        buttonRefresh = view.findViewById(R.id.buttonRefresh)
        switchShowImages = view.findViewById(R.id.switchShowImages)
        searchViewProduct = view.findViewById(R.id.searchViewProduct)
        recyclerViewProductStore = view.findViewById(R.id.recyclerViewProductStore)

        textViewClientNames.text = person.fullName
        textViewClientAddress.text = person.address

        setupRecyclerView()
        setupSearchView()
        setupListeners()
    }
    private fun setupRecyclerView() {
        recyclerViewProductStore.layoutManager = LinearLayoutManager(requireContext())
        recyclerViewProductStore.setHasFixedSize(true)
        productQuotationAdapter = ProductQuotationAdapter(list).apply {
            setOnQuantityChangeListener(object : ProductQuotationAdapter.OnQuantityChangeListener {
                override fun onQuantityChanged(id: Int, quantity: String) {
                    updateProductQuantity(id, quantity)
                }
                override fun onItemClick(id: Int) {
                    list.find { it.productID == id }?.let { openModal(it) }
                }
            })
        }
        recyclerViewProductStore.adapter = productQuotationAdapter
    }
    private fun setupSearchView() {
        searchViewProduct.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                filterProducts(newText)
                return true
            }
        })
    }
    private fun setupListeners() {
        switchShowImages.setOnCheckedChangeListener { _, isChecked ->
            list.forEach { it.showImage = isChecked }
            recyclerViewProductStore.adapter?.notifyDataSetChanged()
        }
        buttonRefresh.setOnClickListener { loadProductStoreInWarehouse() }
        buttonSummary.setOnClickListener { showSummaryDialog() }
    }
    private fun updateProductQuantity(id: Int, quantity: String) {
        list.find { it.productID == id }?.let { product ->
            val newQuantity = quantity.toIntOrNull()?.takeIf { it <= product.stock } ?: 0
            product.quantity = newQuantity
            product.subtotal = newQuantity * product.priceSale
            updateTotal()
        }
    }
    private fun filterProducts(query: String?) {
        val filteredList = list.filter {
            it.productSaleName.contains(query ?: "", ignoreCase = true) ||
                    it.productCode.toString().contains(query ?: "")
        } as ArrayList<Product>
        productQuotationAdapter.getFilter(filteredList)
    }
    private fun showSummaryDialog() {
        if (list.none { it.quantity > 0 }) {
            Toast.makeText(requireContext(), "No hay productos en la cotización.", Toast.LENGTH_SHORT).show()
            return
        }
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_confirm_quotation, null)
        val dialog = AlertDialog.Builder(requireContext()).setView(dialogView).setTitle("Resumen de Preventa").create()

        val btnClose = dialogView.findViewById<Button>(R.id.dialog_close)
        val btnSave = dialogView.findViewById<Button>(R.id.dialog_save)
        btnClose.setOnClickListener { dialog.dismiss() }
        btnSave.setOnClickListener {
            if (textViewTotal.text.toString().toDoubleOrNull() ?: 0.0 > 0) {
                registerQuotation()
                Toast.makeText(requireContext(), "Cotización registrada.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Verificar cotización.", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }
        dialog.show()
    }
    private fun registerQuotation() {
        operation.apply {
            clientID = person.personID
            warehouseID = warehouse.warehouseID
            documentType = "05"
            details = list.filter { it.quantity > 0 }.map {
                Operation.OperationDetail(it.productTariffID, it.priceSale, it.quantity)
            }.toMutableList()
        }
        sendApiQuotation()
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
//        setupUI(view)
        switchShowImages = view.findViewById(R.id.switchShowImages)
        switchShowImages.setOnCheckedChangeListener { _, isChecked ->
            list.forEach { item ->item.showImage=isChecked}
            recyclerViewProductStore.adapter?.notifyDataSetChanged()
        }

        textViewClientNames = view.findViewById(R.id.textViewClientNames)
        textViewClientAddress = view.findViewById(R.id.textViewClientAddress)
        textViewClientNames.text = person.fullName
        textViewClientAddress.text = person.address
        recyclerViewProductStore = view.findViewById(R.id.recyclerViewProductStore)
        recyclerViewProductStore.layoutManager = LinearLayoutManager(globalContext)
        recyclerViewProductStore.setHasFixedSize(true)

        productQuotationAdapter = ProductQuotationAdapter(list)
        productQuotationAdapter.setOnQuantityChangeListener(object : ProductQuotationAdapter.OnQuantityChangeListener{
            override fun onQuantityChanged(id: Int, quantity: String) {
                val pos = list.indexOfFirst { it.productID == id }

                if (pos != -1) {
                    var newSubtotal = 0.0
                    var newQuantity = 0
                    val product = list[pos]
                    if(quantity.isNotBlank()){
                        if(quantity.toInt() <= product.stock){
                            newQuantity = quantity.toInt()
                            newSubtotal = quantity.toInt() * product.priceSale
                        }
                    }
                    product.quantity = newQuantity
                    product.subtotal = newSubtotal
                    updateTotal()
                }
            }
            override fun onItemClick(id: Int) {
                val pos = list.indexOfFirst { it.productID == id }
                openModal(list[pos])
            }
        })

        recyclerViewProductStore.adapter= productQuotationAdapter
        textViewTotal = view.findViewById(R.id.textViewTotal)
        buttonRefresh = view.findViewById(R.id.buttonRefresh)
        buttonRefresh.setOnClickListener {
            loadProductStoreInWarehouse()
        }
        textViewItems = view.findViewById(R.id.textViewItems)
        searchViewProduct = view.findViewById(R.id.searchViewProduct)
        searchViewProduct.setOnQueryTextListener(object : SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(query: String?): Boolean {
                //                searchViewProduct.clearFocus()
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                val filterList: ArrayList<Product> = ArrayList()
                newText?.let {
                    list.forEachIndexed{ _, product ->
                        if(product.productSaleName.lowercase().contains(newText.lowercase()) or product.productCode.contains(newText)){
                            filterList.add(product)
                        }
                    }
                }
                if(filterList.isEmpty()){
                    Toast.makeText(globalContext, "No data found", Toast.LENGTH_LONG).show()
                }else{
                    productQuotationAdapter.getFilter(filterList)
                }
                return true
            }
        })

        buttonSummary = view.findViewById(R.id.buttonSummary)
        buttonSummary.setOnClickListener {
            addInfo()
            updateTotal()
        }
    }

    private fun initializeData(bundle: Bundle?) {
        bundle?.let {
            operation.userID = it.getInt("userID")
            warehouse.apply {
                warehouseID = 3
                warehouseName = it.getString("vehicleLicensePlate", "")
            }
            person.apply {
                personID = it.getInt("personID")
                fullName = it.getString("personFullName", "")
                address = it.getString("personAddress", "")
                documentNumber = it.getString("personDocumentNumber", "")
                documentType = it.getString("personDocumentType", "")
                physicalDistribution = it.getString("physicalDistribution", "")
                physicalDistributionDisplay = it.getString("physicalDistributionDisplay", "")
            }
            operation.routeDate = it.getString("routeDate", "")
        }
    }


    private fun loadPhysicalDistribution(){
        val listPhysicalDistribution = listOf("TIRAS", "SUELTAS", "NO APLICA")
        val adapter = ArrayAdapter(
            globalContext!!,
            android.R.layout.simple_spinner_dropdown_item,
            listPhysicalDistribution
        )
        autoCompletePhysicalDistribution.keyListener = null
        autoCompletePhysicalDistribution.setAdapter(adapter)
        autoCompletePhysicalDistribution.setText(
            adapter.getItem(listPhysicalDistribution.indexOf(person.physicalDistributionDisplay)).toString(),
            false
        )
        autoCompletePhysicalDistribution.isEnabled = false
    }
    private fun updateTotal() {
        var counter = 0
        var total = 0.0
        list.forEach {
            counter += it.quantity
            total += it.quantity * it.priceSale
        }
        val totalF3:Double = Math.round(total * 1000.0) / 1000.0
        val totalF1:Double = Math.round(totalF3 * 100.0) / 100.0
        textViewTotal.text = totalF1.toString()
        textViewItems.text = counter.toString()
        if(this::textViewDialogTotal.isInitialized){
            textViewDialogTotal.text = totalF1.toString()
            textViewDialogItems.text = counter.toString()
        }
    }

    private fun loadProductStoreInWarehouse() {
        UserApiService.create().getStockInWarehouse(warehouse)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ responseData ->
                responseData?.let {
                    list.clear()
                    list.addAll(it)
                    productQuotationAdapter.getFilter(list)
                }
            }, { error -> Log.e("QuotationFragment", "Error loading products: ${'$'}{error.message}") })
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

    private fun showLoading() {
        loadingLayout.visibility = View.VISIBLE
        dialogClose.isEnabled = false
        dialogSave.isEnabled = false
    }

    private fun hideLoading() {
        loadingLayout.visibility = View.GONE
        dialogClose.isEnabled = true
        dialogSave.isEnabled = true
    }

    private fun addInfo(){
        val inflater = LayoutInflater.from(globalContext)
        val v = inflater.inflate(R.layout.dialog_confirm_quotation, null)
        layoutListItem = v.findViewById(R.id.layoutList)
        textViewDialogTotal = v.findViewById(R.id.textViewDialogTotal)
        textViewDialogItems = v.findViewById(R.id.textViewDialogItems)
        autoCompletePhysicalDistribution = v.findViewById(R.id.autoCompletePhysicalDistribution)
        loadingLayout = v.findViewById(R.id.loadingLayout)
        loadPhysicalDistribution()
        list.forEach { item ->

            if (item.quantity>0){
                val inflaterOperationDetail = LayoutInflater.from(globalContext)
                val viewOperationDetail = inflaterOperationDetail.inflate(R.layout.item_operation_detail_view, null)

                val btnRemove = viewOperationDetail.findViewById<Button>(R.id.buttonRemoveItem)
                val btnPlus = viewOperationDetail.findViewById<Button>(R.id.buttonPlus)
                val btnMinus = viewOperationDetail.findViewById<Button>(R.id.buttonMinus)
                val textViewProductName = viewOperationDetail.findViewById<TextView>(R.id.textViewProductName)
                val textViewQuantity = viewOperationDetail.findViewById<TextView>(R.id.textViewQuantity)
                val textViewSku = viewOperationDetail.findViewById<TextView>(R.id.textViewSku)
                val editTextPrice = viewOperationDetail.findViewById<EditText>(R.id.editTextPrice)

                textViewProductName.text = item.productSaleName
                textViewSku.text = item.productSku
                textViewQuantity.text = item.quantity.toString()
                editTextPrice?.setText(item.priceSale.toString())
                layoutListItem.addView(viewOperationDetail)


                editTextPrice.setOnKeyListener(View.OnKeyListener {v, keycode, event ->
                    if (event.action == KeyEvent.ACTION_UP){
                        val textPrice = editTextPrice.text.toString()
                        if (textPrice.isNotEmpty() && textPrice.toDouble() > 0){
                            item.priceSale = textPrice.toDouble()
                            updateTotal()
                        }
                    }
                    false
                })

                btnRemove.setOnClickListener {
                    layoutListItem.removeView(viewOperationDetail)
                    item.quantity = 0
                    updateTotal()
//                    updateFirebaseItem(item)
                    recyclerViewProductStore.adapter?.notifyItemChanged(item.positionInAdapter)

                }

                btnPlus.setOnClickListener {
                    if (item.quantity < item.stock) {
                        item.quantity += 1
                        textViewQuantity.text = item.quantity.toString()
                        updateTotal()
//                    updateFirebaseItem(item)
                        recyclerViewProductStore.adapter?.notifyItemChanged(item.positionInAdapter)
                    }

                }

                btnMinus.setOnClickListener {
                    if (item.quantity >= 1) {
                        item.quantity -= 1
                        textViewQuantity.text = item.quantity.toString()
                        updateTotal()
//                        updateFirebaseItem(item)
                        recyclerViewProductStore.adapter?.notifyItemChanged(item.positionInAdapter)
                    }

                }
            }

        }

        dialogClose = v.findViewById(R.id.dialog_close)
        dialogSave = v.findViewById(R.id.dialog_save)

        val addDialog = AlertDialog.Builder(globalContext)
        addDialog.setView(v)
        addDialog.setTitle("RESUMEN DE PREVENTA")
        addDialog.setCancelable(false)

        currentDialog = addDialog.create()
        currentDialog.show()

        dialogClose.setOnClickListener{
            if (!isProcessing) {
                currentDialog.dismiss()
            }
        }

        dialogSave.setOnClickListener{
            if (isProcessing) return@setOnClickListener

            val totalSale = textViewTotal.text.toString().toDouble()
            if (totalSale > 0) {
                isProcessing = true
                showLoading()
                registerRestQuotation()
            } else {
                Toast.makeText(globalContext, "Verificar cotización.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun validateQuotation(): Boolean {
        // Validar que haya al menos un producto seleccionado
        if (list.none { it.quantity > 0 }) {
            Toast.makeText(globalContext, "Debe seleccionar al menos un producto", Toast.LENGTH_SHORT).show()
            return false
        }

        // Validar que todos los productos tengan precio válido
        val invalidPrices = list.filter { it.quantity > 0 && it.priceSale <= 0 }
        if (invalidPrices.isNotEmpty()) {
            Toast.makeText(globalContext, "Hay productos con precios inválidos", Toast.LENGTH_SHORT).show()
            return false
        }

        // Validar que no se exceda el stock disponible
        val exceedingStock = list.filter { it.quantity > it.stock }
        if (exceedingStock.isNotEmpty()) {
            Toast.makeText(globalContext, "La cantidad excede el stock disponible", Toast.LENGTH_SHORT).show()
            return false
        }

        // Validar que el cliente esté seleccionado
        if (person.personID <= 0) {
            Toast.makeText(globalContext, "Debe seleccionar un cliente", Toast.LENGTH_SHORT).show()
            return false
        }

        // Validar que el almacén esté seleccionado
        if (warehouse.warehouseID <= 0) {
            Toast.makeText(globalContext, "Debe seleccionar un almacén", Toast.LENGTH_SHORT).show()
            return false
        }

        // Validar que el tipo de entrega esté seleccionado
        if (autoCompletePhysicalDistribution.text.isNullOrEmpty()) {
            Toast.makeText(globalContext, "Debe seleccionar el tipo de entrega", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private fun registerRestQuotation(){
        if (!validateQuotation()) {
            return
        }

        when(autoCompletePhysicalDistribution.text.toString()){
            "TIRAS"->operation.physicalDistribution="01"
            "SUELTAS"->operation.physicalDistribution="02"
            "NO APLICA"->operation.physicalDistribution="NA"
        }

        operation.clientID = person.personID
        operation.warehouseID = warehouse.warehouseID
        operation.documentType = "05"

        list.forEach {
            if(it.quantity>0){
                val detail: Operation.OperationDetail = Operation.OperationDetail()
                detail.productTariffID = it.productTariffID
                detail.price = it.priceSale
                detail.quantity = it.quantity

                operation.details.add(detail)
            }
        }
        if (operation.userID>0)
            sendApiQuotation()
        else
            Toast.makeText(globalContext, "Verificar user", Toast.LENGTH_LONG).show()

    }

    private fun sendApiQuotation(){
        val apiInterface = UserApiService.create().sendQuotationData(operation)
        apiInterface.enqueue(object : Callback<Operation>{
            override fun onResponse(call: Call<Operation>, response: Response<Operation>) {
                if (response.body() != null) {
                    operation = response.body()!!
                    val bundle = arguments

                    bundle!!.putInt("userID", operation.userID)
                    bundle.putInt("vehicleID", warehouse.warehouseID)
                    currentDialog.dismiss()
                    findNavController().navigate(R.id.action_quotationFragment_to_saleRealizedFragment, bundle)
                } else {
                    hideLoading()
                    isProcessing = false
                    Toast.makeText(globalContext, "Error al procesar la preventa", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<Operation>, t: Throwable) {
                hideLoading()
                isProcessing = false
                Log.d("MIKE", "sendApiQuotation onFailure: " + t.message.toString())
                Toast.makeText(globalContext, "Error de conexión: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })
    }


}