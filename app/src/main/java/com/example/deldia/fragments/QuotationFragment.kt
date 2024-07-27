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


class QuotationFragment : Fragment() {

//    private lateinit var textInputEditTextClientNames: TextInputEditText
    private lateinit var textViewClientNames: TextView
    private lateinit var textViewClientAddress: TextView
    private lateinit var searchViewProduct: SearchView
    private lateinit var recyclerViewProductStore: RecyclerView
    private lateinit var productQuotationAdapter: ProductQuotationAdapter
    private lateinit var fabSummary: FloatingActionButton

    private lateinit var textViewTotal: TextView
    private lateinit var textViewItems: TextView

    private lateinit var textViewDialogTotal: TextView
    private lateinit var textViewDialogItems: TextView
    private lateinit var autoCompletePhysicalDistribution: AutoCompleteTextView

    private lateinit var switchShowImages: Switch
    private lateinit var buttonRefresh: Button
    private lateinit var database: FirebaseDatabase
    private lateinit var pickingReference: DatabaseReference

    private var globalContext: Context? = null

    private var warehouse: Warehouse = Warehouse()
    private var person: Person = Person()
    private var operation: Operation = Operation()
    private var list = arrayListOf<Product>()
    private var firebaseList = arrayListOf<Product>()
    private lateinit var preference: Preference
    private var nroItem: Int = 1

//    private var paymentMethodList: MutableMap<String, Double> = mutableMapOf()

//    private lateinit var layoutPaymentList: LinearLayout
    private lateinit var layoutListItem: LinearLayout


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        globalContext = this.activity
        preference = Preference(globalContext)

//        database = FirebaseDatabase.getInstance()
//        pickingReference = database.getReference("picking")

        val bundle = arguments
        operation.userID = bundle!!.getInt("userID")
        warehouse.warehouseID = 3  // warehouse central
        warehouse.warehouseName = bundle.getString("vehicleLicensePlate").toString()
        person.personID = bundle.getInt("personID")
        person.fullName = bundle.getString("personFullName").toString()
        person.address = bundle.getString("personAddress").toString()
        person.documentNumber = bundle.getString("personDocumentNumber").toString()
        person.documentType = bundle.getString("personDocumentType").toString()
        person.physicalDistribution = bundle.getString("physicalDistribution").toString()
        person.physicalDistributionDisplay = bundle.getString("physicalDistributionDisplay").toString()
        operation.routeDate = bundle.getString("routeDate").toString()

        loadProductStoreInWarehouse(warehouse)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_quotation, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

//        val paymentRef = pickingReference.child("gangs").child(preference.getData("gangID")).child("users").child(preference.getData("userID"))
//        paymentRef.setValue(null)

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
            loadProductStoreInWarehouse(warehouse)
        }
        textViewItems = view.findViewById(R.id.textViewItems)
//        textViewSubtotal = view.findViewById(R.id.textViewSubtotal)
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
                    productQuotationAdapter.getFilter(filterList)
                }
                return true
            }
        })

        fabSummary = view.findViewById(R.id.fabSummary)
        fabSummary.setOnClickListener {
            addInfo()
            updateTotal()
            // updatePaymentMethodTotal()


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

    private fun loadProductStoreInWarehouse(w: Warehouse) {
        val apiInterface = UserApiService.create().getStockInWarehouse(w)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ responseData ->
                Log.d("Mike", responseData?.size.toString())
                if(responseData != null && responseData.isNotEmpty()){
                    list = responseData
                    productQuotationAdapter.getFilter(list)
                }
            }, { error ->
                Log.d("Mike", error.toString())
            })

    }
//    private fun updateFirebaseItem(product: Product){
//        val paymentRef = pickingReference.child("gangs").child(preference.getData("gangID")).child("users").child(preference.getData("userID"))
//        var code: String = product.productCode
//        if (product.productCode.contains("."))
//            code = product.productCode.split(".")[0]
//        product.firebaseID = code.toInt()
//        paymentRef.child("details").child("${code}").setValue(product)
//        nroItem += 1
//
//        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm")
//        val currentDate = sdf.format(Date())
//
//        paymentRef.child("userID").setValue(preference.getData("userID").toInt())
//        paymentRef.child("userName").setValue(preference.getData("userName"))
//        paymentRef.child("lastPickingDate").setValue(currentDate)
//        paymentRef.child("clientFullName").setValue(person.fullName)
//        paymentRef.child("total").setValue(textViewTotal.text.toString().toDouble())
//    }


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

    private fun addInfo(){
        val inflater = LayoutInflater.from(globalContext)
        val v = inflater.inflate(R.layout.dialog_confirm_quotation, null)
        layoutListItem = v.findViewById(R.id.layoutList)
        textViewDialogTotal = v.findViewById(R.id.textViewDialogTotal)
        textViewDialogItems = v.findViewById(R.id.textViewDialogItems)
        autoCompletePhysicalDistribution = v.findViewById(R.id.autoCompletePhysicalDistribution)
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
//                        recyclerViewProductStore.adapter?.notifyDataSetChanged()
                        recyclerViewProductStore.adapter?.notifyItemChanged(item.positionInAdapter)
                    }

                }
            }

        }

        val dialogClose = v.findViewById<Button>(R.id.dialog_close)
        val dialogSave = v.findViewById<Button>(R.id.dialog_save)


        val addDialog = AlertDialog.Builder(globalContext)
        addDialog.setView(v)
        addDialog.setTitle("RESUMEN DE PREVENTA")

        val dialog: AlertDialog = addDialog.create()
        dialog.show()
        dialogClose.setOnClickListener{
            dialog.dismiss()
        }
        dialogSave.setOnClickListener{
//            val payed = textViewSubtotal.text.toString().toDouble()
            val totalSale = textViewTotal.text.toString().toDouble()
            if (totalSale >= 0 ) {
                dialogSave.isEnabled = false
                registerRestQuotation()
                Toast.makeText(globalContext, "Cotizacion registrada.", Toast.LENGTH_SHORT).show()

            }else {
                Toast.makeText(globalContext, "Verificar cotizacion.", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }
    }


    private fun registerRestQuotation(){
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
                    findNavController().navigate(R.id.action_quotationFragment_to_saleRealizedFragment, bundle)

//                    val paymentRef = pickingReference.child("gangs").child(preference.getData("gangID")).child("users").child(preference.getData("userID"))
//                    paymentRef.setValue(null)
                }

            }

            override fun onFailure(call: Call<Operation>, t: Throwable) {
                Log.d("MIKE", "sendApiQuotation onFailure: " + t.message.toString())
            }

        })

    }


}