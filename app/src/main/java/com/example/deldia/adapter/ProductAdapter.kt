package com.example.deldia.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.deldia.R
import com.example.deldia.models.Product
import com.squareup.picasso.Picasso
import com.squareup.picasso.NetworkPolicy

class ProductAdapter(
    var dataSet: ArrayList<Product>,
    private val mListener: OnItemClickListener?
) : RecyclerView.Adapter<ProductAdapter.ViewHolder>() {

    interface OnItemClickListener {
        fun onItemClick(model: Product)
        fun keyUp(model: Product, position: Int)
    }

    private var context: Context? = null
    private lateinit var picasso: Picasso

    init {
        setupPicasso()
    }

    private fun setupPicasso() {
        picasso = Picasso.get()
        picasso.setIndicatorsEnabled(false)
    }

    fun getFilter(filterDataSet: ArrayList<Product>) {
        dataSet = filterDataSet
        notifyDataSetChanged()
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardProductPath: ImageView = itemView.findViewById(R.id.cardProductPath)
        val cardProductName: TextView = itemView.findViewById(R.id.cardProductName)
        val cardProductCode: TextView = itemView.findViewById(R.id.cardProductCode)
        val cardPriceSale: TextView = itemView.findViewById(R.id.cardPriceSale)
        val cardProductBrandName: TextView = itemView.findViewById(R.id.cardProductBrandName)
        val cardStock: TextView = itemView.findViewById(R.id.cardStock)
        val view: View = itemView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        context = parent.context
        val view = LayoutInflater.from(context).inflate(R.layout.item_product_view, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = dataSet[position]
        
        // Cargar imagen con Picasso
        picasso.load(item.productPath.replace(".png", "_thumb_100.png"))
            .networkPolicy(NetworkPolicy.OFFLINE)
            .placeholder(R.drawable.ic_baseline_reorder_24)
            .error(R.drawable.ic_baseline_cancel_24)
            .into(holder.cardProductPath, object : com.squareup.picasso.Callback {
                override fun onSuccess() {
                    // Imagen cargada desde caché
                }
                override fun onError(e: Exception?) {
                    // Si falla la carga desde caché, intentar desde la red
                    picasso.load(item.productPath.replace(".png", "_thumb_100.png"))
                        .placeholder(R.drawable.ic_baseline_reorder_24)
                        .error(R.drawable.ic_baseline_cancel_24)
                        .into(holder.cardProductPath)
                }
            })

        // Configurar textos
        holder.cardProductCode.text = "Código: ${item.productCode}"
        holder.cardProductName.text = item.productSaleName
        holder.cardProductBrandName.text = "Marca: ${item.productBrandName}"
        holder.cardPriceSale.text = "S/ ${item.priceSale}"
        holder.cardStock.text = "Stock: ${item.stock.toInt()}"

        // Configurar click listener
        holder.view.setOnClickListener {
            mListener?.onItemClick(item)
        }
    }

    override fun getItemCount() = dataSet.size
}