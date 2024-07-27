package com.example.deldia.adapter

import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.deldia.R
import com.example.deldia.models.Gang
import com.example.deldia.models.User
import java.util.*
import kotlin.collections.ArrayList

class UserAdapter (
    private val mContext: Context,
    private val mLayoutResourceId: Int,
    private var users: ArrayList<User>,
    private val mListener: OnItemClickListener?

) : ArrayAdapter<User>(mContext, mLayoutResourceId, users){

    interface OnItemClickListener {
        fun onItemClick(model: User)
    }
    private val client: MutableList<User> = ArrayList(users)
    private var allClients: ArrayList<User> = users

    fun setData(filterDataSet: ArrayList<User>){
        this.users = filterDataSet
        notifyDataSetChanged()
    }
    override fun getCount(): Int {
        return client.size
    }
    override fun getItem(position: Int): User {
        return client[position]
    }
    override fun getItemId(position: Int): Long {
        return client[position].userID!!.toLong()
    }
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var convertView = convertView
        if (convertView == null) {
            val inflater = (mContext as Activity).layoutInflater
            convertView = inflater.inflate(mLayoutResourceId, parent, false)

        }
        try {
            val client: User = getItem(position)
            val clientAutoCompleteViewPhone = convertView!!.findViewById<View>(R.id.autoCompleteTextViewItemWarehouse) as TextView
            val clientAutoCompleteViewName = convertView!!.findViewById<View>(R.id.autoCompleteTextViewItemName) as TextView
            clientAutoCompleteViewPhone.text = client.gang.warehouseName
            clientAutoCompleteViewName.text = client.fullName
            convertView.setOnClickListener {
                mListener!!.onItemClick(client)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return convertView!!
    }
    override fun getFilter(): Filter {
        return object : Filter() {
            override fun convertResultToString(resultValue: Any) :String {
                return (resultValue as User).fullName
            }
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val filterResults = FilterResults()

                if (constraint != null) {
                    val clientSuggestion: MutableList<User> = ArrayList()
                    for (client in allClients) {
                        // Log.d("MIKE","performFiltering for :  ${client.toString()}")
                        if (client.fullName.lowercase(Locale.getDefault()).contains(constraint.toString().lowercase(
                                Locale.getDefault()))
                        ) {
                            clientSuggestion.add(client)
                        }
                    }
                    filterResults.values = clientSuggestion
                    filterResults.count = clientSuggestion.size
                }
                return filterResults
            }
            override fun publishResults(
                constraint: CharSequence?,
                results: FilterResults
            ) {
                client.clear()
                if (results.count > 0) {
                    for (result in results.values as List<*>) {
                        if (result is User) {
                            client.add(result)
                        }
                    }
                    notifyDataSetChanged()
                } else if (constraint == null) {
                    client.addAll(allClients)
                    notifyDataSetInvalidated()
                }
            }
        }
    }



}