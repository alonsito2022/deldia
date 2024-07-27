package com.example.deldia

import android.app.*
import android.content.Intent
import android.os.Build
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
import androidx.navigation.ui.setupWithNavController
import com.example.deldia.localdatabase.Preference
import com.example.deldia.models.User
import com.example.deldia.retrofit.UserApiService
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import kotlinx.android.synthetic.main.activity_main.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {

    lateinit var toggle: ActionBarDrawerToggle
    lateinit var toolbar: Toolbar
    lateinit var drawerLayout: DrawerLayout
    lateinit var navView: NavigationView
    lateinit var bottomNavigationView: BottomNavigationView
    lateinit var navController: NavController
    lateinit var appBarConfiguration: AppBarConfiguration

    private lateinit var preference: Preference

    private lateinit var alarmManager: AlarmManager
    private lateinit var pendingIntent: PendingIntent
    private lateinit var calendar: Calendar

    private var user: User = User()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

//        oscar version
//        supportActionBar?.hide();//Ocultar ActivityBar anterior
        toolbar = findViewById(R.id.include2) as Toolbar
        setSupportActionBar(toolbar)
//        oscar version

        preference = Preference(applicationContext)

        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)
        bottomNavigationView = findViewById(R.id.bottomNavigationView)

        navController = findNavController(R.id.fragmentContainerView)
        appBarConfiguration = AppBarConfiguration(navController.graph,drawerLayout)
        // the title in the action bar will automatically be updated when the destination changes
        setupActionBarWithNavController(navController, drawer_layout)
        navView.setupWithNavController(navController)

        toggle = ActionBarDrawerToggle(this, drawerLayout, R.string.open, R.string.close)
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

//        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val bundle = Bundle()
        bundle.putInt("userID", preference.getData("userID").toInt())
        bundle.putInt("vehicleID", preference.getData("vehicleID").toInt())
        bundle.putString("vehicleLicensePlate", preference.getData("vehicleLicensePlate"))

        bottomNavigationView.setOnItemSelectedListener {
            it.isChecked = true
//            navView.menu.findItem(it.itemId).isChecked = true
            when(it.itemId){
                R.id.nav_products -> navController.navigate(R.id.productFragment, bundle)
                R.id.nav_routes -> navController.navigate(R.id.routeFragment, bundle)
                R.id.nav_map -> navController.navigate(R.id.mapFragment, bundle)
                R.id.nav_sales_realized -> navController.navigate(R.id.saleRealizedFragment, bundle)
            }
            true
        }


        navView.setNavigationItemSelectedListener {

            it.isChecked = true
            drawerLayout.closeDrawers()

            when(it.itemId){
                R.id.clientFragment -> navController.navigate(R.id.clientFragment, bundle)
                R.id.productFragment -> navController.navigate(R.id.productFragment, bundle)
                R.id.routeFragment -> navController.navigate(R.id.routeFragment, bundle)
                R.id.mapFragment -> navController.navigate(R.id.mapFragment, bundle)
                R.id.saleRealizedFragment -> navController.navigate(R.id.saleRealizedFragment, bundle)
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

        val goToMap = intent.getBooleanExtra("GO_TO_MAP", false)
        if (goToMap) { navController.navigate(R.id.mapFragment, bundle)}

        val goToOrders = intent.getBooleanExtra("GO_TO_ORDERS", false)
        if (goToOrders) { navController.navigate(R.id.saleRealizedFragment, bundle)}

        loadUser(preference.getData("userID").toInt())


//        createNotificationChannel()

//        setAlarm()




    }

    private fun loadUser(id: Int) = if (id > 0){
        val u = User()
        u.userID=id
        val apiInterface = UserApiService.create().getUser(u)
        apiInterface.enqueue(object : Callback<User> {
            override fun onResponse(call: Call<User>, response: Response<User>) {

                if (response.body() != null) {
                    user = response.body()!!



                    val gangSaved = preference.getData("gangID").toInt()
                    val gangToday = user.gang.gangID
                    val sessionTokenSaved = preference.getData("token")
                    val sessionTokenToday = SimpleDateFormat("yyyy-MM-dd").format(Date()).toString()

                    Log.d("MIKE", "sessionTokenSaved: $sessionTokenSaved")
                    Log.d("MIKE", "sessionTokenToday: $sessionTokenToday")
                    Log.d("MIKE", "gangSaved: $gangSaved")
                    Log.d("MIKE", "gangToday: $gangToday")

                    if(preference.getData("isStaff").isNullOrBlank() && preference.getData("token").isNullOrBlank()){
                        preference.clearPreference()
                        GoToActivityAsNewTask(this@MainActivity, LoginActivity::class.java)
                    }else{

                        if(sessionTokenSaved != sessionTokenToday){
                            Toast.makeText(applicationContext, "Session finalizada. Guardada: ${sessionTokenSaved}. Today: ${sessionTokenToday}", Toast.LENGTH_SHORT).show()
                            preference.clearPreference()
                            GoToActivityAsNewTask(this@MainActivity, LoginActivity::class.java)
                        }else if(gangSaved != gangToday || gangToday == 0){
                            if (gangToday == 0)
                                Toast.makeText(applicationContext, "Sin cuadrilla. Guardada: ${gangSaved}. Today: ${gangToday}", Toast.LENGTH_SHORT).show()
                            else
                                Toast.makeText(applicationContext, "Verificar cuadrilla. Guardada: ${gangSaved}. Today: ${gangToday}", Toast.LENGTH_SHORT).show()
                            preference.clearPreference()
                            GoToActivityAsNewTask(this@MainActivity, LoginActivity::class.java)
                        }else if(preference.getData("isStaff") == "false")
                            checkWorkTime()

                    }
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

    private fun createNotificationChannel(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            val name : CharSequence = "foxyReminderChannel"
            val description = "Channel for alarm"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("foxy", name, importance)
            channel.description = description
            val notificationManager = getSystemService(
                NotificationManager::class.java
            )

        }
    }

    private fun setAlarm(){

        calendar = Calendar.getInstance()
//        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm")
//        val currentDate = sdf.format(Date())
        // calendar.time = sdf.parse(currentDate)
        calendar.set(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH),
            20,
            58,
            0
        )


        alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlarmReceiver::class.java)

        val pendingFlags: Int = if (Build.VERSION.SDK_INT >= 23)
            (PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        else
            PendingIntent.FLAG_UPDATE_CURRENT
        pendingIntent = PendingIntent.getBroadcast(this, 0, intent, pendingFlags)
        alarmManager.setRepeating(
            AlarmManager.RTC, calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY, pendingIntent
        )

        Toast.makeText(this, "alarm set", Toast.LENGTH_SHORT).show()
    }

    private fun checkWorkTime(){
        val c = Calendar.getInstance()
        val hour = c.get(Calendar.HOUR_OF_DAY)
        if(hour in 7..18) {
            Toast.makeText(applicationContext, "BIENVENIDO ${preference.getData("userName").uppercase()}", Toast.LENGTH_LONG).show()
        }
        else {
            Toast.makeText(applicationContext,"Acceso denegado. El horario de ingreso es de 7am a 6pm", Toast.LENGTH_LONG).show()
            preference.clearPreference()
            GoToActivityAsNewTask(this, LoginActivity::class.java)
        }
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