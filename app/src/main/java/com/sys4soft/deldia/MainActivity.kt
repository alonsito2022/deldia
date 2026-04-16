package com.sys4soft.deldia

import android.app.*
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.sys4soft.deldia.localdatabase.Preference
import com.sys4soft.deldia.models.User
import com.sys4soft.deldia.retrofit.UserApiService
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
//import kotlinx.android.synthetic.main.activity_main.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.sys4soft.deldia.models.Schedule


class MainActivity : AppCompatActivity() {

    lateinit var toggle: ActionBarDrawerToggle
    lateinit var toolbar: Toolbar
    lateinit var drawerLayout: DrawerLayout
    lateinit var navView: NavigationView
    lateinit var bottomNavigationView: BottomNavigationView
    lateinit var navController: NavController
    lateinit var appBarConfiguration: AppBarConfiguration

    private lateinit var preference: Preference

    private var user: User? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

//        oscar version
//        supportActionBar?.hide();//Ocultar ActivityBar anterior
        toolbar = findViewById(R.id.include2)
        setSupportActionBar(toolbar)
//        oscar version

        preference = Preference(applicationContext)

        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)
        bottomNavigationView = findViewById(R.id.bottomNavigationView)

        navController = findNavController(R.id.fragmentContainerView)
        appBarConfiguration = AppBarConfiguration(
            setOf(R.id.productFragment, R.id.collectionSheetFragment, R.id.mapFragment, R.id.quotationFragment, R.id.saleRealizedFragment, R.id.dispatchFragment, 
            R.id.printActivity),
            drawerLayout
        )
        // the title in the action bar will automatically be updated when the destination changes
//        setupActionBarWithNavController(navController, drawer_layout)
        setupActionBarWithNavController(navController, appBarConfiguration)

        toggle = ActionBarDrawerToggle(this, drawerLayout, R.string.open, R.string.close)
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

