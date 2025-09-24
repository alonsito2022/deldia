package com.sys4soft.deldia.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.sys4soft.deldia.R
import com.sys4soft.deldia.models.AssignedGang

class GangMultiSelectAdapter(
    private val gangList: ArrayList<AssignedGang>,
    private val onGangSelectionChanged: (selectedGangs: ArrayList<Int>) -> Unit
) : RecyclerView.Adapter<GangMultiSelectAdapter.GangViewHolder>() {

    private val selectedGangs = ArrayList<Int>()

    class GangViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val checkBoxGang: CheckBox = itemView.findViewById(R.id.checkBoxGang)
        val textViewGangName: TextView = itemView.findViewById(R.id.textViewGangName)
        val textViewWarehouseName: TextView = itemView.findViewById(R.id.textViewWarehouseName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GangViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_gang_multiselect, parent, false)
        return GangViewHolder(view)
    }

    override fun onBindViewHolder(holder: GangViewHolder, position: Int) {
        val gang = gangList[position]
        
        holder.textViewGangName.text = gang.name
        holder.textViewWarehouseName.text = gang.warehouse?.warehouseName ?: ""
        
        holder.checkBoxGang.isChecked = selectedGangs.contains(gang.gangID)
        
        holder.checkBoxGang.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (!selectedGangs.contains(gang.gangID)) {
                    selectedGangs.add(gang.gangID)
                }
            } else {
                selectedGangs.remove(gang.gangID)
            }
            onGangSelectionChanged(selectedGangs)
        }
        
        holder.itemView.setOnClickListener {
            holder.checkBoxGang.isChecked = !holder.checkBoxGang.isChecked
        }
    }

    override fun getItemCount(): Int = gangList.size

    fun getSelectedGangs(): ArrayList<Int> = selectedGangs

    fun setSelectedGangs(gangs: ArrayList<Int>) {
        selectedGangs.clear()
        selectedGangs.addAll(gangs)
        notifyDataSetChanged()
    }

    fun clearSelection() {
        selectedGangs.clear()
        notifyDataSetChanged()
    }
}
