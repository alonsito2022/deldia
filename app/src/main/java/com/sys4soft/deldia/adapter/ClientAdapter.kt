package com.sys4soft.deldia.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.sys4soft.deldia.R
import com.sys4soft.deldia.models.Person

class ClientAdapter(val dataSet: ArrayList<Person>, private val mListener: OnItemClickListener?) : RecyclerView.Adapter<ClientAdapter.ViewHolder>() {

    interface OnItemClickListener {
        fun editClient(model: Person)
        fun dispatchClient(model: Person)
        fun searchSales(model: Person)
    }
    private var context: Context? = null

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        val cardPersonFullName: TextView

        val cardVisitDay: TextView
        val cardAddressName: TextView
        val cardGangName: TextView
        val cardViewPerson: CardView

        val cardEditClient: ImageView
        val cardDispatchClient: ImageView
        val cardSearchSales: ImageView
        val view: View
        init {
            cardPersonFullName = itemView.findViewById(R.id.cardPersonFullName)
            cardEditClient = itemView.findViewById(R.id.cardEditClient)
            cardDispatchClient = itemView.findViewById(R.id.cardDispatchClient)
            cardSearchSales = itemView.findViewById(R.id.cardSearchSales)
            cardVisitDay = itemView.findViewById(R.id.cardVisitDay)
            cardAddressName = itemView.findViewById(R.id.cardAddressName)
            cardGangName = itemView.findViewById(R.id.cardGangName)
            cardViewPerson = itemView.findViewById(R.id.cardViewPerson)
            view = itemView

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_person_view, parent, false)
        context = parent.context
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = dataSet[position]

        holder.cardPersonFullName.text = item.fullName

        val valueDT = item.documentType
        var nameDT = "NA"
        if (valueDT == "01"){nameDT="DNI"}
        else if (valueDT == "06"){nameDT="RUC"}
        var address =""
        if (!item.address.isNullOrEmpty())
            address = item.address.uppercase()
        var gangName =""
        if (!item.gangName.isNullOrEmpty())
            gangName = item.gangName.uppercase()
        holder.cardVisitDay.text = "DIA DE VISITA: ${item.visitDayDisplay}"
        holder.cardAddressName.text = "DIRECCION: ${address}"
        holder.cardGangName.text = "CUADRILLA: ${gangName}"
//        holder.view.setBackgroundColor(ContextCompat.getColor(context!!, R.color.danger))

        if(item.isEnabled) {
            holder.cardPersonFullName.setTextColor(
                ContextCompat.getColor(
                    context!!,
                    R.color.indigo
                )
            )
            holder.cardDispatchClient.isEnabled = true

        }
        else {
            holder.cardPersonFullName.setTextColor(
                ContextCompat.getColor(
                    context!!,
                    R.color.danger
                )
            )
            holder.cardDispatchClient.isEnabled = false
        }

        holder.cardEditClient.setOnClickListener {
            mListener!!.editClient(item)
        }
        holder.cardDispatchClient.setOnClickListener {
            mListener!!.dispatchClient(item)
        }
        holder.cardSearchSales.setOnClickListener {
            mListener!!.searchSales(item)
        }

    }


    override fun getItemCount(): Int {
        return dataSet.size
    }

}