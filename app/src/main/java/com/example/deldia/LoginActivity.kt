package com.example.deldia

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import com.example.deldia.localdatabase.Preference
import com.example.deldia.models.ResponseApi
import com.example.deldia.models.ResponseLogin
import com.example.deldia.models.User
import com.example.deldia.retrofit.UserApiService
import com.example.deldia.databinding.ActivityLoginBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

class LoginActivity : AppCompatActivity(), View.OnClickListener {
    lateinit var preference: Preference

    private lateinit var binding: ActivityLoginBinding

    companion object {
        const val PREF_TOKEN = "TOKEN"
        const val PREF_USER_ID = "userID"
        const val PREF_USER_NAME = "userName"
        const val PREF_USER_DOCUMENT = "userDocument"
        const val PREF_USER_EMAIL = "userEmail"
        const val PREF_SUBSIDIARY_ID = "subsidiaryID"
        const val PREF_SUBSIDIARY_NAME = "subsidiaryName"
        const val PREF_VEHICLE_ID = "vehicleID"
        const val PREF_VEHICLE_LICENSE_PLATE = "vehicleLicensePlate"
        const val PREF_GANG_ID = "gangID"
        const val PREF_GANG_NAME = "gangName"
        const val PREF_TOKEN_DATE = "token"
        const val PREF_IS_STAFF = "isStaff"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root) // This is the only setContentView you need
        supportActionBar?.hide()
        binding.btnLogin.setOnClickListener(this)

        preference = Preference(applicationContext)

        if(preference.getData("TOKEN") != ""){
            startActivity(Intent(applicationContext, MainActivity::class.java))
            finish()
        }
    }

    override fun onClick(p0: View?) {
        when (p0) {
            binding.btnLogin -> login()
        }
    }

    private fun login(){
        val email =  binding.emailLogin.text.toString().trim()
        val password =  binding.passwordLogin.text.toString().trim()
        if(email.isEmpty() || password.isEmpty()){
            showToast("Todos los campos son requeridos")
        }else{

            val user = User()
            user.email = email
            user.password = password
            restAuth(user)
        }
    }

    private  fun restAuth(u: User){
        val apiInterface=UserApiService.create().getAuth(u)
        apiInterface.enqueue(object : Callback<ResponseApi> {
            override fun onResponse(call: Call<ResponseApi>, response: Response<ResponseApi>) {
                response.body()?.let { r ->
                    preference.saveData("TOKEN", r.access)
                    getUser(r.access)
                } ?: run {
                    showToast("Acceso no autorizado o credenciales incorrectas.")
                }
            }

            override fun onFailure(call: Call<ResponseApi>, t: Throwable) {
                Toast.makeText(this@LoginActivity, t.message.toString(), Toast.LENGTH_SHORT).show()
            }
        })
    }
    private fun getUser(t:String){
        var apiInterface= UserApiService.create().getLogin(token="Bearer $t")
        apiInterface.enqueue(object : Callback<ResponseLogin> {
            override fun onResponse(call: Call<ResponseLogin>, response: Response<ResponseLogin>) {
                response.body()?.let { r ->
                    preference.apply {
                        saveData(PREF_USER_ID, r.user_id.toString())
                        saveData(PREF_USER_NAME, r.first_name)
                        saveData(PREF_USER_DOCUMENT, r.document)
                        saveData(PREF_USER_EMAIL, r.email)
                        saveData(PREF_SUBSIDIARY_ID, r.subsidiary.subsidiary_id.toString())
                        saveData(PREF_SUBSIDIARY_NAME, r.subsidiary.name)
                        saveData(PREF_VEHICLE_ID, r.vehicle.vehicleID.toString())
                        saveData(PREF_VEHICLE_LICENSE_PLATE, r.vehicle.vehicleLicensePlate)
                        saveData(PREF_GANG_ID, r.vehicle.gangID.toString())
                        saveData(PREF_GANG_NAME, r.vehicle.gangName.toString())
                        saveData(PREF_TOKEN_DATE, SimpleDateFormat("yyyy-MM-dd").format(Date()).toString())
                        saveData(PREF_IS_STAFF, if (r.is_staff) "true" else "false")
                    }
                    startActivity(Intent(applicationContext, MainActivity::class.java))
                    finish()
                } ?: run {
                    showToast("Error al obtener los datos del usuario.")
                }
            }

            override fun onFailure(call: Call<ResponseLogin>, t: Throwable) {
                Toast.makeText(this@LoginActivity, t.message.toString(), Toast.LENGTH_SHORT).show()
                Log.d("APP", t.message.toString())
            }
        })
    }

    private fun showToast(text:String){
        Toast.makeText(this@LoginActivity, text, Toast.LENGTH_SHORT).show()
    }
}