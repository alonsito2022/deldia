package com.sys4soft.deldia.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.sys4soft.deldia.R
import com.sys4soft.deldia.models.FirebaseGang

class FirebaseGangAdapter(val c: Context, val dataSet: MutableMap<Int, FirebaseGang.User>, private val mListener: OnItemClickListener?) : RecyclerView.Adapter<FirebaseGangAdapter.ViewHolder>(){
    interface OnItemClickListener {
        fun onItemClick(model: FirebaseGang.User)
    }
    private var context: Context? = null
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        val cardPickingDate: TextView
        val cardPickingUser: TextView
        val cardPickingItems: TextView
        val cardPickingTotal: TextView
        val cardPickingClient: TextView
        val view: View

        init {
            cardPickingDate = itemView.findViewById(R.id.cardPickingDate)
            cardPickingUser = itemView.findViewById(R.id.cardPickingUser)
            cardPickingItems = itemView.findViewById(R.id.cardPickingItems)
            cardPickingTotal = itemView.findViewById(R.id.cardPickingTotal)
            cardPickingClient = itemView.findViewById(R.id.cardPickingClient)
            view = itemView

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_firebase_user_picking_view, parent, false)
        context = parent.context
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = dataSet[dataSet.keys.elementAt(position)]
        holder.cardPickingDate.text = item!!.lastPickingDate
        holder.cardPickingUser.text = item.userName
        holder.cardPickingItems.text = "${item.details.filter  { (_, v) -> v.quantity > 0 }.count()} ITEMS"
        holder.cardPickingTotal.text = "S/ ${ item.total}"
        holder.cardPickingClient.text = item.clientFullName
        holder.view.setOnClickListener {
            mListener!!.onItemClick(item)
        }
    }

    override fun getItemCount(): Int {
        return dataSet.size
    }
}