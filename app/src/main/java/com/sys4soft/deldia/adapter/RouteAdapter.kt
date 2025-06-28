package com.sys4soft.deldia.adapter

import android.content.Context
import android.content.res.ColorStateList
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.sys4soft.deldia.R
import com.sys4soft.deldia.models.Person

class RouteAdapter(val c: Context, var dataSet: ArrayList<Person>, private val mListener: OnMenuItemClickListener?) : RecyclerView.Adapter<RouteAdapter.ViewHolder>() {
    interface OnMenuItemClickListener {
        fun editClient(model: Person)
        fun makeAnOrder(model: Person)
        fun withoutOrdering(model: Person)
    }
    private var context: Context? = null

    fun getFilter(filterDataSet: ArrayList<Person>){
        dataSet = filterDataSet
        notifyDataSetChanged()
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        val cardRouteDate: TextView
        val cardWeekDayName: TextView
        val cardClientAddress: TextView
        val cardClientFullName: TextView
        val cardClientDocumentNumber: TextView
        val cardClientDocumentType: TextView
        val cardStatusDisplay: TextView
        val cardStatus: ImageView
        val moreMenu: ImageView
        val view: View
        init {
            cardRouteDate = itemView.findViewById(R.id.cardRouteDate)
            cardWeekDayName = itemView.findViewById(R.id.cardWeekDayName)
            cardClientAddress = itemView.findViewById(R.id.cardClientAddress)
            cardClientFullName = itemView.findViewById(R.id.cardClientFullName)
            cardClientDocumentNumber = itemView.findViewById(R.id.cardClientDocumentNumber)
            cardClientDocumentType = itemView.findViewById(R.id.cardClientDocumentType)
            moreMenu = itemView.findViewById(R.id.moreMenu)

            cardStatusDisplay = itemView.findViewById(R.id.cardStatusDisplay)
            cardStatus = itemView.findViewById(R.id.cardStatus)
            view = itemView

        }
    }
    private fun popupMenus(v:View, position: Int){
        val popupMenus = PopupMenu(c, v, Gravity.RIGHT)
        popupMenus.inflate(R.menu.road_menu)
        popupMenus.setOnMenuItemClickListener {
            when(it.itemId){
                R.id.editClient->{
                    mListener!!.editClient(dataSet[position])
                    true
                }
                R.id.makeAnOrder->{
                    mListener!!.makeAnOrder(dataSet[position])
                    true
                }
                R.id.withoutOrdering->{
                    mListener!!.withoutOrdering(dataSet[position])
                    true
                }
                else-> true
            }
        }
        try {
            val popup = PopupMenu::class.java.getDeclaredField("mPopup")
            popup.isAccessible = true
            val menu = popup.get(popupMenus)
            menu.javaClass.getDeclaredMethod("setForceShowIcon", Boolean::class.java).invoke(menu, true)
        }catch (e:Exception){
            Log.e("Mike", "Error showing menu icons.", e)
        }finally {
            popupMenus.show()
        }
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_road_view, parent, false)
        context = parent.context
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = dataSet[position]
        holder.cardRouteDate.text = item.routeDate
        holder.cardWeekDayName.text = "${item.visitDayDisplay}: "
        holder.cardClientAddress.text = item.address
        holder.cardClientFullName.text = item.fullName
        holder.cardClientDocumentNumber.text = item.documentNumber
        holder.cardClientDocumentType.text = item.documentType
        holder.cardStatusDisplay.text = item.routeStatusDisplay

        if (item.routeStatusDisplay == "COMPLETADO"){
            holder.cardStatus.setBackgroundResource(R.drawable.ic_baseline_check_circle_24)
            holder.cardStatus.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(c, R.color.success))
        }else if (item.routeStatusDisplay == "CANCELADO"){
            holder.cardStatus.setBackgroundResource(R.drawable.ic_baseline_cancel_24)
            holder.cardStatus.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(c, R.color.danger))
        }
        holder.moreMenu.setOnClickListener { popupMenus(holder.view, position) }
    }

    override fun getItemCount(): Int {
        return dataSet.size
    }
}