package com.sys4soft.deldia.adapter

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.TextView
import com.sys4soft.deldia.R
import com.sys4soft.deldia.models.Cash
import java.util.*
import kotlin.collections.ArrayList

class CashAdapter(
    private val mContext: Context,
    private val mLayoutResourceId: Int,
    cashes: ArrayList<Cash>,
    private val mListener: CashAdapter.OnItemClickListener?
) : ArrayAdapter<Cash>(mContext, mLayoutResourceId, cashes){

    interface OnItemClickListener {
        fun onItemClick(model: Cash)
    }
    private val cash: MutableList<Cash> = ArrayList(cashes)
    private var allCashes: ArrayList<Cash> = cashes

    override fun getCount(): Int {
        return cash.size
    }
    override fun getItem(position: Int): Cash {
        return cash[position]
    }
    override fun getItemId(position: Int): Long {
        return cash[position].cashID!!.toLong()
    }
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var convertView = convertView
        if (convertView == null) {
            val inflater = (mContext as Activity).layoutInflater
            convertView = inflater.inflate(mLayoutResourceId, parent, false)

        }
        try {
            val cash: Cash = getItem(position)
            val autoCompleteTextViewCashType = convertView!!.findViewById<View>(R.id.autoCompleteTextViewCashType) as TextView
            val autoCompleteTextViewCashName = convertView!!.findViewById<View>(R.id.autoCompleteTextViewCashName) as TextView
            autoCompleteTextViewCashType.text = "S/ ${cash.balance}"
            autoCompleteTextViewCashName.text = cash.name
            convertView.setOnClickListener {
                mListener!!.onItemClick(cash)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return convertView!!
    }
    override fun getFilter(): Filter {
        return object : Filter() {
            override fun convertResultToString(resultValue: Any) :String {
                return (resultValue as Cash).name
            }
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val filterResults = FilterResults()

                if (constraint != null) {
                    val cashSuggestion: MutableList<Cash> = ArrayList()
                    for (cash in allCashes) {
                        // Log.d("MIKE","performFiltering for :  ${cash.toString()}")
                        if (cash.name.lowercase(Locale.getDefault()).contains(constraint.toString().lowercase(
                                Locale.getDefault()))
                        ) {
                            cashSuggestion.add(cash)
                        }
                    }
                    filterResults.values = cashSuggestion
                    filterResults.count = cashSuggestion.size
                }
                return filterResults
            }
            override fun publishResults(
                constraint: CharSequence?,
                results: FilterResults
            ) {
                cash.clear()
                if (results.count > 0) {
                    for (result in results.values as List<*>) {
                        if (result is Cash) {
                            cash.add(result)
                        }
                    }
                    notifyDataSetChanged()
                } else if (constraint == null) {
                    cash.addAll(allCashes)
                    notifyDataSetInvalidated()
                }
            }
        }
    }

}