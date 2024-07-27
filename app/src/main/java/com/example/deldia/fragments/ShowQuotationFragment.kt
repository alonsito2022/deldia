package com.example.deldia.fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.deldia.R
import com.example.deldia.adapter.OperationDetailAdapter
import com.example.deldia.adapter.ProductStoreAdapter
import com.example.deldia.models.Operation
import com.example.deldia.retrofit.UserApiService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ShowQuotationFragment : Fragment() {

    private var operation: Operation = Operation()
    private var globalContext: Context? = null
    private lateinit var recyclerViewOperationDetail: RecyclerView
    private lateinit var operationDetailAdapter: OperationDetailAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        globalContext = this.activity

        val bundle = arguments
        operation.operationID = bundle!!.getInt("operationID")
        loadOperation(operation)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_show_quotation, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerViewOperationDetail = view.findViewById(R.id.recyclerViewOperationDetail)

    }

    private fun loadOperation(o: Operation) {

        val apiInterface = UserApiService.create().getSaleByID(o)
        apiInterface.enqueue(object : Callback<Operation> {
            override fun onResponse(call: Call<Operation>, response: Response<Operation>) {

                if (response.body() != null) {
                    operation = response.body()!!

                    recyclerViewOperationDetail.layoutManager = LinearLayoutManager(globalContext)
                    recyclerViewOperationDetail.setHasFixedSize(true)
                    operationDetailAdapter = OperationDetailAdapter(operation.details, object: OperationDetailAdapter.OnItemClickListener{

                        override fun onItemClick(model: Operation.OperationDetail, position: Int) {
                            Toast.makeText(globalContext, "loadOperation operationDetailAdapter: ${model.productSaleName}", Toast.LENGTH_SHORT).show()
                        }

                    })
                    recyclerViewOperationDetail.adapter = operationDetailAdapter

                }
            }

            override fun onFailure(call: Call<Operation>, t: Throwable) {
                Log.d("MIKE", "loadOperation. Algo salio mal..." + t.message.toString())
            }
        })
    }
}