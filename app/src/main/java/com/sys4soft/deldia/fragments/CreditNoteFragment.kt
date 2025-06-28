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
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sys4soft.deldia.R
import com.sys4soft.deldia.adapter.DevolutionProductStoreAdapter
import com.sys4soft.deldia.localdatabase.Preference
import com.sys4soft.deldia.models.Operation
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

class CreditNoteFragment : Fragment() {

    private var globalContext: Context? = null
    private var operation: Operation = Operation()

    private lateinit var textViewClientNames: TextView
    private lateinit var autoCompleteDocumentType: AutoCompleteTextView
    private lateinit var autoCompletePhysicalDistribution: AutoCompleteTextView

    private lateinit var recyclerViewProductStore: RecyclerView
    private lateinit var devolutionProductStoreAdapter: DevolutionProductStoreAdapter
    private lateinit var fabSummary: FloatingActionButton

    private lateinit var textViewTotal: TextView
    private lateinit var textViewItems: TextView
    private lateinit var textViewSubtotal: TextView
    private lateinit var textViewDialogTotal: TextView
    private lateinit var textViewDialogItems: TextView
    private lateinit var textViewDialogSubtotal: TextView
    private lateinit var editTextMethodPrice: TextInputEditText

    private lateinit var layoutPaymentList: LinearLayout
    private lateinit var layoutListItem: LinearLayout

    private var list = arrayListOf<Product>()
    private var warehouse: Warehouse = Warehouse()
    lateinit var preference: Preference
    private var paymentMethodList: MutableMap<String, Double> = mutableMapOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        globalContext = this.activity
        val bundle = arguments
        operation.userID = bundle!!.getInt("userID")
        operation.operationID = bundle.getInt("operationID")
        Toast.makeText(globalContext, "operation.operationID: ${operation.operationID}", Toast.LENGTH_LONG).show()
        preference = Preference(globalContext)
        warehouse.warehouseID = 3  // warehouse central
        loadOperation(operation)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_credit_note, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        textViewClientNames = view.findViewById(R.id.textViewClientNames)
        recyclerViewProductStore = view.findViewById(R.id.recyclerViewProductStore)
        textViewTotal = view.findViewById(R.id.textViewTotal)
        textViewItems = view.findViewById(R.id.textViewItems)
        textViewSubtotal = view.findViewById(R.id.textViewSubtotal)
        recyclerViewProductStore.layoutManager = LinearLayoutManager(globalContext)
        recyclerViewProductStore.setHasFixedSize(true)

        devolutionProductStoreAdapter = DevolutionProductStoreAdapter(list)

