package com.sys4soft.deldia.adapter

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.Guideline
import androidx.recyclerview.widget.RecyclerView
import com.sys4soft.deldia.R
import com.sys4soft.deldia.models.Product
import com.squareup.picasso.Callback
import com.squareup.picasso.NetworkPolicy
import com.squareup.picasso.Picasso
import kotlin.math.roundToInt

class ProductStoreAdapter(var dataSet: ArrayList<Product>): RecyclerView.Adapter<ProductStoreAdapter.ViewHolder>() {
    private var context: Context? = null
    private var onQuantityChangeListener: OnQuantityChangeListener? = null
    private var picasso: Picasso

    init {
        // Configurar Picasso con caché
        picasso = Picasso.get()
        picasso.setIndicatorsEnabled(false) // Desactivar indicadores de debug
    }

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
    private fun loadImage(imageUrl: String, imageView: ImageView) {
        // Primero intentar cargar desde caché
        picasso.load(imageUrl.replace(".png", "_thumb_100.png"))
            .networkPolicy(NetworkPolicy.OFFLINE)
            .placeholder(R.drawable.ic_baseline_reorder_24) // Placeholder mientras carga
            .error(R.drawable.ic_baseline_cancel_24) // Imagen en caso de error
            .into(imageView, object : Callback {
                override fun onSuccess() {
                    // Imagen cargada exitosamente desde caché
                }

                override fun onError(e: Exception) {
                    // Si falla la carga desde caché, intentar desde la red
                    picasso.load(imageUrl.replace(".png", "_thumb_100.png"))
                        .placeholder(R.drawable.ic_baseline_reorder_24)
                        .error(R.drawable.ic_baseline_cancel_24)
                        .into(imageView)
                }
            })
    }
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = dataSet[position]
//        Picasso.get().load(item.productPath.replace(".png", "_thumb_100.png")).into(holder.cardProductPath)
        if(!item.showImage) {
            holder.cardProductPath.visibility = View.GONE
            holder.guidelineStockV20.setGuidelinePercent(0.025f)
            holder.cardProductName.textSize = coverPixelToDP(10)
        }
        else {
            holder.cardProductPath.visibility = View.VISIBLE
            holder.guidelineStockV20.setGuidelinePercent(0.2f)
            holder.cardProductName.textSize = coverPixelToDP(8)
            loadImage(item.productPath, holder.cardProductPath)
        }
        holder.cardViewProduct.startAnimation(AnimationUtils.loadAnimation(holder.itemView.context, R.anim.anim_four))
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