package com.example.deldia.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.example.deldia.R
import com.example.deldia.adapter.GangAdapter
import com.example.deldia.adapter.UserAdapter
import com.example.deldia.localdatabase.Preference
import com.example.deldia.models.*
import com.example.deldia.retrofit.UserApiService
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IAxisValueFormatter
import com.github.mikephil.charting.formatter.IValueFormatter
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.github.mikephil.charting.utils.ViewPortHandler
import com.google.android.material.textfield.TextInputEditText
import kotlinx.android.synthetic.main.fragment_chart.view.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*


class ChartFragment : Fragment() {

    private var globalContext: Context? = null

    private lateinit var pieChartView: PieChart
    private lateinit var barChartView: LineChart
    private lateinit var horizontalBarChartView: BarChart
    private lateinit var editTextSearchDate: TextInputEditText
    private lateinit var btnSearch: Button
    private lateinit var textViewNumberVisits: TextView
    private lateinit var autoCompleteGang: AutoCompleteTextView
    private lateinit var autoCompleteUser: AutoCompleteTextView

    private var saleChartPie: SaleChartPie = SaleChartPie()
    private var saleOfWeekBarChart: SaleOfWeekBarChart = SaleOfWeekBarChart()
    private var soldProductList = arrayListOf<SoldProduct>()

    private var user: User = User()
    private var productNames = ArrayList<String>()
    private var soldProducts = ArrayList<BarEntry>()
    private var listGangs = arrayListOf<Gang>()
    private var listUsers = arrayListOf<User>()
    private lateinit var preference: Preference
    private lateinit var userAdapter: UserAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        globalContext = this.activity
        preference = Preference(globalContext)

//        val bundle = arguments
//        user.userID= bundle!!.getInt("userID")
        user.userID= preference.getData("userID").toInt()
        user.gang.gangID = preference.getData("gangID").toInt()

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_chart, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        pieChartView = view.findViewById(R.id.pieChartView)
        barChartView = view.findViewById(R.id.barChartView)
        horizontalBarChartView = view.findViewById(R.id.horizontalBarChartView)
        editTextSearchDate = view.findViewById(R.id.editTextSearchDate)
        textViewNumberVisits = view.findViewById(R.id.textViewNumberVisits)
        autoCompleteGang = view.findViewById(R.id.autoCompleteGang)
//        autoCompleteGang.setText(preference.getData("gangName"))
        autoCompleteUser = view.findViewById(R.id.autoCompleteUser)
        loadDistributors()
        val simpleDate = SimpleDateFormat("dd/M/yyyy hh:mm:ss")
        val currentDate = simpleDate.format(Date())

        textViewNumberVisits.text = currentDate

        btnSearch = view.findViewById(R.id.btnSearch)
        btnSearch.setOnClickListener{
            if(user.routeDate.count() > 0){
                if (autoCompleteGang.text.toString().isNotEmpty()){
                    if (autoCompleteUser.text.toString().isNotEmpty()){
                        loadSoldProducts(user)
                        loadSales(user)
                        loadSalesOfWeek(user)
                        autoCompleteGang.setText(user.firstName)

                    }else
                        Toast.makeText(globalContext, "Elija Usuario.", Toast.LENGTH_SHORT).show()
                }else
                    Toast.makeText(globalContext, "Elija Ruta.", Toast.LENGTH_SHORT).show()
            }else
                Toast.makeText(globalContext, "Elija fecha.", Toast.LENGTH_SHORT).show()
        }

        val sdf2 = SimpleDateFormat("dd/MM/yyyy").format(Date())
        val sdf3 = SimpleDateFormat("yyyy-MM-dd").format(Date())
        user.routeDate = sdf3
        user.operationDate = sdf3
        editTextSearchDate.setText(sdf2)
        editTextSearchDate.setOnClickListener { showDatePickerDialog() }

