package com.sys4soft.deldia.fragments

import android.app.DatePickerDialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputEditText
import com.sys4soft.deldia.R
import com.sys4soft.deldia.adapter.UserAdapter
import com.sys4soft.deldia.localdatabase.Preference
import com.sys4soft.deldia.models.*
import com.sys4soft.deldia.retrofit.UserApiService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class CollectionSheetFragment : Fragment() {

    private var globalContext: Context? = null
    private lateinit var preference: Preference
    private lateinit var dateFormatter: SimpleDateFormat
    private var selectedDate: Date = Date()
    private var selectedUser: User? = null
    private var user: User = User()

    private lateinit var editTextSearchDate: TextInputEditText
    private lateinit var editTextUserFullName: TextInputEditText
    private lateinit var autoCompleteUser: AutoCompleteTextView
    private lateinit var userAdapter: UserAdapter
    private lateinit var textViewTotalPurchased: TextView
    private lateinit var textViewTotalCredit: TextView
    private lateinit var textViewTotalYape: TextView
    private lateinit var textViewTotalReturned: TextView
    private lateinit var btnSearch: Button
    private lateinit var textViewTotalCash: TextView
    private lateinit var textViewTotalPending: TextView
    private lateinit var cardViewTotals: CardView
    private var listUsers = arrayListOf<User>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        globalContext = this.activity
        preference = Preference(globalContext)
        dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_collection_sheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        editTextSearchDate = view.findViewById(R.id.editTextSearchDate)
        editTextUserFullName = view.findViewById(R.id.editTextUserFullName)
        autoCompleteUser = view.findViewById(R.id.autoCompleteUser)
        textViewTotalPurchased = view.findViewById(R.id.textViewTotalPurchased)
        textViewTotalCredit = view.findViewById(R.id.textViewTotalCredit)
        textViewTotalYape = view.findViewById(R.id.textViewTotalYape)
        textViewTotalReturned = view.findViewById(R.id.textViewTotalReturned)
        btnSearch = view.findViewById(R.id.btnSearch)
        textViewTotalCash = view.findViewById(R.id.textViewTotalCash)
        textViewTotalPending = view.findViewById(R.id.textViewTotalPending)
        cardViewTotals = view.findViewById(R.id.cardViewTotals)

        setupDatePicker()
        checkUserRoleAndConfigureFields()
        setupSearchButton()
        
        // Set default date to today
        updateDateDisplay()
    }

    private fun setupDatePicker() {
        editTextSearchDate.setOnClickListener {
            showDatePicker()
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        calendar.time = selectedDate
        
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val newDate = Calendar.getInstance()
                newDate.set(year, month, dayOfMonth)
                selectedDate = newDate.time
                updateDateDisplay()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    private fun updateDateDisplay() {
        editTextSearchDate.setText(dateFormatter.format(selectedDate))
    }

    private fun checkUserRoleAndConfigureFields() {
        val userRoleName = preference.getData("userRoleName")
        
        when (userRoleName.lowercase()) {
            "preventista" -> {
                // Para preventistas: mostrar campos de solo lectura con datos del usuario logueado
                showReadOnlyFields()
            }
            "repartidor" -> {
                // Para repartidores: mostrar campos de solo lectura con datos del usuario logueado
                showReadOnlyFields()
            }
            "administrador" -> {
                // Para administradores: permitir selección de usuario
                showEditableFieldsForAdmin()
            }
            else -> {
                // Para otros roles: mantener funcionalidad normal
                showReadOnlyFields()
            }
        }
    }
    
    private fun showReadOnlyFields() {
        // Ocultar campo editable
        view?.findViewById<View>(R.id.textInputLayoutUser)?.visibility = View.GONE
        
        // Mostrar campo de solo lectura
        view?.findViewById<View>(R.id.textInputLayoutUserReadOnly)?.visibility = View.VISIBLE
        
        // Cargar datos del usuario logueado
        loadCurrentUser()
    }
    
    private fun showEditableFieldsForAdmin() {
        // Mostrar campo editable
        view?.findViewById<View>(R.id.textInputLayoutUser)?.visibility = View.VISIBLE
        
        // Ocultar campo de solo lectura
        view?.findViewById<View>(R.id.textInputLayoutUserReadOnly)?.visibility = View.GONE
        
        // Configurar listener para el AutoCompleteTextView como Spinner
        autoCompleteUser.setOnClickListener {
            autoCompleteUser.showDropDown()
        }
        
        // Deshabilitar la escritura manual para que se comporte como Spinner
        autoCompleteUser.keyListener = null
        autoCompleteUser.isFocusable = false
        autoCompleteUser.isClickable = true
        
        // Cargar lista de usuarios para administradores
        loadAllUsersForAdmin()
    }
    
    private fun loadCurrentUser() {
        user.lastName = preference.getData("userLastName")
        user.firstName = preference.getData("userName")
        user.userID = preference.getData("userID").toInt()
        
        // Set the user full name in the text field
        editTextUserFullName.setText("${user.firstName} ${user.lastName}")
        selectedUser = user
    }
    
    private fun loadAllUsersForAdmin(){
        val apiInterface = UserApiService.create(requireContext()).getAllSellers()
        apiInterface.enqueue(object : Callback<ArrayList<User>> {
            override fun onResponse(call: Call<ArrayList<User>>, response: Response<ArrayList<User>>) {
                listUsers = response.body()!!
                
                // Para administradores: NO agregar opción "TODOS"
                // Solo mostrar usuarios reales para selección específica
                
                userAdapter = UserAdapter(globalContext!!, R.layout.item_user_view, listUsers, object : UserAdapter.OnItemClickListener{
                    override fun onItemClick(model: User) {
                        autoCompleteUser.setText(model.fullName, false)
                        autoCompleteUser.dismissDropDown()
                        selectedUser = model
                        Log.d("CollectionSheetFragment", "Admin seleccionó usuario: ${model.fullName} (ID: ${model.userID})")
                    }
                })
                autoCompleteUser.setAdapter(userAdapter)
                
                // Configurar el dropdown para que se comporte como Spinner
                autoCompleteUser.threshold = 1
                autoCompleteUser.setOnItemClickListener { parent, view, position, id ->
                    val selectedUserItem = userAdapter.getItem(position)
                    selectedUserItem?.let {
                        autoCompleteUser.setText(it.fullName, false)
                        selectedUser = it
                        Log.d("CollectionSheetFragment", "Admin seleccionó usuario: ${it.fullName} (ID: ${it.userID})")
                    }
                }

                // Para administradores: preseleccionar el usuario logueado por defecto
                val currentUserID = preference.getData("userID").toIntOrNull() ?: 0
                
                // Buscar y preseleccionar el usuario actual
                val currentUser = listUsers.find { it.userID == currentUserID }
                if (currentUser != null) {
                    autoCompleteUser.setText(currentUser.fullName, false)
                    selectedUser = currentUser
                    Log.d("CollectionSheetFragment", "Admin preseleccionado: ${currentUser.fullName}")
                }
            }

            override fun onFailure(call: Call<ArrayList<User>>, t: Throwable) {
                Log.e("CollectionSheetFragment", "Error al cargar usuarios para admin: ${t.message}")
                Toast.makeText(requireContext(), "Error al cargar usuarios", Toast.LENGTH_SHORT).show()
            }
        })
    }


    private fun setupSearchButton() {
        btnSearch.setOnClickListener {
            if (selectedUser == null) {
                Toast.makeText(requireContext(), "Usuario no encontrado", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            // Validación adicional para administradores
            val userRoleName = preference.getData("userRoleName")
            if (userRoleName.lowercase() == "administrador") {
                if (autoCompleteUser.text.toString().isEmpty()) {
                    Toast.makeText(requireContext(), "Por favor seleccione un usuario", Toast.LENGTH_SHORT).show()
                    autoCompleteUser.showDropDown()
                    return@setOnClickListener
                }
            }
            
            searchSales()
        }
    }

    private fun searchSales() {
        val request = SalesBySellerRequest().apply {
            userID = selectedUser!!.userID.toString()
            gangID = "0"
            searchDate = dateFormatter.format(selectedDate)
            selectedGangs = arrayListOf()
        }

        val apiInterface = UserApiService.create(requireContext()).getAllSalesBySellers(request)
        apiInterface.enqueue(object : Callback<ArrayList<SaleBySeller>> {
            override fun onResponse(call: Call<ArrayList<SaleBySeller>>, response: Response<ArrayList<SaleBySeller>>) {
                if (response.isSuccessful) {
                    response.body()?.let { sales ->
                        calculateAndDisplayTotals(sales)
                    }
                } else {
                    Toast.makeText(requireContext(), "Error al consultar ventas", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ArrayList<SaleBySeller>>, t: Throwable) {
                Log.d("CollectionSheet", "Error al consultar ventas: ${t.message}")
                Toast.makeText(requireContext(), "Error de conexión", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun calculateAndDisplayTotals(sales: ArrayList<SaleBySeller>) {
        // Filter sales with operationStatus === "02" (COMPLETE)
        val completedSales = sales.filter { it.operationStatus == "02" }
        
        // Calculate sumTotalPurchased
        val sumTotalPurchased = completedSales.sumOf { it.totalPurchased.toDouble() }
        
        // Calculate sumTotalPaid
        val sumTotalPaid = completedSales.sumOf { it.totalPaid.toDouble() }
        
        // Calculate sumTotalPaidInCash
        val sumTotalPaidInCash = completedSales.sumOf { it.totalPaidInCash.toDouble() }
        
        // Calculate sumTotalPaidInYape
        val sumTotalPaidInYape = completedSales.sumOf { it.totalPaidInYape.toDouble() }
        
        // Calculate sumTotalReturned
        val sumTotalReturned = completedSales.sumOf { it.totalReturned.toDouble() }
        
        // Calculate sumTotalInCredit (with additional filters)
        val sumTotalInCredit = sales
            .filter { 
                it.totalPending > 0 && 
                it.dailyRouteStatus == "06" && 
                it.operationStatus == "02" 
            }
            .sumOf { it.totalInCredit.toDouble() }
        
        // Calculate sumTotalPending
        val sumTotalPending = completedSales.sumOf { it.totalPending.toDouble() }

        // Display totals
        textViewTotalPurchased.text = String.format("%.2f", sumTotalPurchased)
        textViewTotalCredit.text = String.format("%.2f", sumTotalInCredit)
        textViewTotalYape.text = String.format("%.2f", sumTotalPaidInYape)
        textViewTotalReturned.text = String.format("%.2f", sumTotalReturned)
        textViewTotalCash.text = String.format("%.2f", sumTotalPaidInCash)
        textViewTotalPending.text = String.format("%.2f", sumTotalPending)

        // Show the card
        cardViewTotals.visibility = View.VISIBLE
    }
}