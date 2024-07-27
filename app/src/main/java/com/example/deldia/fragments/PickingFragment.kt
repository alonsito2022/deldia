package com.example.deldia.fragments

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.deldia.R
import com.example.deldia.adapter.FirebaseGangAdapter
import com.example.deldia.adapter.FirebasePickingDetailAdapter
import com.example.deldia.adapter.ProductStoreAdapter
import com.example.deldia.localdatabase.Preference
import com.example.deldia.models.*
import com.example.deldia.retrofit.UserApiService
import com.google.firebase.database.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.ArrayList

class PickingFragment : Fragment() {
    private var globalContext: Context? = null
    private var user: User = User()
    private var selectedUser: Int = 0
    private var operation: Operation = Operation()
    private lateinit var preference: Preference
    private lateinit var database: FirebaseDatabase
    private lateinit var pickingReference: DatabaseReference
    private lateinit var recyclerViewPickingGangUser: RecyclerView
    private lateinit var recyclerViewPickingDetail: RecyclerView
    private var firebasePickingDetailAdapter: FirebasePickingDetailAdapter = FirebasePickingDetailAdapter(mutableMapOf())
    private lateinit var btnClose: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        globalContext = this.activity
        preference = Preference(globalContext)
        val bundle = arguments
        user.userID= bundle!!.getInt("userID")
        selectedUser =  user.userID
        user.gang.gangID = preference.getData("gangID").toInt()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_picking, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerViewPickingGangUser = view.findViewById(R.id.recyclerViewPickingGangUser)

        database = FirebaseDatabase.getInstance()
        pickingReference = database.getReference("picking")
//        paymentRef.setValue(operation)
        loadPickingByGang()
    }

    private fun loadPickingByGang(){
        pickingReference.child("gangs").child(user.gang.gangID.toString()).child("users").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                val gang = FirebaseGang()
                for (productSnapshot in snapshot.children){
                    Log.d("MIKE", "productSnapshot: $productSnapshot")

                    val userTemp = productSnapshot.getValue(FirebaseGang.User::class.java)!!
                    gang.users[userTemp.userID] = userTemp
                }

                if(firebasePickingDetailAdapter.getItemCount() == 0){
                    Log.d("MIKE", "firebasePickingDetailAdapter hasn't items")
                }else{
                    Log.d("MIKE", "firebasePickingDetailAdapter has items ${gang.users.filter { (k, v) -> k == selectedUser }.isNotEmpty()}")
                    if(gang.users.filter { (k, v) -> k == selectedUser }.isNotEmpty()){
                        firebasePickingDetailAdapter = FirebasePickingDetailAdapter(gang.users[selectedUser]?.details!!.filter  { (_, v) -> v.quantity > 0 } as MutableMap<String, Product>)
                        recyclerViewPickingDetail.adapter=firebasePickingDetailAdapter
                        firebasePickingDetailAdapter.notifyDataSetChanged()
                    }
                    else{
                         firebasePickingDetailAdapter = FirebasePickingDetailAdapter(mutableMapOf())
                        recyclerViewPickingDetail.adapter=firebasePickingDetailAdapter
                        firebasePickingDetailAdapter.notifyDataSetChanged()
                    }

                }

                recyclerViewPickingGangUser.layoutManager = LinearLayoutManager(activity)
                recyclerViewPickingGangUser.setHasFixedSize(true)
                recyclerViewPickingGangUser.adapter = FirebaseGangAdapter(
                    globalContext!!,
                    gang.users as MutableMap<Int, FirebaseGang.User>,
                    object : FirebaseGangAdapter.OnItemClickListener {
                        override fun onItemClick(model: FirebaseGang.User) {
                            selectedUser = model.userID
                            addInfo(model)

                        }
                    }
                )
            }

            override fun onCancelled(error: DatabaseError) {
                Log.d("MIKE", error.message)
            }
        })

    }
    private fun addInfo(d: FirebaseGang.User) {
        val inflater = LayoutInflater.from(globalContext)
        val v = inflater.inflate(R.layout.dialog_firebase_picking, null)
        recyclerViewPickingDetail = v.findViewById<RecyclerView>(R.id.recyclerViewPickingDetail)
        btnClose = v.findViewById(R.id.dialog_close)
        recyclerViewPickingDetail.layoutManager = LinearLayoutManager(activity)
        recyclerViewPickingDetail.setHasFixedSize(true)

//        firebasePickingDetailAdapter = FirebasePickingDetailAdapter((d.details.filter  { (_, v) -> v.quantity > 0 } as MutableMap<String, Product>).toList().sortedBy { (key, value) -> value }.toMap())
        firebasePickingDetailAdapter = FirebasePickingDetailAdapter(d.details.filter  { (_, v) -> v.quantity > 0 }.toList().sortedBy { (_, value) ->
            value.firebaseID

        }.toMap() as MutableMap<String, Product>)
        recyclerViewPickingDetail.adapter = firebasePickingDetailAdapter

        val addDialog = AlertDialog.Builder(globalContext)
        addDialog.setView(v)
        addDialog.create()
        val dialog: AlertDialog = addDialog.create()
        dialog.show()
        btnClose.setOnClickListener{
            dialog.dismiss()
        }
    }


}