        loadSales(user)
        loadSalesOfWeek(user)
        loadSoldProducts(user)

    }
    private fun loadDistributors(){

        val apiInterface = UserApiService.create().getGangs()
        apiInterface.enqueue(object : Callback<java.util.ArrayList<Gang>> {
            override fun onResponse(call: Call<java.util.ArrayList<Gang>>, response: Response<java.util.ArrayList<Gang>>) {
                listGangs = response.body()!!
                val g: Gang = Gang()
                g.gangID = 0
                g.name = "TODOS"
                listGangs.add(g)
                autoCompleteGang.setAdapter(GangAdapter(globalContext!!, R.layout.item_gang_view, listGangs, object : GangAdapter.OnItemClickListener{
                    override fun onItemClick(model: Gang) {
                        autoCompleteGang.setText(model.name)
                        autoCompleteGang.dismissDropDown()
                        val inputManager = context!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        autoCompleteGang.closeKeyBoard(inputManager)
                        autoCompleteGang.clearFocus()
                        // route.gangID = model.gangID
                        user.gang.gangID = model.gangID
                        loadUsers(model)

                    }
                }))
                Log.d("MIKE", "loadDistributors ok: " + listGangs.size)
            }

            override fun onFailure(call: Call<java.util.ArrayList<Gang>>, t: Throwable) {
                Log.d("MIKE", "loadDistributors onFailure: " + t.message.toString())
            }
        })

    }
    private fun loadUsers(g: Gang){

        val apiInterface = UserApiService.create().getUsersByGang(g)
        apiInterface.enqueue(object : Callback<ArrayList<User>> {
            override fun onResponse(call: Call<ArrayList<User>>, response: Response<ArrayList<User>>) {
                listUsers = response.body()!!
                val u: User = User()
                u.userID = 0
                u.fullName = "TODOS"
                listUsers.add(u)
                userAdapter = UserAdapter(globalContext!!, R.layout.item_user_view, listUsers, object : UserAdapter.OnItemClickListener{
                    override fun onItemClick(model: User) {
                        autoCompleteUser.setText(model.fullName)
                        autoCompleteUser.dismissDropDown()
                        val inputManager = context!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        autoCompleteUser.closeKeyBoard(inputManager)
                        autoCompleteUser.clearFocus()
                        user.userID = model.userID
                    }
                })
                autoCompleteUser.setAdapter(userAdapter)
                Log.d("MIKE", "loadUsers ok: " + listUsers.size)
            }

            override fun onFailure(call: Call<ArrayList<User>>, t: Throwable) {
                Log.d("MIKE", "loadUsersByGang onFailure: " + t.message.toString())
            }
        })

    }
    private fun View.closeKeyBoard(inputMethodManager: InputMethodManager) {
        inputMethodManager.hideSoftInputFromWindow(windowToken, 0)
    }

    private fun showDatePickerDialog(){
        val fm: FragmentManager = (activity as AppCompatActivity?)!!.supportFragmentManager
        val datePicker = DatePickerFragment {day, month, year -> onDateSelected(day, month, year) }
        datePicker.show(fm, "datePicker")
    }

    @SuppressLint("SimpleDateFormat")
    private fun onDateSelected(day:Int, month:Int, year:Int){
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.YEAR, year)
        calendar.set(Calendar.MONTH, month)
        calendar.set(Calendar.DAY_OF_MONTH, day)
        val sdf2 = SimpleDateFormat("dd/MM/yyyy").format(calendar.time)
        val sdf3 = SimpleDateFormat("yyyy-MM-dd").format(calendar.time)
        user.routeDate = sdf3
        user.operationDate = sdf3
        editTextSearchDate.setText(sdf2)
    }

    private fun setVerticalBarChartValues(){
//        val dataSets: ArrayList<IBarDataSet> = ArrayList()
//        dataSets.add(depenses as IBarDataSet)
//        val Data = BarData(dataSets)
//
        val xValues = ArrayList<String>()
        xValues.add("LUNES")
        xValues.add("MARTES")
        xValues.add("MIERCOLES")
        xValues.add("JUEVES")
        xValues.add("VIERNES")
        xValues.add("SABADO")
        xValues.add("DOMINGO")

        val xAXis = barChartView.xAxis
        xAXis.valueFormatter = IndexAxisValueFormatter(xValues)
        xAXis.axisMinimum = 0f
        xAXis.setCenterAxisLabels(true)
        xAXis.position = XAxis.XAxisPosition.BOTTOM
        if(isDarkMode(globalContext!!))
            xAXis.textColor = ContextCompat.getColor(globalContext!!, R.color.purple_200)
        else
            xAXis.textColor = ContextCompat.getColor(globalContext!!, R.color.black)

        xAXis.textSize = 14f
        xAXis.labelRotationAngle = -90f
        xAXis.setDrawGridLines(false)
        xAXis.setDrawAxisLine(false)

        val salesCompleted = ArrayList<Entry>()
        salesCompleted.add(BarEntry(1.0f, saleOfWeekBarChart.monday) )
        salesCompleted.add(BarEntry(2.0f, saleOfWeekBarChart.tuesday) )
        salesCompleted.add(BarEntry(3.0f, saleOfWeekBarChart.wednesday) )
        salesCompleted.add(BarEntry(4.0f, saleOfWeekBarChart.thursday) )
        salesCompleted.add(BarEntry(5.0f, saleOfWeekBarChart.friday) )
        salesCompleted.add(BarEntry(6.0f, saleOfWeekBarChart.saturday) )
        salesCompleted.add(BarEntry(7.0f, saleOfWeekBarChart.sunday) )

        val set2 = LineDataSet(salesCompleted, "TOTAL DE VENTA")
        set2.color = ContextCompat.getColor(globalContext!!, R.color.green_dark)

        if(isDarkMode(globalContext!!))
            set2.valueTextColor = ContextCompat.getColor(globalContext!!, R.color.purple_200)
        else
            set2.valueTextColor = ContextCompat.getColor(globalContext!!, R.color.black)

        set2.valueTextSize = 18f

        val data = LineData(set2)
        barChartView.data = data
        barChartView.setVisibleXRangeMaximum(7f)
        barChartView.axisLeft.valueFormatter = MyYAxisValueFormatter()
//        barChartView.axisLeft.axisMinimum = -10f
        barChartView.extraTopOffset = 10f
        barChartView.extraBottomOffset = -50f
//        barChartView.setFitBars(true)
        barChartView.isDragEnabled = true
        barChartView.animateXY(3000, 3000)
        barChartView.setNoDataText("")
        barChartView.description.isEnabled = false
        barChartView.invalidate()


    }

    private fun setHorizontalBarChartValues(){
        val xValues = ArrayList<String>()
        val productCompleted = ArrayList<BarEntry>()


        var i = 0.5f
        soldProductList.forEach { item ->
//            productNames.add( item.productName.substring(0, 10) + "" + item.productName.substring(10, item.productName.length))
//            soldProducts.add(BarEntry(i, item.quantitySold) )
//            xValues.add(item.productName.substring(0, 10))
            xValues.add(item.productName.substring(0, 10) + "\n" + item.productName.substring(10, item.productName.length))
            productCompleted.add(BarEntry(i, item.quantitySold) )
            i+=1
        }

        val xAXis = horizontalBarChartView.xAxis
        xAXis.valueFormatter = IndexAxisValueFormatter(xValues)
//        xAXis.valueFormatter = IAxisValueFormatter { value, axis -> value.toInt().toString() }
        xAXis.axisMinimum = -0.5f
        xAXis.axisMaximum = 120f
        xAXis.setCenterAxisLabels(true)
        xAXis.position = XAxis.XAxisPosition.BOTTOM

        if(isDarkMode(globalContext!!))
            xAXis.textColor = ContextCompat.getColor(globalContext!!, R.color.purple_200)
        else
            xAXis.textColor = ContextCompat.getColor(globalContext!!, R.color.black)

        xAXis.textSize = 14f
        xAXis.labelRotationAngle = -90f

//        xAXis.mAxisMinimum = 0f
//        xAXis.mAxisMaximum = 120f

        val set2 = BarDataSet(productCompleted, "TOTAL DE PRODUCTOS")
        set2.setColors(ColorTemplate.MATERIAL_COLORS,250)
//        set2.setValueFormatter(MyYAxisValueFormatter())

        if(isDarkMode(globalContext!!))
            set2.valueTextColor = ContextCompat.getColor(globalContext!!, R.color.purple_200)
        else
            set2.valueTextColor = ContextCompat.getColor(globalContext!!, R.color.black)

        set2.valueTextSize = 18f

        val data = BarData(set2)
        data.setValueFormatter(IntegerFormatter())

//        data.barWidth = 5f
        horizontalBarChartView.data = data
        horizontalBarChartView.setVisibleXRangeMaximum(8f)
        horizontalBarChartView.axisLeft.valueFormatter = MyYAxisValueFormatter()
        horizontalBarChartView.axisLeft.axisMinimum = -10f
        horizontalBarChartView.extraTopOffset = 10f
        horizontalBarChartView.extraBottomOffset = -120f
//        horizontalBarChartView.setFitBars(false)
        horizontalBarChartView.isDragEnabled = true
        horizontalBarChartView.animateXY(3000, 3000)
        horizontalBarChartView.setNoDataText("")
        horizontalBarChartView.description.isEnabled = false
        horizontalBarChartView.invalidate()
//        val xAXis = horizontalBarChartView.xAxis
//        xAXis.valueFormatter = IndexAxisValueFormatter(productNames)
//        xAXis.axisMinimum = -0.5f
//        xAXis.setCenterAxisLabels(true)
//        xAXis.position = XAxis.XAxisPosition.TOP_INSIDE
//        xAXis.textColor = Color.BLACK
//        xAXis.mLabelWidth = 150
//        xAXis.textSize = 14f
//
//        val yaxis = horizontalBarChartView.axisLeft
//        yaxis.axisMinimum = -20.5f
//        yaxis.axisMaximum = 180f
//        yaxis.textColor = Color.LTGRAY
//
//        val set2 = BarDataSet(soldProducts, "PRODUCTOS VENDIDOS")
//        set2.setColors(ColorTemplate.MATERIAL_COLORS,250)
//        set2.valueTextColor = Color.BLACK
//        set2.valueTextSize = 20f
//        val legend = horizontalBarChartView.legend
//        legend.textColor = Color.BLACK
//        legend.textSize = 12f
//
//        val data = BarData(set2)
//        data.barWidth = 0.5f
//        horizontalBarChartView.data = data
//        horizontalBarChartView.setDrawValueAboveBar(false)
//        horizontalBarChartView.setFitBars(true)
//        horizontalBarChartView.animateXY(3000, 3000)
//        horizontalBarChartView.description.isEnabled = false
//        horizontalBarChartView.invalidate()

    }

    private fun setPieChartValues(){

        val pieChartEntry = ArrayList<PieEntry>()
        val colors = ArrayList<Int>()

        val total = (saleChartPie.allThumbtacks).toDouble()

        if(saleChartPie.grayThumbtacks>0){
            val e = (saleChartPie.grayThumbtacks * 100) / total
            val percentE:Double = Math.round(e * 100.0) / 100.0
            pieChartEntry.add(PieEntry(saleChartPie.grayThumbtacks, "DADOS DE BAJA") )
            colors.add(Color.rgb(211, 211, 211))
        }

        if(saleChartPie.blueThumbtacks>0){
            val a = (saleChartPie.blueThumbtacks * 100) / total
            val percentA:Double = Math.round(a * 100.0) / 100.0
            pieChartEntry.add(PieEntry(saleChartPie.blueThumbtacks, "PENDIENTES") )
            colors.add(Color.rgb(9,105,218))
        }

        if(saleChartPie.greenThumbtacks>0){
            val c = (saleChartPie.greenThumbtacks * 100) / total
            val percentC:Double = Math.round(c * 100.0) / 100.0
            pieChartEntry.add(PieEntry(saleChartPie.greenThumbtacks, "AUTOVENTA - VENDIDOS") )
            colors.add(Color.rgb(0, 160, 0))
        }

        if(saleChartPie.redThumbtacks>0){
            val b = (saleChartPie.redThumbtacks * 100) / total
            val percentB:Double = Math.round(b * 100.0) / 100.0
//            pieChartEntry.add(PieEntry(saleChartPie.redThumbtacks, "SIN COMPRA ($percentB %)") )
            pieChartEntry.add(PieEntry(saleChartPie.redThumbtacks, "SIN COMPRA") )
            colors.add(Color.rgb(211, 47, 47))
        }


        if(saleChartPie.yellowThumbtacks>0){
            val d = (saleChartPie.yellowThumbtacks * 100) / total
            val percentD:Double = Math.round(d * 100.0) / 100.0
            pieChartEntry.add(PieEntry(saleChartPie.yellowThumbtacks, "PREVENTAS - PENDIENTES") )
            colors.add(Color.rgb(251, 203, 10))
        }

        if(saleChartPie.skyBlueThumbtacks>0){
            val f = (saleChartPie.skyBlueThumbtacks * 100) / total
            val percentF:Double = Math.round(f * 100.0) / 100.0
            pieChartEntry.add(PieEntry(saleChartPie.skyBlueThumbtacks, "PREVENTAS - ENVIADO") )
            colors.add(Color.rgb(154, 208, 236))
        }

        if(saleChartPie.darkGreenThumbtacks>0){
            val g = (saleChartPie.darkGreenThumbtacks * 100) / total
            val percentG:Double = Math.round(g * 100.0) / 100.0
            pieChartEntry.add(PieEntry(saleChartPie.darkGreenThumbtacks, "PREVENTAS - ENTREGADO CONFORME") )
            colors.add(Color.rgb(18, 91, 80))
        }

        if(saleChartPie.fuchsiaThumbtacks>0){
            val h = (saleChartPie.fuchsiaThumbtacks * 100) / total
            val percentH:Double = Math.round(h * 100.0) / 100.0
            pieChartEntry.add(PieEntry(saleChartPie.fuchsiaThumbtacks, "PREVENTAS - ENVIO FALLIDO") )
            colors.add(Color.rgb(240, 18, 190))
        }


        val set = PieDataSet(pieChartEntry, "TOTAL DE CLIENTES: $total")
        set.sliceSpace = 0.25f
        set.colors = colors
        set.xValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
        set.yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
//        set.valueLinePart1OffsetPercentage = 100f
//        set.valueLinePart1Length = 0f
//        set.valueLinePart2Length = 0.5f

        val data = PieData(set)

        if(isDarkMode(globalContext!!))
            data.setValueTextColor(ContextCompat.getColor(globalContext!!, R.color.purple_200))
        else
            data.setValueTextColor(ContextCompat.getColor(globalContext!!, R.color.black))


        data.setValueTextSize(25f)
        data.setValueFormatter(IntegerFormatter())

        val legend = pieChartView.legend

        if(isDarkMode(globalContext!!))
            legend.textColor = ContextCompat.getColor(globalContext!!, R.color.purple_200)
        else
            legend.textColor = ContextCompat.getColor(globalContext!!, R.color.black)

        legend.textSize = 14f
        legend.orientation = Legend.LegendOrientation.VERTICAL
        legend.horizontalAlignment = Legend.LegendHorizontalAlignment.LEFT
        legend.verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
        legend.isWordWrapEnabled = true

        pieChartView.data = data
        pieChartView.holeRadius = 58f
        pieChartView.setHoleColor(Color.TRANSPARENT)
        pieChartView.setDrawEntryLabels(false)
        pieChartView.isDrawHoleEnabled = true
        pieChartView.transparentCircleRadius = 61f
        pieChartView.setTransparentCircleAlpha(110)
        pieChartView.rotationAngle = 0f

        pieChartView.setNoDataText("SIN DATOS")
        pieChartView.description.isEnabled = false
        pieChartView.setExtraOffsets(0f, 10f, 20f, 20f)
        pieChartView.animateY(3000)
        pieChartView.invalidate()


    }

    private fun loadSales(u: User){

        val apiInterface = UserApiService.create().getActualSalesForCharPie(u)
        apiInterface.enqueue(object : Callback<SaleChartPie> {
            override fun onResponse(call: Call<SaleChartPie>, response: Response<SaleChartPie>) {
                if (response.body() != null){
                    saleChartPie = response.body()!!
//                    pieChartView.clear()
//                    pieChartView.data?.clearValues()
//                    pieChartView.invalidate()
                    setPieChartValues()
                }
            }
            override fun onFailure(call: Call<SaleChartPie>, t: Throwable) {
                Log.d("MIKE", "loadSales onFailure: " + t.message.toString())
            }
        })

    }

    private fun loadSalesOfWeek(u: User){

        val apiInterface = UserApiService.create().getActualSalesOfWeekForBarChar(u)
        apiInterface.enqueue(object : Callback<SaleOfWeekBarChart> {
            override fun onResponse(call: Call<SaleOfWeekBarChart>, response: Response<SaleOfWeekBarChart>) {
                if (response.body() != null){
                    saleOfWeekBarChart = response.body()!!
                    setVerticalBarChartValues()
                }
            }
            override fun onFailure(call: Call<SaleOfWeekBarChart>, t: Throwable) {
                Log.d("MIKE", "loadSales onFailure: " + t.message.toString())
            }
        })

    }

    private fun loadSoldProducts(u: User){
        soldProducts.clear()

        val apiInterface = UserApiService.create().getActualSoldProductForBarChar(u)
        apiInterface.enqueue(object : Callback<ArrayList<SoldProduct>> {
            override fun onResponse(call: Call<ArrayList<SoldProduct>>, response: Response<ArrayList<SoldProduct>>) {
                if (response.body() != null){
                    soldProductList = response.body()!!

//                    var i = 0.5f
//
//                    soldProductList.forEach { item ->
//                        productNames.add( item.productName.substring(0, 10) + "" + item.productName.substring(10, item.productName.length))
//                        soldProducts.add(BarEntry(i, item.quantitySold) )
//                        i+=1
//                    }
                    setHorizontalBarChartValues()
                }
            }
            override fun onFailure(call: Call<ArrayList<SoldProduct>>, t: Throwable) {
                Log.d("MIKE", "loadSales onFailure: " + t.message.toString())
            }
        })
    }

    fun isDarkMode(context: Context): Boolean {
        val darkModeFlag = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return darkModeFlag == Configuration.UI_MODE_NIGHT_YES
    }

    class MyYAxisValueFormatter : IAxisValueFormatter {
        private val mFormat: DecimalFormat

        init {
            mFormat = DecimalFormat("###,###,##0")
        }

        override fun getFormattedValue(value: Float, axis: AxisBase?): String {
            return mFormat.format(value.toDouble())
        }
    }

    class IntegerFormatter : IValueFormatter  {
        private val mFormat: DecimalFormat
        fun getBarLabel(barEntry: BarEntry): String {
            return mFormat.format(barEntry.y.toDouble())
        }

        init {
            mFormat = DecimalFormat("###,##0")
        }

        override fun getFormattedValue(
            value: Float,
            entry: Entry?,
            dataSetIndex: Int,
            viewPortHandler: ViewPortHandler?
        ): String {
            return mFormat.format(value)
        }
    }

}