package com.example.deldia.adapter

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.Guideline
import androidx.recyclerview.widget.RecyclerView
import com.example.deldia.R
import com.example.deldia.models.Product
import com.squareup.picasso.Picasso
import kotlin.math.roundToInt

class DevolutionProductStoreAdapter(var dataSet: ArrayList<Product>): RecyclerView.Adapter<DevolutionProductStoreAdapter.ViewHolder>() {
    private var context: Context? = null
    private var onQuantityChangeListener: OnQuantityChangeListener? = null

    interface OnQuantityChangeListener {
        fun onQuantityChanged(id:Int, quantity: String)
        fun onItemClick(id:Int)
    }

    fun setOnQuantityChangeListener(listener: OnQuantityChangeListener) {
        this.onQuantityChangeListener = listener
    }

    fun getFilter(filterDataSet: ArrayList<Product>){
        this.dataSet = filterDataSet
        notifyDataSetChanged()
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        val cardViewProduct: CardView
        val guidelineStockV20: Guideline
        val cardProductPath: ImageView
        val cardProductName: TextView
        val cardProductSize: TextView
        val cardProductCode: TextView
        val cardStock: TextView
        val cardPriceSale: TextView
        val cardSubtotalSale: TextView
        val editTextQuantity: EditText
        val view: View
        init {
            cardViewProduct = itemView.findViewById(R.id.cardViewProduct)
            guidelineStockV20 = itemView.findViewById(R.id.guideline_stock_V20)
            cardProductPath = itemView.findViewById(R.id.cardProductPath)
            cardProductName = itemView.findViewById(R.id.cardProductName)
            cardProductSize = itemView.findViewById(R.id.cardProductSize)
            cardProductCode = itemView.findViewById(R.id.cardProductCode)
            cardStock = itemView.findViewById(R.id.cardStock)
            cardPriceSale = itemView.findViewById(R.id.cardPriceSale)
            editTextQuantity = itemView.findViewById(R.id.editTextQuantity)
            cardSubtotalSale = itemView.findViewById(R.id.cardSubtotalSale)

            editTextQuantity.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                }

                override fun afterTextChanged(s: Editable?) {
                    var subtotal = 0.0
                    var quantity = 0
                    val ppk = dataSet[bindingAdapterPosition].productID
                    onQuantityChangeListener?.onQuantityChanged(ppk!!, s.toString())
                    if (s.toString().isNotBlank()){

                        quantity = s.toString().toInt()
                        val price = cardPriceSale.text.toString().toDouble()
                        if (ppk!! >= 0 && quantity <= dataSet[bindingAdapterPosition].stock){
                            subtotal = ((price * quantity) * 100.0).roundToInt() / 100.0
                        }
                        else{
                            editTextQuantity.text.clear()
                            Toast.makeText(context, "Stock insuficiente", Toast.LENGTH_SHORT).show()
                        }

                    }
                    cardSubtotalSale.text = subtotal.toString()
                }
            })
            cardProductPath.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val ppk = dataSet[bindingAdapterPosition].productID
                    onQuantityChangeListener?.onItemClick(ppk!!)
                }
            }
            view = itemView

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_product_store_view, parent, false)
        context = parent.context
        return ViewHolder(view)
    }
    private fun coverPixelToDP(dps: Int): Float {
        val scale: Float = context!!.resources.displayMetrics.density
        return (dps * scale)
    }
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = dataSet[position]
        Picasso.get().load(item.productPath.replace(".png", "_thumb_100.png")).into(holder.cardProductPath)
        holder.cardViewProduct.startAnimation(AnimationUtils.loadAnimation(holder.itemView.context, R.anim.anim_four))
        if(!item.showImage) {
            holder.cardProductPath.visibility = View.GONE
            holder.guidelineStockV20.setGuidelinePercent(0.025f)
            holder.cardProductName.textSize = coverPixelToDP(10)
        }
        else {
            holder.cardProductPath.visibility = View.VISIBLE
            holder.guidelineStockV20.setGuidelinePercent(0.2f)
            holder.cardProductName.textSize = coverPixelToDP(8)
        }
        holder.cardProductCode.text = item.productCode
        holder.cardProductName.text = item.productSaleName
        holder.cardProductSize.text = item.productSize
        holder.cardStock.text = item.stock.toInt().toString()
        holder.cardPriceSale.text = item.priceSale.toString()
        holder.editTextQuantity.setText(dataSet[holder.bindingAdapterPosition].quantity.toString())
    }

    override fun getItemCount(): Int {
        return dataSet.size
    }
}