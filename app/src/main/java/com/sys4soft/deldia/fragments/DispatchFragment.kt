package com.sys4soft.deldia.fragments

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.sys4soft.deldia.R
import com.sys4soft.deldia.adapter.ProductStoreAdapter
import com.sys4soft.deldia.localdatabase.Preference
import com.sys4soft.deldia.models.Operation
import com.sys4soft.deldia.models.Person
import com.sys4soft.deldia.models.Product
import com.sys4soft.deldia.models.Warehouse
import com.sys4soft.deldia.retrofit.UserApiService
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.squareup.picasso.Picasso
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.ArrayList

class DispatchFragment : Fragment() {


    private lateinit var textViewClientNames: TextView
    private lateinit var autoCompleteDocumentType: AutoCompleteTextView
    private lateinit var autoCompletePhysicalDistribution: AutoCompleteTextView

    private lateinit var searchViewProduct: SearchView
    private lateinit var recyclerViewProductStore: RecyclerView
    private lateinit var productStoreAdapter: ProductStoreAdapter
    private lateinit var fabSummary: FloatingActionButton

    private lateinit var textViewTotal: TextView
    private lateinit var textViewItems: TextView
    private lateinit var textViewSubtotal: TextView

    private lateinit var textViewDialogTotal: TextView
    private lateinit var textViewDialogItems: TextView
    private lateinit var textViewDialogSubtotal: TextView
    private lateinit var editTextMethodPrice: TextInputEditText
    private lateinit var switchShowImages: Switch
    private lateinit var refreshLayout: SwipeRefreshLayout
    private lateinit var buttonRefresh: Button

    private var globalContext: Context? = null

    private var warehouse: Warehouse = Warehouse()
    private var person: Person = Person()
    private var operation: Operation = Operation()
    private var list = arrayListOf<Product>()

    lateinit var preference: Preference
    private var paymentMethodList: MutableMap<String, Double> = mutableMapOf()

    private lateinit var layoutPaymentList: LinearLayout
    private lateinit var layoutListItem: LinearLayout
    private lateinit var loadingLayout: FrameLayout
    private lateinit var currentDialog: AlertDialog
    private lateinit var dialogClose: Button
//    private lateinit var dialogUpdate: Button
    private lateinit var dialogTerminate: Button
    private var isProcessing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        globalContext = this.activity

        val bundle = arguments
        operation.userID = bundle!!.getInt("userID")
        operation.operationID = bundle.getInt("operationID")

        preference = Preference(globalContext)
        warehouse.warehouseID = 3  // warehouse central
        warehouse.warehouseName = preference.getData("vehicleLicensePlate")
        loadOperation(operation)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_dispatch, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        buttonRefresh = view.findViewById(R.id.buttonRefresh)
        buttonRefresh.setOnClickListener {
            loadOperation(operation)

        }
        switchShowImages = view.findViewById(R.id.switchShowImages)
        switchShowImages.setOnCheckedChangeListener { _, isChecked ->
            list.forEach { item ->item.showImage=isChecked}
            recyclerViewProductStore.adapter?.notifyDataSetChanged()
        }

        textViewClientNames = view.findViewById(R.id.textViewClientNames)

        recyclerViewProductStore = view.findViewById(R.id.recyclerViewProductStore)
        recyclerViewProductStore.layoutManager = LinearLayoutManager(globalContext)
        recyclerViewProductStore.setHasFixedSize(true)

        productStoreAdapter = ProductStoreAdapter(list)

