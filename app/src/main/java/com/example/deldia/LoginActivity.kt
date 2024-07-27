package com.example.deldia

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Toast
import com.example.deldia.localdatabase.Preference
import com.example.deldia.models.ResponseApi
import com.example.deldia.models.ResponseLogin
import com.example.deldia.models.User
import com.example.deldia.retrofit.UserApiService
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices


import kotlinx.android.synthetic.main.activity_login.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

class LoginActivity : AppCompatActivity(), View.OnClickListener {
    lateinit var preference: Preference



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        supportActionBar?.hide()
        btn_login.setOnClickListener(this)

        preference = Preference(applicationContext)

        if(preference.getData("TOKEN") != ""){
            startActivity(Intent(applicationContext, MainActivity::class.java))
            finish()
        }
    }

    override fun onClick(p0: View?) {
        when(p0){
            btn_login -> {
                login()
            }
        }
    }

    private fun login(){
        val email = email_login.text.toString().trim()
        val password = password_login.text.toString().trim()
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
                if(response.body() != null){
                    val r = response.body()!!
                    preference.saveData("TOKEN",r.access)
                    getUser(r.access)
                }else{
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
                val r = response.body()!!
                preference.saveData("userID",r.user_id.toString())
                preference.saveData("userName",r.first_name)
                preference.saveData("userDocument",r.document)
                preference.saveData("userEmail",r.email)
                preference.saveData("subsidiaryID",r.subsidiary.subsidiary_id.toString())
                preference.saveData("subsidiaryName",r.subsidiary.name)
                preference.saveData("vehicleID",r.vehicle.vehicleID.toString())
                preference.saveData("vehicleLicensePlate",r.vehicle.vehicleLicensePlate)
                preference.saveData("gangID",r.vehicle.gangID.toString())
                preference.saveData("gangName",r.vehicle.gangName.toString())
                preference.saveData("token", SimpleDateFormat("yyyy-MM-dd").format(Date()).toString())
                if(r.is_staff)
                    preference.saveData("isStaff","true")
                else
                    preference.saveData("isStaff","false")
                startActivity(Intent(applicationContext,MainActivity::class.java))
                finish()
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