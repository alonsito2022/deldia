package com.example.deldia.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.deldia.R
import com.example.deldia.models.Product
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView
import android.graphics.Paint
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.KeyEvent
import android.widget.EditText
import com.example.deldia.models.Person
import com.google.android.material.textfield.TextInputEditText

class ProductAdapter(var dataSet: ArrayList<Product>, private val mListener: OnItemClickListener?) : RecyclerView.Adapter<ProductAdapter.ViewHolder>() {
    interface OnItemClickListener {
        fun onItemClick(model: Product)
        fun keyUp(model: Product, position:Int)
    }
    fun getFilter(filterDataSet: ArrayList<Product>){
        dataSet = filterDataSet
        notifyDataSetChanged()
    }
    private var context: Context? = null

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
//        val cardProductPath: ImageView
        val cardProductName: TextView
        val cardProductSku: TextView
        val cardPriceSale: TextView
        val cardProductBrandName: TextView
        val cardProductCode: TextView
        val textViewStarted: TextView
        val textViewTotalDelivered: TextView
        val textViewSold: TextView
        val textResiduary: TextView
//        val textInputEditTextStarted: TextInputEditText
//        val textInputEditTextTotalDelivered: TextInputEditText
//        val textInputEditTextSold: TextInputEditText
//        val textInputEditTextResiduary: TextInputEditText
//        val textInputEditTextFound: TextInputEditText
        val editTextFound: EditText
        val view: View
        val myCustomEditTextListener = MyCustomEditTextListener()
        init {
//            cardProductPath = itemView.findViewById(R.id.cardProductPath)
            cardProductName = itemView.findViewById(R.id.cardProductName)
            cardProductSku = itemView.findViewById(R.id.cardProductSku)
            cardPriceSale = itemView.findViewById(R.id.cardPriceSale)
            cardProductBrandName = itemView.findViewById(R.id.cardProductBrandName)
            cardProductCode = itemView.findViewById(R.id.cardProductCode)
            textViewStarted = itemView.findViewById(R.id.textViewStarted)
            textViewTotalDelivered = itemView.findViewById(R.id.textViewTotalDelivered)
            textViewSold = itemView.findViewById(R.id.textViewSold)
            textResiduary = itemView.findViewById(R.id.textResiduary)
//            textInputEditTextStarted = itemView.findViewById(R.id.textInputEditTextStarted)
//            textInputEditTextTotalDelivered = itemView.findViewById(R.id.textInputEditTextTotalDelivered)
//            textInputEditTextSold = itemView.findViewById(R.id.textInputEditTextSold)
//            textInputEditTextResiduary = itemView.findViewById(R.id.textInputEditTextResiduary)
//            textInputEditTextFound = itemView.findViewById(R.id.textInputEditTextFound)
            editTextFound = itemView.findViewById(R.id.editTextFound)
            editTextFound.addTextChangedListener(myCustomEditTextListener)
            view = itemView

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductAdapter.ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_product_view, parent, false)
        context = parent.context
        return ViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ProductAdapter.ViewHolder, position: Int) {
        val item = dataSet[position]
//        Picasso.get().load(item.productPath).into(holder.cardProductPath)
//        Picasso.get().load(item.productPath.replace(".png", "_thumb_100.png")).into(holder.cardProductPath)

        holder.cardProductCode.text = item.productCode
        holder.cardProductName.text = item.productSaleName
        holder.cardProductSku.text = "SKU: ${item.productSku}"
        holder.textViewStarted.text = item.stockStarted.toInt().toString()
        holder.textViewTotalDelivered.text = item.stockTotalDelivered.toInt().toString()
        holder.textViewSold.text = item.stockSold.toInt().toString()
        holder.textResiduary.text = item.stockResiduary.toInt().toString()

//        holder.textInputEditTextStarted.setText(item.stockStarted.toString())
//        holder.textInputEditTextTotalDelivered.setText(item.stockTotalDelivered.toString())
//        holder.textInputEditTextSold.setText(item.stockSold.toString())
//        holder.textInputEditTextResiduary.setText(item.stockResiduary.toString())
//        holder.textInputEditTextFound.setText(item.stockResiduary.toString())

        holder.editTextFound.setOnKeyListener { v, keycode, event ->
            if (event.action == KeyEvent.ACTION_UP){
                mListener!!.keyUp(item, position)
            }
            false
        }
        holder.myCustomEditTextListener.updatePosition(holder.adapterPosition)
        holder.editTextFound.setText(dataSet[holder.adapterPosition].stockChecked.toInt().toString())
//        holder.editTextFound.setText(item.stockChecked.toInt().toString())
//        if(item.stock <= 0){
//            holder.cardProductStock.paintFlags = Paint.STRIKE_THRU_TEXT_FLAG
//        }
//        else{
//            holder.cardProductStock.paintFlags = Paint.ANTI_ALIAS_FLAG
//            holder.cardProductStock.setTextColor(ContextCompat.getColor(context!!, R.color.secondary_text))
//        }
        holder.cardPriceSale.text = "PRECIO: S/${item.priceSale}"
        holder.cardProductBrandName.text = "MARCA: ${item.productBrandName.uppercase()}"
        holder.view.setOnClickListener {
            mListener!!.onItemClick(item)
        }
    }
    inner class MyCustomEditTextListener : TextWatcher {
        private var position = 0
        fun updatePosition(position: Int) {
            this.position = position
        }

        override fun beforeTextChanged(charSequence: CharSequence, i: Int, i2: Int, i3: Int) {
            // no op
        }

        override fun onTextChanged(charSequence: CharSequence, i: Int, i2: Int, i3: Int) {
            if(charSequence.isNotEmpty()){
                if (charSequence.toString().toInt() >= 0) {
                    dataSet[position].stockChecked = charSequence.toString().toDouble()
                }
                else{
                    dataSet[position].stockChecked = 0.0
                }
            }
            else {
                dataSet[position].stockChecked = 0.0

            }
        }

        override fun afterTextChanged(editable: Editable) {
            // no op
        }
    }
    override fun getItemCount(): Int {
        return dataSet.size
    }
}