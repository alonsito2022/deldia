package com.example.deldia.adapter

import android.content.Context
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.deldia.R
import com.example.deldia.models.Operation

class OperationAdapter(val c: Context, val dataSet: ArrayList<Operation>, private val mListener: OnMenuItemClickListener?) : RecyclerView.Adapter<OperationAdapter.ViewHolder>() {
    interface OnMenuItemClickListener {
        fun print(model: Operation)
        fun cancelSale(model: Operation)
        fun cancelPresale(model: Operation)
        fun show(model: Operation)

        fun generateQuotation(model: Operation)
        fun generateDispatch(model: Operation)
        fun showDetail(model: Operation)
        fun deleteOperation(model: Operation)
    }
    private var context: Context? = null

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        val cardOperationDate: TextView
        val cardOperationClientFullName: TextView
        val cardOperationUserFullName: TextView
        val cardOperationDocumentNumber: TextView
        val cardClientDocumentNumber: TextView
        val cardClientDocumentType: TextView
        val cardClientVisitDay: TextView
        val cardClientGangName: TextView
        val cardOperationTotal: TextView
        val cardClientObservation: TextView
        val cardClientCellphone: TextView
        val showMenu: ImageView
        val view: View
        init {
            cardOperationDate = itemView.findViewById(R.id.cardOperationDate)
            cardOperationClientFullName = itemView.findViewById(R.id.cardOperationClientFullName)
            cardOperationUserFullName = itemView.findViewById(R.id.cardOperationUserFullName)
            cardOperationDocumentNumber = itemView.findViewById(R.id.cardOperationDocumentNumber)
            cardClientDocumentNumber = itemView.findViewById(R.id.cardClientDocumentNumber)
            cardClientDocumentType = itemView.findViewById(R.id.cardClientDocumentType)
            cardClientVisitDay = itemView.findViewById(R.id.cardClientVisitDay)
            cardClientGangName = itemView.findViewById(R.id.cardClientGangName)
            cardOperationTotal = itemView.findViewById(R.id.cardOperationTotal)
            cardClientObservation = itemView.findViewById(R.id.cardClientObservation)
            cardClientCellphone = itemView.findViewById(R.id.cardClientCellphone)
            showMenu = itemView.findViewById(R.id.showMenu)
            view = itemView

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_operation_view, parent, false)
        context = parent.context
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = dataSet[position]
        var docNumber = "SIN GUARDAR"
        if(item.documentNumber.isNotBlank()) docNumber = item.documentNumber
        holder.cardOperationDate.text = item.operationDate
        holder.cardOperationClientFullName.text = item.clientFullName
        holder.cardOperationUserFullName.text = item.userFullName
        holder.cardOperationDocumentNumber.text = docNumber
        holder.cardClientDocumentType.text = "${ item.clientDocumentType }: "
        holder.cardClientDocumentNumber.text = item.clientDocumentNumber
        holder.cardClientVisitDay.text = item.clientVisitDayDisplay
        holder.cardClientGangName.text = "RUTA: ${ item.clientGangName }"
        holder.cardClientObservation.text = "CODIGO RUTA: ${ item.clientObservation }"
        holder.cardClientCellphone.text = "CELULAR: ${ item.clientCellphone }"
        holder.cardOperationTotal.text = "S/${item.total}"
        holder.showMenu.setOnClickListener { popupMenus(holder.view, position) }
    }
    private fun popupMenus(v:View, position: Int){
//        val position = dataSet[position]
        Log.e("MIKE", "popupMenus documentType: ${dataSet[position].documentType}")
        val popupMenus = PopupMenu(c, v, Gravity.LEFT)
        popupMenus.inflate(R.menu.dispatch_menu)

        popupMenus.menu.findItem(R.id.deleteOperation).isVisible = false

        if (dataSet[position].documentType=="05"){ // quotation
            popupMenus.menu.findItem(R.id.printDispatch).isVisible = true  // view preview
            popupMenus.menu.findItem(R.id.showDispatch).isVisible = dataSet[position].routeStatus=="04"  // confirm
            popupMenus.menu.findItem(R.id.cancelPresale).isVisible = false
            popupMenus.menu.findItem(R.id.cancelDispatch).isVisible = false
            popupMenus.menu.findItem(R.id.generateQuotation).isVisible = false
            popupMenus.menu.findItem(R.id.generateDispatch).isVisible = false
            popupMenus.menu.findItem(R.id.showDetail).isVisible = false
        }
        else {

            if (dataSet[position].documentType == "06") {  // no saved
                popupMenus.menu.findItem(R.id.printDispatch).isVisible = false
                popupMenus.menu.findItem(R.id.showDispatch).isVisible = false
                popupMenus.menu.findItem(R.id.cancelPresale).isVisible = false
                popupMenus.menu.findItem(R.id.cancelDispatch).isVisible = false
                popupMenus.menu.findItem(R.id.generateQuotation).isVisible = true
                popupMenus.menu.findItem(R.id.generateDispatch).isVisible = false
                popupMenus.menu.findItem(R.id.showDetail).isVisible = true
            } else {  // sale documentType = [01,02,03]
                popupMenus.menu.findItem(R.id.printDispatch).isVisible = true  // view preview
                popupMenus.menu.findItem(R.id.showDispatch).isVisible = false
                popupMenus.menu.findItem(R.id.cancelPresale).isVisible = false
                popupMenus.menu.findItem(R.id.cancelDispatch).isVisible = true  // annul sale
                popupMenus.menu.findItem(R.id.generateQuotation).isVisible = false
                popupMenus.menu.findItem(R.id.generateDispatch).isVisible = false
                popupMenus.menu.findItem(R.id.showDetail).isVisible = false
            }
        }
        popupMenus.setOnMenuItemClickListener {

            when(it.itemId){
                R.id.printDispatch->{
                    mListener!!.print(dataSet[position])
                    true
                }
                R.id.cancelDispatch->{
                    mListener!!.cancelSale(dataSet[position])
                    true
                }
                R.id.showDispatch->{
                    mListener!!.show(dataSet[position])
                    true
                }
                R.id.cancelPresale->{
                    mListener!!.cancelPresale(dataSet[position])
                    true
                }
                R.id.generateQuotation->{
                    mListener!!.generateQuotation(dataSet[position])
                    true
                }
                R.id.generateDispatch->{
                    mListener!!.generateDispatch(dataSet[position])
                    true
                }
                R.id.showDetail->{
                    mListener!!.showDetail(dataSet[position])
                    true
                }
                R.id.deleteOperation->{
                    mListener!!.deleteOperation(dataSet[position])
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
    override fun getItemCount(): Int {
        return dataSet.size
    }

}