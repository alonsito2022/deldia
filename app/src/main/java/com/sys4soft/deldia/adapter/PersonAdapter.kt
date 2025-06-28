package com.sys4soft.deldia.adapter

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.sys4soft.deldia.R
import com.sys4soft.deldia.models.Person

class PersonAdapter(var dataSet: ArrayList<Person>, private val mListener: OnItemClickListener?) : RecyclerView.Adapter<PersonAdapter.ViewHolder>() {

    interface OnItemClickListener {
        fun onItemClick(model: Person)
    }
    fun getFilter(filterDataSet: ArrayList<Person>){
        dataSet = filterDataSet
        notifyDataSetChanged()
    }
    private var context: Context? = null

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        val cardPersonFullName: TextView
        val cardAddressName: TextView
        val cardObservation: TextView
        val cardRouteStatusDisplay: TextView
        val cardRouteDate: TextView
        val cardDocumentType: TextView
        val cardDocumentNumber: TextView
        val constraintPerson: ConstraintLayout
        val view: View
        init {
            cardPersonFullName = itemView.findViewById(R.id.cardPersonFullName)
            cardAddressName = itemView.findViewById(R.id.cardAddressName)
            cardObservation = itemView.findViewById(R.id.cardObservation)
            cardRouteStatusDisplay = itemView.findViewById(R.id.cardRouteStatusDisplay)
            cardRouteDate = itemView.findViewById(R.id.cardRouteDate)
            cardDocumentType = itemView.findViewById(R.id.cardDocumentType)
            cardDocumentNumber = itemView.findViewById(R.id.cardDocumentNumber)
            constraintPerson = itemView.findViewById(R.id.constraintPerson)
            view = itemView

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_person_two_view, parent, false)
        context = parent.context
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = dataSet[position]
        holder.cardPersonFullName.text = item.fullName.lowercase().capitalizeWords()
        if(!item.address.isNullOrEmpty())
            holder.cardAddressName.text = item.address.lowercase().capitalizeWords()
        holder.cardObservation.text = item.observation
        holder.cardRouteStatusDisplay.text = item.routeStatusDisplay
        holder.cardRouteDate.text = item.routeDate
        holder.cardDocumentType.text = "${ item.documentTypeDisplay }: "
        holder.cardDocumentNumber.text = item.documentNumber

        if (!item.isEnabled){
            if (item.documentNumber == "00000000")
                if(isDarkMode(context!!))
                    holder.constraintPerson.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context!!,R.color.danger))
                else
                    holder.constraintPerson.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context!!, R.color.red_light))
        }
        else{
            if (item.documentNumber == "00000000")
                if(isDarkMode(context!!))
                    holder.constraintPerson.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context!!,R.color.purple_700))
                else
                    holder.constraintPerson.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context!!,
                        R.color.warning
                    ))
        }

        when(item.routeStatus){
            "01", "08"-> {
                if (!item.isEnabled){
                    holder.cardObservation.background = ContextCompat.getDrawable(context!!, R.drawable.ic_marker_gray)
                }
                else{
                    if(item.customerType == 0)
                        holder.cardObservation.background = ContextCompat.getDrawable(context!!, R.drawable.ic_marker_blue)
                    else
                        holder.cardObservation.background = ContextCompat.getDrawable(context!!, R.drawable.ic_marker_orange)
                }

            }
            "02"-> holder.cardObservation.background = ContextCompat.getDrawable(context!!, R.drawable.ic_marker_green)
            "03"-> holder.cardObservation.background = ContextCompat.getDrawable(context!!, R.drawable.ic_marker_red)
            "04"-> holder.cardObservation.background = ContextCompat.getDrawable(context!!, R.drawable.ic_marker_yellow)
            "05"-> holder.cardObservation.background = ContextCompat.getDrawable(context!!, R.drawable.ic_marker_sky_blue)
            "06"-> holder.cardObservation.background = ContextCompat.getDrawable(context!!, R.drawable.ic_marker_dark_green)
            "07"-> holder.cardObservation.background = ContextCompat.getDrawable(context!!, R.drawable.ic_marker_fuchsia)
        }

        holder.view.setOnClickListener {
            mListener!!.onItemClick(item)
        }
    }

    override fun getItemCount(): Int {
        return dataSet.size
    }

    private fun isDarkMode(context: Context): Boolean {
        val darkModeFlag = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return darkModeFlag == Configuration.UI_MODE_NIGHT_YES
    }
    private fun String.capitalizeWords(): String = split(" ").map { it.capitalize() }.joinToString(" ")

}