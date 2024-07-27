package com.example.deldia.adapter

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.TextView
import com.example.deldia.R
import com.example.deldia.models.Gang
import com.example.deldia.models.Product
import java.util.*
import kotlin.collections.ArrayList

class GangAdapter(
    private val mContext: Context,
    private val mLayoutResourceId: Int,
    users: ArrayList<Gang>,
    private val mListener: OnItemClickListener?
) : ArrayAdapter<Gang>(mContext, mLayoutResourceId, users){

    interface OnItemClickListener {
        fun onItemClick(model: Gang)
    }
    private var gang: MutableList<Gang> = ArrayList(users)
    private var allClients: ArrayList<Gang> = users

    fun setData(filterDataSet: MutableList<Gang>){
        this.gang = filterDataSet
        notifyDataSetChanged()
    }

    override fun getCount(): Int {
        return gang.size
    }
    override fun getItem(position: Int): Gang {
        return gang[position]
    }
    override fun getItemId(position: Int): Long {
        return gang[position].gangID!!.toLong()
    }
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var convertView = convertView
        if (convertView == null) {
            val inflater = (mContext as Activity).layoutInflater
            convertView = inflater.inflate(mLayoutResourceId, parent, false)

        }
        try {
            val gang: Gang = getItem(position)
            val textViewVehicleLicensePlate = convertView!!.findViewById<View>(R.id.textViewVehicleLicensePlate) as TextView
            val textViewGangName = convertView!!.findViewById<View>(R.id.textViewGangName) as TextView
            textViewVehicleLicensePlate.text = gang.warehouse.warehouseName
            textViewGangName.text = gang.name
            convertView.setOnClickListener {
                mListener!!.onItemClick(gang)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return convertView!!
    }
    override fun getFilter(): Filter {
        return object : Filter() {
            override fun convertResultToString(resultValue: Any) :String {
                return (resultValue as Gang).name
            }
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val filterResults = FilterResults()

                if (constraint != null) {
                    val gangSuggestion: MutableList<Gang> = ArrayList()
                    for (gang in allClients) {
                        // Log.d("MIKE","performFiltering for :  ${gang.toString()}")
                        if (gang.name.lowercase(Locale.getDefault()).contains(constraint.toString().lowercase(
                                Locale.getDefault()))
                        ) {
                            gangSuggestion.add(gang)
                        }
                    }
                    filterResults.values = gangSuggestion
                    filterResults.count = gangSuggestion.size
                }
                return filterResults
            }
            override fun publishResults(
                constraint: CharSequence?,
                results: FilterResults
            ) {
                gang.clear()
                if (results.count > 0) {
                    for (result in results.values as List<*>) {
                        if (result is Gang) {
                            gang.add(result)
                        }
                    }
                    notifyDataSetChanged()
                } else if (constraint == null) {
                    gang.addAll(allClients)
                    notifyDataSetInvalidated()
                }
            }
        }
    }

}