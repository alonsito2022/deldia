package com.example.deldia.fragments

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.deldia.R
import com.example.deldia.adapter.ProductQuotationAdapter
import com.example.deldia.adapter.ProductSaleAdapter
import com.example.deldia.localdatabase.MySQLiteHelper
import com.example.deldia.localdatabase.Preference
import com.example.deldia.models.Operation
import com.example.deldia.models.Person
import com.example.deldia.models.Product
import com.example.deldia.models.Warehouse
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


class SaleFragment : Fragment() {

    //    private lateinit var textInputEditTextClientNames: TextInputEditText
    private lateinit var textViewClientNames: TextView
    private lateinit var textViewClientAddress: TextView
    private lateinit var autoCompleteDocumentType: AutoCompleteTextView
    private lateinit var autoCompletePhysicalDistribution: AutoCompleteTextView
    private lateinit var searchViewProduct: SearchView
    private lateinit var recyclerViewProductStore: RecyclerView
    private lateinit var productSaleAdapter: ProductSaleAdapter
    private lateinit var fabSummary: MaterialButton

    private lateinit var textViewTotal: TextView
    private lateinit var textViewItems: TextView
    private lateinit var textViewSubtotal: TextView

    private lateinit var textViewDialogTotal: TextView
    private lateinit var textViewDialogItems: TextView
    private lateinit var textViewDialogSubtotal: TextView

    private lateinit var switchShowImages: Switch
    private lateinit var refreshLayout: SwipeRefreshLayout
    private lateinit var buttonRefresh: Button

    private var globalContext: Context? = null

    private var warehouse: Warehouse = Warehouse()
    private var person: Person = Person()
    private var operation: Operation = Operation()
    private var list = arrayListOf<Product>()
    private lateinit var preference: Preference


    private var paymentMethodList: MutableMap<String, Double> = mutableMapOf()

    private lateinit var layoutPaymentList: LinearLayout
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
        val bundle = arguments
        operation.userID = bundle!!.getInt("userID")
        warehouse.warehouseID = bundle.getInt("vehicleID")
        warehouse.warehouseName = bundle.getString("vehicleLicensePlate").toString()
        person.personID = bundle.getInt("personID")
        person.fullName = bundle.getString("personFullName").toString()
        person.address = bundle.getString("personAddress").toString()
        person.documentNumber = bundle.getString("personDocumentNumber").toString()
        person.documentType = bundle.getString("personDocumentType").toString()
        person.physicalDistribution = bundle.getString("physicalDistribution").toString()
        person.physicalDistributionDisplay = bundle.getString("physicalDistributionDisplay").toString()
        operation.routeDate = bundle.getString("routeDate").toString()

        val formatter = SimpleDateFormat("yyyy-MM-dd")
        val date = formatter.parse(operation.routeDate)
        val calendar = Calendar.getInstance()
        calendar.time = date
        person.visitDay = calendar.get(Calendar.DAY_OF_WEEK)
        when(person.visitDay){
            1-> person.visitDayDisplay = "DOMINGO"
            2-> person.visitDayDisplay = "LUNES"
            3-> person.visitDayDisplay = "MARTES"
            4-> person.visitDayDisplay = "MIERCOLES"
            5-> person.visitDayDisplay = "JUEVES"
            6-> person.visitDayDisplay = "VIERNES"
            7-> person.visitDayDisplay = "SABADO"
        }
        if(person.documentType=="01") person.documentTypeDisplay="DNI"
        else if(person.documentType=="06") person.documentTypeDisplay="RUC"