        devolutionProductStoreAdapter.setOnQuantityChangeListener(object : DevolutionProductStoreAdapter.OnQuantityChangeListener{

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

        recyclerViewProductStore.adapter= devolutionProductStoreAdapter

        textViewTotal = view.findViewById(R.id.textViewTotal)
        textViewItems = view.findViewById(R.id.textViewItems)
        textViewSubtotal = view.findViewById(R.id.textViewSubtotal)
        fabSummary = view.findViewById(R.id.fabSummary)
        fabSummary.setOnClickListener {
            addInfo()
            updateTotal()
            updatePaymentMethodTotal()

        }
    }

    private fun loadOperation(o: Operation) {
        val apiInterface = UserApiService.create().getSaleByID(o)
        apiInterface.enqueue(object : Callback<Operation> {
            override fun onResponse(call: Call<Operation>, response: Response<Operation>) {

                if (response.body() != null) {
                    operation = response.body()!!
                    textViewClientNames.text = operation.clientFullName
                    loadProductStoreInWarehouse(operation)
                    // fabSummary.callOnClick()
                }
            }

            override fun onFailure(call: Call<Operation>, t: Throwable) {
                Log.d("MIKE", "loadOperation. Algo salio mal..." + t.message.toString())
            }
        })
    }

    private fun loadProductStoreInWarehouse(o: Operation) {
        val apiInterface = UserApiService.create().getStockBySaleAndWarehouse(o)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ responseData ->
                Log.d("Mike", "loadAllStockInWarehouse ${responseData?.size.toString()}")
                if(responseData != null && responseData.isNotEmpty()){
                    val productIDsInOperation = operation.details.map { it.productID }.toSet()
                    list = responseData.filter { it.productID in productIDsInOperation } as ArrayList<Product>

                    operation.details.forEach { operationDetail ->
                        if(operationDetail.quantity>0){
                            val element = list.find { it.productID == operationDetail.productID }
                            val getPosition = list.indexOfFirst { it.productID == operationDetail.productID }
                            element?.stock = operationDetail.quantity.toDouble()
                            element?.quantity = operationDetail.quantity
                            element?.positionInAdapter = getPosition
                            list[getPosition] = element!!
                        }
                    }
                    updateTotal()
//                    fabSummary.callOnClick()
                    devolutionProductStoreAdapter.getFilter(list)
                }
            }, { error ->
                Log.d("Mike", error.toString())
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

    private fun terminateRestQuotation(){
        when(autoCompletePhysicalDistribution.text.toString()){
            "TIRAS"->operation.physicalDistribution="01"
            "SUELTAS"->operation.physicalDistribution="02"
            "NO APLICA"->operation.physicalDistribution="NA"
        }
        when(autoCompleteDocumentType.text.toString()){
            "TICKET"->operation.documentType="01"
            "BOLETA"->operation.documentType="02"
            "FACTURA"->operation.documentType="03"
        }

        operation.paymentMethods = paymentMethodList

        operation.details.clear()

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
            sendApiDevolutionDeliveryData()
        else
            Toast.makeText(globalContext, "Verificar user", Toast.LENGTH_LONG).show()

    }

    private fun sendApiDevolutionDeliveryData(){
        val apiInterface = UserApiService.create().sendDevolutionDeliveryData(operation)
        apiInterface.enqueue(object : Callback<Operation>{
            override fun onResponse(call: Call<Operation>, response: Response<Operation>) {
                if (response.body() != null) {
                    operation = response.body()!!
                    // HERE
                    val bundle = arguments
                    bundle!!.getInt("vehicleID", operation.warehouseID)
                    bundle.getInt("userID", operation.userID)

                    findNavController().navigate(R.id.action_creditNoteFragment_to_mapFragment, bundle)


                }

            }

            override fun onFailure(call: Call<Operation>, t: Throwable) {
                Log.d("MIKE", "sendApiDevolutionDeliveryData onFailure: " + t.message.toString())
            }

        })
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
        val listMethod = listOf("CONTADO", "YAPE", "CREDITO")
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


        val dialogClose = v.findViewById<Button>(R.id.dialog_close)
//        val dialogUpdate = v.findViewById<Button>(R.id.dialog_update)
        val dialogTerminate = v.findViewById<Button>(R.id.dialog_terminate)


        val addDialog = AlertDialog.Builder(globalContext)
        addDialog.setView(v)
        addDialog.setTitle("RESUMEN DE ENTREGA")

        val dialog: AlertDialog = addDialog.create()
        dialog.show()
        dialogClose.setOnClickListener{
            dialog.dismiss()
        }

        dialogTerminate.setOnClickListener{
            val payed = textViewSubtotal.text.toString().toDouble()
            val totalSale = textViewTotal.text.toString().toDouble()
            if (totalSale >= 0 && totalSale == payed) {
//                dialogUpdate.visibility = View.INVISIBLE
                dialogTerminate.visibility = View.INVISIBLE
                terminateRestQuotation()
                Toast.makeText(globalContext, "Entrega finalizada.", Toast.LENGTH_SHORT).show()
            }else {
                Toast.makeText(globalContext, "Verificar Entrega.", Toast.LENGTH_SHORT).show()
//                dialogUpdate.visibility = View.VISIBLE
                dialogTerminate.visibility = View.VISIBLE
            }
            dialog.dismiss()
        }

//        dialogUpdate.visibility = View.GONE
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

            val subtotal:Double = textViewDialogSubtotal.text.toString().replace("S/", "").trim().toDouble()
            val balance: Double = totalF1 - subtotal
            editTextMethodPrice.setText(balance.toString())


        }
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