//        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val bundle = Bundle()
        bundle.putInt("userID", preference.getData("userID").toInt())
        bundle.putInt("vehicleID", preference.getData("vehicleID").toInt())
        bundle.putString("vehicleLicensePlate", preference.getData("vehicleLicensePlate"))

        bottomNavigationView.setOnItemSelectedListener { item ->
            val destination = when (item.itemId) {
                R.id.nav_products -> R.id.productFragment
                R.id.nav_collection_sheet -> R.id.collectionSheetFragment
                R.id.nav_map -> R.id.mapFragment
                R.id.nav_sales_realized -> {
                    // Verificar si el usuario tiene permisos para acceder a Pedidos
                    val userRoleName = preference.getData("userRoleName")
                    if (userRoleName.equals("REPARTIDOR", ignoreCase = true)) {
                        null // No permitir acceso para REPARTIDOR
                    } else {
                        R.id.saleRealizedFragment
                    }
                }
                else -> null
            }
            destination?.let { navController.navigate(it, bundle) }
            true
        }

        navView.setNavigationItemSelectedListener {

            it.isChecked = true
            drawerLayout.closeDrawers()

            when(it.itemId){
                R.id.clientFragment -> navController.navigate(R.id.clientFragment, bundle)
                R.id.productFragment -> navController.navigate(R.id.productFragment, bundle)
                R.id.collectionSheetFragment -> navController.navigate(R.id.collectionSheetFragment, bundle)
                R.id.mapFragment -> navController.navigate(R.id.mapFragment, bundle)
                R.id.saleRealizedFragment -> {
                    // Verificar si el usuario tiene permisos para acceder a Pedidos
                    val userRoleName = preference.getData("userRoleName")
                    if (!userRoleName.equals("REPARTIDOR", ignoreCase = true)) {
                        navController.navigate(R.id.saleRealizedFragment, bundle)
                    }
                }
                R.id.pickingFragment -> navController.navigate(R.id.pickingFragment, bundle)
                R.id.transferFragment -> navController.navigate(R.id.transferFragment, bundle)
                R.id.exchangeFragment -> navController.navigate(R.id.exchangeFragment, bundle)
                R.id.cashFragment -> navController.navigate(R.id.cashFragment, bundle)
                R.id.chartFragment -> navController.navigate(R.id.chartFragment, bundle)
                R.id.profileFragment -> navController.navigate(R.id.profileFragment, bundle)
                R.id.nav_logout -> {
                    preference.clearPreference()
                    GoToActivityAsNewTask(this, LoginActivity::class.java)
                }
            }
            true
        }

        val goToRoute = intent.getBooleanExtra("GO_TO_ROUTE", false)
        if (goToRoute) { navController.navigate(R.id.routeFragment, bundle)}
        val goToCollectionSheet = intent.getBooleanExtra("GO_TO_COLLECTION_SHEET", false)
        if (goToCollectionSheet) { navController.navigate(R.id.collectionSheetFragment, bundle)}

        val goToMap = intent.getBooleanExtra("GO_TO_MAP", false)
        if (goToMap) { navController.navigate(R.id.mapFragment, bundle)}

        val goToOrders = intent.getBooleanExtra("GO_TO_ORDERS", false)
        if (goToOrders) { navController.navigate(R.id.saleRealizedFragment, bundle)}

        loadUser(preference.getData("userID").toInt())
        
        // Configurar navegación según el rol del usuario
        configureNavigationByRole()

        // Sincronizar y verificar horarios
        fetchSchedules()
    }

    override fun onResume() {
        super.onResume()
        // Verificar horario cada vez que la app vuelve al primer plano
        checkWorkTime()
    }
    
    private fun configureNavigationByRole() {
        val userRoleName = preference.getData("userRoleName")
        
        if (userRoleName.equals("REPARTIDOR", ignoreCase = true)) {
            // Ocultar la opción "Pedidos" para usuarios REPARTIDOR
            hideBottomNavigationItem(R.id.nav_sales_realized)
            hideDrawerNavigationItem(R.id.saleRealizedFragment)
        }
    }
    
    private fun hideBottomNavigationItem(itemId: Int) {
        val menu = bottomNavigationView.menu
        val item = menu.findItem(itemId)
        item?.isVisible = false
    }
    
    private fun hideDrawerNavigationItem(itemId: Int) {
        val menu = navView.menu
        val item = menu.findItem(itemId)
        item?.isVisible = false
    }
    
    private fun validateSession(user: User) {
        val gangSaved = preference.getData("gangID").toInt()
        val gangToday = user.gang.gangID
        val sessionTokenSaved = preference.getData("token")
        val sessionTokenToday = SimpleDateFormat("yyyy-MM-dd").format(Date())

        if (preference.getData("isStaff").isNullOrBlank() && sessionTokenSaved.isNullOrBlank()) {
            preference.clearPreference()
            GoToActivityAsNewTask(this, LoginActivity::class.java)
            return
        }

        if (sessionTokenSaved != sessionTokenToday) {
            Toast.makeText(applicationContext, "Sesión finalizada", Toast.LENGTH_SHORT).show()
            preference.clearPreference()
            GoToActivityAsNewTask(this, LoginActivity::class.java)
            return
        }

        if (gangSaved != gangToday || gangToday == 0) {
            Toast.makeText(applicationContext, "Verificar cuadrilla", Toast.LENGTH_SHORT).show()
            preference.clearPreference()
            GoToActivityAsNewTask(this, LoginActivity::class.java)
            return
        }

        if (preference.getData("isStaff") == "false") checkWorkTime()
    }
    private fun loadUser(id: Int) = if (id > 0){
        val u = User()
        u.userID=id
        val apiInterface = UserApiService.create(applicationContext).getUser(u)
        apiInterface.enqueue(object : Callback<User> {
            override fun onResponse(call: Call<User>, response: Response<User>) {
                response.body()?.let {
                    user = it
//                    validateSession(it)
                }
            }

            override fun onFailure(call: Call<User>, t: Throwable) {
                Log.d("MIKE", "loadUser. Algo salio mal..." + t.message.toString())
            }
        })
    }
    else{
        Toast.makeText(applicationContext, "userID invalido.", Toast.LENGTH_SHORT).show()
    }

    private fun fetchSchedules() {
        val apiInterface = UserApiService.create(applicationContext).getSchedules()
        apiInterface.enqueue(object : Callback<ArrayList<Schedule>> {
            override fun onResponse(call: Call<ArrayList<Schedule>>, response: Response<ArrayList<Schedule>>) {
                if (response.isSuccessful && response.body() != null) {
                    val gson = Gson()
                    val json = gson.toJson(response.body())
                    preference.saveData("SCHEDULES", json)
                    checkWorkTime()
                }
            }
            override fun onFailure(call: Call<ArrayList<Schedule>>, t: Throwable) {
                Log.e("MIKE", "Error al obtener horarios: ${t.message}")
            }
        })
    }

    private fun checkWorkTime() {
        val isStaff = preference.getData("isStaff") == "true"
        if (isStaff) return // Los administradores/staff suelen no tener restricciones aquí o se manejan distinto

        val userRoleName = preference.getData("userRoleName")
        if (userRoleName.isEmpty()) return

        val schedulesJson = preference.getData("SCHEDULES")
        if (schedulesJson.isEmpty()) return

        try {
            val gson = Gson()
            val type = object : TypeToken<ArrayList<Schedule>>() {}.type
            val schedules: ArrayList<Schedule> = gson.fromJson(schedulesJson, type)

            val schedule = schedules.find { it.roleName.equals(userRoleName, ignoreCase = true) }
            
            if (schedule == null || !schedule.hasRestrictions) return

            val now = Calendar.getInstance()
            val currentDayRaw = SimpleDateFormat("EEEE", Locale("es", "ES")).format(now.time).toUpperCase(Locale.ROOT)
            val currentDay = currentDayRaw.replace("Á", "A")
                .replace("É", "E")
                .replace("Í", "I")
                .replace("Ó", "O")
                .replace("Ú", "U")
            val currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(now.time)

            // Validar día
            val allowedDays = schedule.daysOfWeek.split("-")
            val isAllowedDay = allowedDays.any { it.equals(currentDay, ignoreCase = true) }

            if (!isAllowedDay) {
                logoutDueToSchedule("Hoy ($currentDayRaw) no es un día laborable para tu rol.")
                return
            }

            // Validar hora (Comparación de strings HH:mm:ss funciona bien para rangos)
            if (currentTime < schedule.startTime || currentTime > schedule.endTime) {
                logoutDueToSchedule("Fuate de horario. Tu horario es de ${schedule.startTime} a ${schedule.endTime}")
            }
        } catch (e: Exception) {
            Log.e("MIKE", "Error validando horario", e)
        }
    }

    private fun logoutDueToSchedule(message: String) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
        preference.clearPreference()
        GoToActivityAsNewTask(this, LoginActivity::class.java)
    }

    fun GoToActivityAsNewTask(context: Activity, clazz: Class<*>?) {
        val intent = Intent(context, clazz)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        context.startActivity(intent)
        context.finish()
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)){
            drawerLayout.closeDrawer(GravityCompat.START)
        }else{
            super.onBackPressed()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.fragmentContainerView)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(toggle.onOptionsItemSelected(item)){
            return true
        }
        return super.onOptionsItemSelected(item)
    }

}