        loadProductStoreInWarehouse()
    }

    private fun loadDocumentType(){
        val listMethod = listOf("TICKET", "BOLETA", "FACTURA")
        val adapter = ArrayAdapter(
            globalContext!!,
            android.R.layout.simple_spinner_dropdown_item,
            listMethod
        )
        autoCompleteDocumentType.keyListener = null
        autoCompleteDocumentType.setAdapter(adapter)

        when(person.documentType){
            "01"->
                autoCompleteDocumentType.setText(autoCompleteDocumentType.adapter.getItem(listMethod.indexOf("BOLETA")).toString(),false)
            "06"->
                autoCompleteDocumentType.setText(autoCompleteDocumentType.adapter.getItem(listMethod.indexOf("FACTURA")).toString(),false)
            else->
                autoCompleteDocumentType.setText(autoCompleteDocumentType.adapter.getItem(0).toString(),false)
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
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_sale, container, false)
    }

    private fun setupRecyclerView() {
        // Configurar el LayoutManager con optimizaciones
        val layoutManager = LinearLayoutManager(requireContext())
        recyclerViewProductStore.apply {
            this.layoutManager = layoutManager
            setHasFixedSize(true)
            setItemViewCacheSize(20)
            isNestedScrollingEnabled = true  // Habilitar scroll
            recycledViewPool.setMaxRecycledViews(0, 15)
        }

        productSaleAdapter = ProductSaleAdapter(list).apply {
            setHasStableIds(true)
            setOnQuantityChangeListener(object : ProductSaleAdapter.OnQuantityChangeListener{
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
        }
        recyclerViewProductStore.adapter = productSaleAdapter


    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

//        val paymentRef = pickingReference.child("gangs").child(preference.getData("gangID")).child("users").child(preference.getData("userID"))
//        paymentRef.setValue(null)

        textViewClientNames = view.findViewById(R.id.textViewClientNames)
        textViewClientAddress = view.findViewById(R.id.textViewClientAddress)
        textViewClientNames.text = person.fullName
        textViewClientAddress.text = person.address
        recyclerViewProductStore = view.findViewById(R.id.recyclerViewProductStore)
        switchShowImages = view.findViewById(R.id.switchShowImages)
        switchShowImages.setOnCheckedChangeListener { _, isChecked ->
            list.forEach { item ->item.showImage=isChecked}
            recyclerViewProductStore.adapter?.notifyDataSetChanged()
        }

        setupRecyclerView()



        textViewTotal = view.findViewById(R.id.textViewTotal)
        buttonRefresh = view.findViewById(R.id.buttonRefresh)
        buttonRefresh.setOnClickListener {
            loadProductStoreInWarehouse()
        }
        textViewItems = view.findViewById(R.id.textViewItems)
        textViewSubtotal = view.findViewById(R.id.textViewSubtotal)
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
                        if(product.productSaleName.lowercase().contains(newText.lowercase()) or product.productCode.toString().contains(newText)){
                            filterList.add(product)
                        }
                    }
                }
                if(filterList.isEmpty()){
                    Toast.makeText(globalContext, "No data found", Toast.LENGTH_LONG).show()
                }else{
                    productSaleAdapter.getFilter(filterList)
                }
                return true
            }
        })

        fabSummary = view.findViewById(R.id.fabSummary)
        fabSummary.setOnClickListener {

//            if (checkQuantities()){
                addInfo()
                updateTotal()
                updatePaymentMethodTotal()
//            }
//            else{
//                Toast.makeText(globalContext, "Verificar cantidades. Productos chicos (x12); medianos (x6) grandes (x1)", Toast.LENGTH_LONG).show()
//
//            }

        }
    }

    private fun checkQuantities(): Boolean {
        var isValid = true
        list.forEach {
            if(it.priceSale > 3.3){
                // big
            }
            else if(it.priceSale > 2 && it.priceSale <= 3.3){
                // medium
                if (it.quantity % 6 != 0) isValid = false
            }
            else if(it.priceSale > 0.8 && it.priceSale <= 2){
                // small
                if (it.quantity % 6 != 0) isValid = false
            }
        }
        return isValid
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
        val apiInterface = UserApiService.create().getStockInWarehouse(warehouse)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ responseData ->
                responseData?.let {
                    list.clear()
                    list.addAll(it)
                    productSaleAdapter.getFilter(list)
                }
            }, { error -> Log.e("SaleFragment", "Error loading products: ${error.message}") })
    }

    private fun openModal(p: Product){
        val inflater = LayoutInflater.from(globalContext)
        val v = inflater.inflate(R.layout.dialog_show_image, null)
        val btnClose = v.findViewById<Button>(R.id.btnClose)
        val imageViewProduct = v.findViewById<ImageView>(R.id.imageViewProduct)
        Picasso.get().setIndicatorsEnabled(true)
        Picasso.get().load(p.productPath)
//            .memoryPolicy(MemoryPolicy.NO_CACHE,MemoryPolicy.NO_STORE)
//            .networkPolicy(NetworkPolicy.NO_CACHE, NetworkPolicy.NO_STORE)
//            .fit()
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

    private fun validateSale(): Boolean {
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

        // Validar que el tipo de documento esté seleccionado
        if (autoCompleteDocumentType.text.isNullOrEmpty()) {
            Toast.makeText(globalContext, "Debe seleccionar el tipo de documento", Toast.LENGTH_SHORT).show()
            return false
        }

        // Validar que el monto pagado coincida con el total
        val totalSale = textViewTotal.text.toString().toDoubleOrNull() ?: 0.0
        val totalPaid = textViewSubtotal.text.toString().toDoubleOrNull() ?: 0.0
        if (totalSale != totalPaid) {
            Toast.makeText(globalContext, "El monto pagado debe ser igual al total de la venta", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
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

    private fun registerRestDispatch() {
        if (!validateSale()) return

        when(autoCompletePhysicalDistribution.text.toString()) {
            "TIRAS" -> operation.physicalDistribution = "01"
            "SUELTAS" -> operation.physicalDistribution = "02"
            "NO APLICA" -> operation.physicalDistribution = "NA"
        }
        when(autoCompleteDocumentType.text.toString()) {
            "TICKET" -> operation.documentType = "01"
            "BOLETA" -> operation.documentType = "02"
            "FACTURA" -> operation.documentType = "03"
        }

        operation.clientID = person.personID
        operation.warehouseID = warehouse.warehouseID
        operation.paymentMethods = paymentMethodList

        list.forEach {
            if(it.quantity > 0) {
                val detail = Operation.OperationDetail().apply {
                    productTariffID = it.productTariffID
                    price = it.priceSale
                    quantity = it.quantity
                }
                operation.details.add(detail)
            }
        }

        if (operation.userID > 0) {
            showLoading()
            isProcessing = true
            sendApiDispatch(operation.operationID)
        } else {
            Toast.makeText(globalContext, "Verificar usuario", Toast.LENGTH_LONG).show()
        }
    }

    private fun sendApiDispatch(id: Int) {
        val apiInterface = UserApiService.create().sendDispatchData(operation)
        apiInterface.enqueue(object : Callback<Operation> {
            override fun onResponse(call: Call<Operation>, response: Response<Operation>) {
                hideLoading()
                isProcessing = false
                
                if (response.body() != null) {
                    operation = response.body()!!
                    val bundle = arguments
                    bundle!!.putString("operationID", operation.operationID.toString())

                    val documentType = autoCompleteDocumentType.text.toString()
                    when {
                        documentType == "TICKET" -> {
                            findNavController().navigate(R.id.action_saleFragment_to_printActivity, bundle)
                        }
                        operation.pseSent -> {
                            findNavController().navigate(R.id.action_saleFragment_to_printActivity, bundle)
                        }
                        else -> {
                            Toast.makeText(globalContext, operation.messageStatus, Toast.LENGTH_LONG).show()
                            findNavController().navigate(R.id.action_saleFragment_to_saleRealizedFragment, bundle)
                        }
                    }
                    currentDialog.dismiss()
                } else {
                    Toast.makeText(globalContext, "Error al procesar la venta", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<Operation>, t: Throwable) {
                hideLoading()
                isProcessing = false
                Log.e("SaleFragment", "Error en sendApiDispatch: ${t.message}")
                Toast.makeText(globalContext, "Error de conexión: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun addInfo() {
        val inflater = LayoutInflater.from(globalContext)
        val v = inflater.inflate(R.layout.dialog_confirm_dispatch, null)
        val buttonAddPayment = v.findViewById<Button>(R.id.buttonAddPayment)
        layoutPaymentList = v.findViewById(R.id.layoutPaymentList)
        layoutListItem = v.findViewById(R.id.layoutList)
        textViewDialogTotal = v.findViewById(R.id.textViewDialogTotal)
        textViewDialogSubtotal = v.findViewById(R.id.textViewDialogSubtotal)
        textViewDialogItems = v.findViewById(R.id.textViewDialogItems)
        autoCompletePhysicalDistribution = v.findViewById(R.id.autoCompletePhysicalDistribution)
        autoCompleteDocumentType = v.findViewById(R.id.autoCompleteDocumentType)
        loadingLayout = v.findViewById(R.id.loadingLayout)
        dialogClose = v.findViewById(R.id.dialog_close)
        dialogSave = v.findViewById(R.id.dialog_terminate)
        loadDocumentType()
        loadPhysicalDistribution()
        val editTextMethodPrice = v.findViewById<TextInputEditText>(R.id.editTextMethodPrice)
        val autoCompleteMethodName = v.findViewById<AutoCompleteTextView>(R.id.autoCompleteMethodName)
        val listMethod = listOf("CONTADO", "YAPE", "BCP - DEPOSITO", "CREDITO")
        val adapter = ArrayAdapter(
            globalContext!!,
            android.R.layout.simple_spinner_dropdown_item,
            listMethod
        )
        autoCompleteMethodName.keyListener = null
        autoCompleteMethodName.setAdapter(adapter)
        autoCompleteMethodName.setText(
            autoCompleteMethodName.adapter.getItem(0).toString(),
            false
        )
        if (paymentMethodList.isEmpty()){
            val total: String = textViewTotal.text.toString()
            editTextMethodPrice.setText(total)
        }

        buttonAddPayment.setOnClickListener {
            if (editTextMethodPrice.text.toString().isNotEmpty()){
                var selectedItem = "cash"
                val value = autoCompleteMethodName.text.toString()
                val amount = editTextMethodPrice.text.toString().toDouble()

                when (value) {
                    "CONTADO" -> selectedItem = "cash"
                    "YAPE" -> selectedItem = "yape"
                    "BCP - DEPOSITO" -> selectedItem = "bcp"
                    "CREDITO" -> selectedItem = "credit"
                }

                val searchPaymentMethod = paymentMethodList.filter { (key, _) -> key == selectedItem }
                if (searchPaymentMethod.isEmpty()) {
                    val inflaterMethod = LayoutInflater.from(globalContext)
                    val viewMethod = inflaterMethod.inflate(R.layout.item_payment_method, null)

                    val textViewPaymentMethodName = viewMethod.findViewById<TextView>(R.id.textViewPaymentMethodName)
                    val textViewPaymentMethodPrice = viewMethod.findViewById<TextView>(R.id.textViewPaymentMethodPrice)
                    val btnRemove = viewMethod.findViewById<Button>(R.id.buttonRemovePaymentMethodItem)

                    textViewPaymentMethodName.text = value
                    textViewPaymentMethodPrice.text = "S/ $amount"

                    paymentMethodList[selectedItem] = amount
                    updatePaymentMethodTotal()

                    btnRemove.setOnClickListener {
                        removePaymentMethodItem(viewMethod)
                        paymentMethodList.remove(selectedItem)
                        updatePaymentMethodTotal()
                    }

                    editTextMethodPrice.setText("")
                    val inputManager = globalContext?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    editTextMethodPrice.closeKeyBoard(inputManager)
                    editTextMethodPrice.clearFocus()

                    layoutPaymentList.addView(viewMethod)
                }else {
                    autoCompleteMethodName.showDropDown()
                    Toast.makeText(globalContext, "Ya existe el metodo de pago", Toast.LENGTH_LONG)
                        .show()
                }
            }else {
                Toast.makeText(globalContext, "Verifique el monto", Toast.LENGTH_LONG).show()
            }
        }

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
//                    recyclerViewProductStore.adapter?.notifyDataSetChanged()
                    recyclerViewProductStore.adapter?.notifyItemChanged(item.positionInAdapter)

                }

                btnPlus.setOnClickListener {
                    if (item.quantity < item.stock) {
                        item.quantity += 1
                        textViewQuantity.text = item.quantity.toString()
                        updateTotal()
//                        updateFirebaseItem(item)
//                        recyclerViewProductStore.adapter?.notifyDataSetChanged()
                        recyclerViewProductStore.adapter?.notifyItemChanged(item.positionInAdapter)
                    }
                }

                btnMinus.setOnClickListener {
                    if (item.quantity >= 1) {
                        item.quantity -= 1
                        textViewQuantity.text = item.quantity.toString()
                        updateTotal()
//                        updateFirebaseItem(item)
//                        recyclerViewProductStore.adapter?.notifyDataSetChanged()
                        recyclerViewProductStore.adapter?.notifyItemChanged(item.positionInAdapter)
                    }

                }
            }

        }

        val addDialog = AlertDialog.Builder(globalContext)
        addDialog.setView(v)
        addDialog.setTitle("RESUMEN DE VENTA")
        addDialog.setCancelable(false)

        currentDialog = addDialog.create()
        currentDialog.show()

        dialogClose.setOnClickListener {
            if (!isProcessing) {
                currentDialog.dismiss()
            }
        }

        dialogSave.setOnClickListener {
            if (isProcessing) return@setOnClickListener
            registerRestDispatch()
        }
    }

    private fun removePaymentMethodItem(view: View) {
        layoutPaymentList.removeView(view)
    }

    private fun updatePaymentMethodTotal() {
        var total = 0.0
        paymentMethodList.forEach { (_, value) ->
            total += value
        }
        val totalF1:Double = String.format("%.2f", total).toDouble()
        textViewSubtotal.text = totalF1.toString()
        if(this::textViewDialogSubtotal.isInitialized){
            textViewDialogSubtotal.text = totalF1.toString()
        }
    }

    private fun View.closeKeyBoard(inputMethodManager: InputMethodManager) {
        inputMethodManager.hideSoftInputFromWindow(windowToken, 0)
    }
}