        productStoreAdapter.setOnQuantityChangeListener(object : ProductStoreAdapter.OnQuantityChangeListener{

            override fun onQuantityChanged(id: Int, quantity: String) {
                val pos = list.indexOfFirst { it.productID == id }

                if (pos != -1) {
                    var newSubtotal = 0.0
                    var newQuantity = 0
                    val product = list[pos]
                    if(quantity.isNotBlank()){
                        if(quantity.toInt() <= product.stock) {
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

        recyclerViewProductStore.adapter= productStoreAdapter

        textViewTotal = view.findViewById(R.id.textViewTotal)
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
                    productStoreAdapter.getFilter(filterList)
                }
                return true
            }
        })

        fabSummary = view.findViewById(R.id.fabSummary)
        fabSummary.setOnClickListener {
            addInfo()
            updateTotal()
            updatePaymentMethodTotal()

            //sendApiDispatch()

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

        when(operation.physicalDistribution){
            "01"->
                autoCompletePhysicalDistribution.setText(autoCompletePhysicalDistribution.adapter.getItem(listPhysicalDistribution.indexOf("TIRAS")).toString(),false)
            "02"->
                autoCompletePhysicalDistribution.setText(autoCompletePhysicalDistribution.adapter.getItem(listPhysicalDistribution.indexOf("SUELTAS")).toString(),false)
            "NA"->
                autoCompletePhysicalDistribution.setText(autoCompletePhysicalDistribution.adapter.getItem(listPhysicalDistribution.indexOf("NO APLICA")).toString(),false)
            else->
                autoCompletePhysicalDistribution.setText(autoCompletePhysicalDistribution.adapter.getItem(0).toString(),false)
        }
        autoCompletePhysicalDistribution.isEnabled = false

    }
    private fun loadOperation(o: Operation) {
//        o.operationID=31
        val apiInterface = UserApiService.create().getSaleByID(o)
        apiInterface.enqueue(object : Callback<Operation> {
            override fun onResponse(call: Call<Operation>, response: Response<Operation>) {

                if (response.body() != null) {
                    operation = response.body()!!
                    textViewClientNames.text = operation.clientFullName
                    loadProductStoreInWarehouse(warehouse)
                    // fabSummary.callOnClick()
                }
            }

            override fun onFailure(call: Call<Operation>, t: Throwable) {
                Log.d("MIKE", "loadOperation. Algo salio mal..." + t.message.toString())
            }
        })
    }

    private fun updateTotal() {
        var counter = 0
        var total = 0.0
        list.forEach {
            counter += it.quantity
            total += it.quantity * it.priceSale
        }
//        val totalF1:Double = String.format("%.2f", total).toDouble()
        val totalF3:Double = Math.round(total * 1000.0) / 1000.0
        val totalF1:Double = Math.round(totalF3 * 100.0) / 100.0
        textViewTotal.text = totalF1.toString()
        textViewItems.text = counter.toString()
        if(this::textViewDialogTotal.isInitialized){
            textViewDialogTotal.text = totalF1.toString()
            textViewDialogItems.text = counter.toString()

            val subtotal:Double = textViewDialogSubtotal.text.toString().replace("S/", "").trim().toDouble()
            val balance: Double = totalF1 - subtotal
            editTextMethodPrice.setText(balance.toString())


        }
    }

    private fun loadProductStoreInWarehouse(w: Warehouse) {
        val apiInterface = UserApiService.create().getStockInWarehouse(w)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ responseData ->
                Log.d("Mike", "loadProductStoreInWarehouse ${responseData?.size.toString()}")
                if(responseData != null && responseData.isNotEmpty()){
                    list = responseData
                    operation.details.forEach { operationDetail ->
                        if(operationDetail.quantity>0){
                            val element = list.find { it.productID == operationDetail.productID }
                            val getPosition = list.indexOfFirst { it.productID == operationDetail.productID }
                            element?.quantity = operationDetail.quantity
                            element?.positionInAdapter = getPosition
                            list[getPosition] = element!!
                        }
                    }
                    updateTotal()
                    fabSummary.callOnClick()
                    productStoreAdapter.getFilter(list)
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
    private fun showLoading() {
        loadingLayout.visibility = View.VISIBLE
        dialogClose.isEnabled = false
//        dialogUpdate.isEnabled = false
        dialogTerminate.isEnabled = false
    }

    private fun hideLoading() {
        loadingLayout.visibility = View.GONE
        dialogClose.isEnabled = true
//        dialogUpdate.isEnabled = true
        dialogTerminate.isEnabled = true
    }

    private fun addInfo(){
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
        loadDocumentType()
        loadPhysicalDistribution()
        editTextMethodPrice = v.findViewById(R.id.editTextMethodPrice)
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
        autoCompleteMethodName.setOnItemClickListener { adapterView, view, i, l ->
            val balance: Double = textViewDialogTotal.text.toString().toDouble() - textViewDialogSubtotal.text.toString().toDouble()
            editTextMethodPrice.setText(balance.toString())
        }
        if (paymentMethodList.isEmpty()){
            val total: String = textViewTotal.text.toString()
            editTextMethodPrice.setText(total)
        }
        paymentMethodList.forEach { (key, value) ->

            val inflaterPaymentMethod = LayoutInflater.from(globalContext)
            val viewPaymentMethod = inflaterPaymentMethod.inflate(R.layout.item_payment_method, null)
            val textViewPaymentMethodName = viewPaymentMethod.findViewById<TextView>(R.id.textViewPaymentMethodName)
            val textViewPaymentMethodPrice = viewPaymentMethod.findViewById<TextView>(R.id.textViewPaymentMethodPrice)
            val btnRemove = viewPaymentMethod.findViewById<Button>(R.id.buttonRemovePaymentMethodItem)

            var way: String = "CONTADO"
            when(key){
                "cash" -> way = "CONTADO"
                "yape" -> way = "YAPE"
                "bcp" -> way = "BCP - DEPOSITO"
                "credit" -> way = "CREDITO"
            }
            Log.d("MIKE", "way... $way value... $value")
            textViewPaymentMethodName.text = way
            textViewPaymentMethodPrice.text = "S/ $value"

            btnRemove.setOnClickListener {
                removePaymentMethodItem(viewPaymentMethod)
                paymentMethodList.remove(key)
                updatePaymentMethodTotal()
            }

            layoutPaymentList.addView(viewPaymentMethod)
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
                        Log.d("MIKE", keycode.toString())
                        Log.d("MIKE", editTextPrice.text.toString())
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
//                        recyclerViewProductStore.adapter?.notifyDataSetChanged()
                        recyclerViewProductStore.adapter?.notifyItemChanged(item.positionInAdapter)

                    }
                }

                btnMinus.setOnClickListener {
                    if (item.quantity >= 1) {
                        item.quantity -= 1
                        textViewQuantity.text = item.quantity.toString()
                        updateTotal()
//                        recyclerViewProductStore.adapter?.notifyDataSetChanged()
                        recyclerViewProductStore.adapter?.notifyItemChanged(item.positionInAdapter)

                    }

                }
            }

        }

        buttonAddPayment.setOnClickListener {
            if (editTextMethodPrice.text.toString().isNotEmpty()){
                var selectedItem = "cash"
                val value = autoCompleteMethodName.text.toString()
                val amount = editTextMethodPrice.text.toString().toDouble()

                when (value) {
                    "CONTADO" -> {selectedItem = "cash"}
                    "YAPE" -> {selectedItem = "yape"}
                    "BCP - DEPOSITO" -> {selectedItem = "bcp"}
                    "CREDITO" -> {selectedItem = "credit"}
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

        loadingLayout = v.findViewById(R.id.loadingLayout)
        dialogClose = v.findViewById(R.id.dialog_close)
//        dialogUpdate = v.findViewById(R.id.dialog_update)
        dialogTerminate = v.findViewById(R.id.dialog_terminate)

        val addDialog = AlertDialog.Builder(globalContext)
        addDialog.setView(v)
        addDialog.setTitle("RESUMEN DE VENTA")
        addDialog.setCancelable(false)

        currentDialog = addDialog.create()
        currentDialog.show()

        dialogClose.setOnClickListener{
            if (!isProcessing) {
                currentDialog.dismiss()
            }
        }
        
        dialogTerminate.setOnClickListener{
            if (isProcessing) return@setOnClickListener

            val payed = textViewSubtotal.text.toString().toDouble()
            val totalSale = textViewTotal.text.toString().toDouble()
            if (totalSale >= 0 && totalSale == payed) {
                isProcessing = true
                showLoading()
                terminateRestQuotation()
            } else {
                Toast.makeText(globalContext, "Verificar Venta.", Toast.LENGTH_SHORT).show()
//                dialogUpdate.visibility = View.VISIBLE
                dialogTerminate.visibility = View.VISIBLE
            }
        }

//        dialogUpdate.setOnClickListener{
//            val totalSale = textViewTotal.text.toString().toDouble()
//            if (totalSale > 0) {
//                updateRestQuotation()
//                Toast.makeText(globalContext, "Cotizacion actualizada.", Toast.LENGTH_SHORT).show()
//            }else {
//                Toast.makeText(globalContext, "Verificar cotizacion.", Toast.LENGTH_SHORT).show()
//            }
//            currentDialog.dismiss()
//        }
    }

    private fun terminateRestQuotation(){
        // 1. Validación de tipo de documento
        if (autoCompleteDocumentType.text.isNullOrEmpty()) {
            hideLoading()
            isProcessing = false
            Toast.makeText(globalContext, "Seleccione un tipo de documento", Toast.LENGTH_LONG).show()
            return
        }

        // Validación de factura con RUC
        if (autoCompleteDocumentType.text.toString() == "FACTURA" && operation.clientDocumentType != "RUC") {
            hideLoading()
            isProcessing = false
            Toast.makeText(globalContext, "Solo se puede emitir factura a clientes con RUC", Toast.LENGTH_LONG).show()
            return
        }

        // 2. Validación de distribución física
        if (autoCompletePhysicalDistribution.text.isNullOrEmpty()) {
            hideLoading()
            isProcessing = false
            Toast.makeText(globalContext, "Seleccione un tipo de distribución física", Toast.LENGTH_LONG).show()
            return
        }

        // 3. Validación de métodos de pago
        if (paymentMethodList.isEmpty()) {
            hideLoading()
            isProcessing = false
            Toast.makeText(globalContext, "Debe agregar al menos un método de pago", Toast.LENGTH_LONG).show()
            return
        }

        // 4. Validación del total y monto pagado
        val totalVenta = textViewTotal.text.toString().toDoubleOrNull() ?: 0.0
        val totalPagado = textViewSubtotal.text.toString().toDoubleOrNull() ?: 0.0
        if (totalVenta <= 0) {
            hideLoading()
            isProcessing = false
            Toast.makeText(globalContext, "El total de la venta debe ser mayor a 0", Toast.LENGTH_LONG).show()
            return
        }
        if (totalVenta != totalPagado) {
            hideLoading()
            isProcessing = false
            Toast.makeText(globalContext, "El monto pagado debe ser igual al total de la venta", Toast.LENGTH_LONG).show()
            return
        }

        // Procesar distribución física
        when(autoCompletePhysicalDistribution.text.toString()){
            "TIRAS"->operation.physicalDistribution="01"
            "SUELTAS"->operation.physicalDistribution="02"
            "NO APLICA"->operation.physicalDistribution="NA"
        }

        // Procesar tipo de documento
        when(autoCompleteDocumentType.text.toString()){
            "TICKET"->operation.documentType="01"
            "BOLETA"->operation.documentType="02"
            "FACTURA"->operation.documentType="03"
        }

        operation.paymentMethods = paymentMethodList
        operation.details.clear()

        // 5. Validación de productos
        var hayProductos = false
        list.forEach {
            if(it.quantity > 0){
                if (it.priceSale <= 0) {
                    hideLoading()
                    isProcessing = false
                    Toast.makeText(globalContext, "Hay productos con precio inválido", Toast.LENGTH_LONG).show()
                    return
                }
                val detail: Operation.OperationDetail = Operation.OperationDetail()
                detail.productTariffID = it.productTariffID
                detail.price = it.priceSale
                detail.quantity = it.quantity
                operation.details.add(detail)
                hayProductos = true
            }
        }

        if (!hayProductos) {
            hideLoading()
            isProcessing = false
            Toast.makeText(globalContext, "Debe agregar al menos un producto", Toast.LENGTH_LONG).show()
            return
        }

        // 6. Validación de usuario
        if (operation.userID <= 0) {
            hideLoading()
            isProcessing = false
            Toast.makeText(globalContext, "Verificar user", Toast.LENGTH_LONG).show()
            return
        }

        sendApiTerminateQuotation()
    }

    private fun sendApiTerminateQuotation(){
        val apiInterface = UserApiService.create().sendTerminateQuotationData(operation)
        apiInterface.enqueue(object : Callback<Operation>{
            override fun onResponse(call: Call<Operation>, response: Response<Operation>) {
                if (response.body() != null) {
                    operation = response.body()!!
                    val bundle = arguments
                    bundle!!.putString("operationID", operation.operationID.toString())

                    val documentType = autoCompleteDocumentType.text.toString()
                    currentDialog.dismiss()

                    if (documentType=="TICKET"){
                        findNavController().navigate(R.id.action_dispatchFragment_to_printActivity, bundle)
                    }else{
                        if (operation.pseSent){
                            findNavController().navigate(R.id.action_dispatchFragment_to_printActivity, bundle)
                        }else{
                            Toast.makeText(globalContext, operation.messageStatus, Toast.LENGTH_LONG).show()
                            findNavController().navigate(R.id.action_dispatchFragment_to_saleRealizedFragment, bundle)
                        }
                    }
                } else {
                    hideLoading()
                    isProcessing = false
                    Toast.makeText(globalContext, "Error al procesar el pedido", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<Operation>, t: Throwable) {
                hideLoading()
                isProcessing = false
                Log.d("MIKE", "sendApiTerminateQuotation onFailure: " + t.message.toString())
                Toast.makeText(globalContext, "Error de conexión: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })
    }
    private fun updateRestQuotation(){

        when(autoCompletePhysicalDistribution.text.toString()){
            "TIRAS"->operation.physicalDistribution="01"
            "SUELTAS"->operation.physicalDistribution="02"
            "NO APLICA"->operation.physicalDistribution="NA"
        }
        operation.details.clear()

        list.forEach {
            if(it.quantity>0){
                val detail: Operation.OperationDetail = Operation.OperationDetail()
                detail.productTariffID = it.productTariffID
                detail.price = it.priceSale
                detail.quantity = it.quantity
//                detail.batchCode = it.batchCode
//                detail.batchStock = it.batchStock

                operation.details.add(detail)
            }
        }
        if (operation.userID>0)
            sendApiUpdateQuotation()
        else
            Toast.makeText(globalContext, "Verificar user", Toast.LENGTH_LONG).show()

    }

    private fun sendApiUpdateQuotation(){

        val apiInterface = UserApiService.create().sendUpdateQuotationData(operation)
        apiInterface.enqueue(object : Callback<Operation>{
            override fun onResponse(call: Call<Operation>, response: Response<Operation>) {
                val bundle = arguments
                bundle!!.putInt("userID", operation.userID)
                bundle.putInt("vehicleID", operation.warehouseID)
                findNavController().navigate(R.id.action_dispatchFragment_to_saleRealizedFragment, bundle)
            }

            override fun onFailure(call: Call<Operation>, t: Throwable) {
                Log.d("MIKE", "sendApiUpdateQuotation onFailure: " + t.message.toString())
            }

        })

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

        when(operation.clientDocumentType){
            "DNI"->
                autoCompleteDocumentType.setText(autoCompleteDocumentType.adapter.getItem(listMethod.indexOf("BOLETA")).toString(),false)
            "RUC"->
                autoCompleteDocumentType.setText(autoCompleteDocumentType.adapter.getItem(listMethod.indexOf("FACTURA")).toString(),false)
            else->
                autoCompleteDocumentType.setText(autoCompleteDocumentType.adapter.getItem(0).toString(),false)
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
//        val totalF1:Double = String.format("%.2f", total).toDouble()
        val totalF3:Double = Math.round(total * 1000.0) / 1000.0
        val totalF1:Double = Math.round(totalF3 * 100.0) / 100.0
        textViewSubtotal.text = totalF1.toString()
        if(this::textViewDialogSubtotal.isInitialized){
            textViewDialogSubtotal.text = totalF1.toString()
            val balance: Double = textViewTotal.text.toString().toDouble() - totalF1
            editTextMethodPrice.setText(balance.toString())
        }
    }

    private fun View.closeKeyBoard(inputMethodManager: InputMethodManager) {
        inputMethodManager.hideSoftInputFromWindow(windowToken, 0)
    }
}