package com.sys4soft.deldia.retrofit
import com.sys4soft.deldia.models.*
import io.reactivex.Observable
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit


interface UserApiService {
    @POST("hrm/auth/login/")
    @Headers("Accept: application/json", "Content-type:application/json")
    fun getAuth(@Body params: User) : Call<ResponseApi>

    @GET("hrm/api/get_user/")
    @Headers("Accept: application/json", "Content-type:application/json")
    fun getLogin(@Header("Authorization") token : String) : Call<ResponseLogin>

    @POST("dispatches/api/v1/get_user_by_id/")
    @Headers("Accept: application/json", "Content-type:application/json")
    fun getUser(@Body params: User) : Call<User>

    @GET("dispatches/api/v1/get_gangs/")
    @Headers("Accept: application/json", "Content-type:application/json")
    fun getGangs() : Call<ArrayList<Gang>>

    @POST("sales/api/v1/get_product_stock_by_warehouse/")
    @Headers("Accept: application/json", "Content-type:application/json")
    fun getStockInWarehouse(@Body params: Warehouse) : Observable<ArrayList<Product>>

    @POST("sales/api/v1/get_product_stock_by_sale_and_warehouse/")
    @Headers("Accept: application/json", "Content-type:application/json")
    fun getStockBySaleAndWarehouse(@Body params: Operation) : Observable<ArrayList<Product>>

    @POST("dispatches/api/v1/get_sales_in_warehouse/")
    @Headers("Accept: application/json", "Content-type:application/json")
    fun getSalesInWarehouse(@Body params: Operation) : Call<ArrayList<Operation>>

    @POST("dispatches/api/v1/get_sales_by_client_id/")
    @Headers("Accept: application/json", "Content-type:application/json")
    fun getSalesByClientID(@Body params: Operation) : Call<ArrayList<Operation>>

    @POST("dispatches/api/v1/get_order_history_by_client_id/")
    @Headers("Accept: application/json", "Content-type:application/json")
    fun getOrderHistoryByClientID(@Body params: Operation) : Call<ArrayList<Operation>>

    @POST("sales/api/v1/register_inventory_check/")
    @Headers("Accept: application/json", "Content-type:application/json")
    fun sendInventoryCheck(@Body params: Operation) : Call<Operation>

    @POST("dispatches/api/v1/get_sale_by_id/")
    @Headers("Accept: application/json", "Content-type:application/json")
    fun getSaleByID(@Body params: Operation) : Call<Operation>

    @POST("sales/api/v1/get_person_by_id/")
    @Headers("Accept: application/json", "Content-type:application/json")
    fun getPersonByID(@Body params: Person) : Call<Person>

    @POST("sales/api/v1/document_consultation/")
    @Headers("Accept: application/json", "Content-type:application/json")
    fun getDocumentConsultation(@Body params: Person) : Call<Person>

    @POST("dispatches/api/v1/register_client/")
    @Headers("Accept: application/json", "Content-type:application/json")
    fun sendClientData(@Body params: Person) : Call<Person>

    @POST("dispatches/api/v1/update_client/")
    @Headers("Accept: application/json", "Content-type:application/json")
    fun sendUpdateClientData(@Body params: Person) : Call<Person>

    @POST("dispatches/api/v1/register_client_address/")
    @Headers("Accept: application/json", "Content-type:application/json")
    fun sendClientAddressData(@Body params: Person) : Call<Person>

    @POST("dispatches/api/v1/register_quotation/")
    @Headers("Accept: application/json", "Content-type:application/json")
    fun sendQuotationData(@Body params: Operation) : Call<Operation>

    @POST("dispatches/api/v1/update_quotation/")
    @Headers("Accept: application/json", "Content-type:application/json")
    fun sendUpdateQuotationData(@Body params: Operation) : Call<Operation>

    @POST("dispatches/api/v1/devolution_of_delivery/")
    @Headers("Accept: application/json", "Content-type:application/json")
    fun sendDevolutionDeliveryData(@Body params: Operation) : Call<Operation>

    @POST("dispatches/api/v1/terminate_quotation/")
    @Headers("Accept: application/json", "Content-type:application/json")
    fun sendTerminateQuotationData(@Body params: Operation) : Call<Operation>

    @POST("dispatches/api/v1/register_dispatch/")
    @Headers("Accept: application/json", "Content-type:application/json")
    fun sendDispatchData(@Body params: Operation) : Call<Operation>

    @POST("dispatches/api/v1/filter_client_by_action/")
    @Headers("Accept: application/json", "Content-type:application/json")
    fun getFilterClient(@Body params: RequestFilterPerson): Call<ResponseFilterPerson> // Changed return type

    @POST("dispatches/api/v1/get_daily_route/")
    @Headers("Accept: application/json", "Content-type:application/json")
    fun getDailyRoute(@Body params: User) : Observable<ArrayList<Person>>

    @POST("dispatches/api/v1/get_route/")
    @Headers("Accept: application/json", "Content-type:application/json")
    fun getRoute(@Body params: Route) : Observable<ArrayList<Person>>

    @POST("dispatches/api/v1/get_cashes/")
    @Headers("Accept: application/json", "Content-type:application/json")
    fun getCashes(@Body params: User) : Call<ArrayList<Cash>>

    @POST("dispatches/api/v1/get_cash_flow/")
    @Headers("Accept: application/json", "Content-type:application/json")
    fun getCashFlow(@Body params: CashFlow) : Call<ArrayList<CashFlow>>

    @POST("dispatches/api/v1/register_cash_flow/")
    @Headers("Accept: application/json", "Content-type:application/json")
    fun saveCashFlow(@Body params: CashFlow) : Call<ArrayList<CashFlow>>

    @POST("dispatches/api/v1/register_visit_without_dispatch/")
    @Headers("Accept: application/json", "Content-type:application/json")
    fun saveVisitWithoutDispatch(@Body params: Person) : Call<ResponseApi>

    @POST("dispatches/api/v1/presale_delivered/")
    @Headers("Accept: application/json", "Content-type:application/json")
    fun savePresaleDelivered(@Body params: Operation) : Call<Operation>

    @POST("dispatches/api/v1/presale_no_delivered/")
    @Headers("Accept: application/json", "Content-type:application/json")
    fun savePresaleNoDelivered(@Body params: Operation) : Call<Operation>

    @POST("dispatches/api/v1/cancel_presale/")
    @Headers("Accept: application/json", "Content-type:application/json")
    fun saveCancelPresale(@Body params: Operation) : Call<Operation>

    @POST("dispatches/api/v1/decline_sale_from_the_app/")
    @Headers("Accept: application/json", "Content-type:application/json")
    fun saveDeclinedSale(@Body params: Person) : Call<ResponseApi>

    @POST("dispatches/api/v1/cancel_sale/")
    @Headers("Accept: application/json", "Content-type:application/json")
    fun cancelDispatchData(@Body params: Operation) : Call<Operation>

    @POST("dispatches/api/v1/put_in_pending/")
    @Headers("Accept: application/json", "Content-type:application/json")
    fun putInPending(@Body params: Operation) : Call<Operation>

    @POST("dispatches/api/v1/get_actual_sales_for_chart_pie/")
    @Headers("Accept: application/json", "Content-type:application/json")
    fun getActualSalesForCharPie(@Body params: User) : Call<SaleChartPie>

    @POST("dispatches/api/v1/get_actual_sales_of_week_for_bar_char/")
    @Headers("Accept: application/json", "Content-type:application/json")
    fun getActualSalesOfWeekForBarChar(@Body params: User) : Call<SaleOfWeekBarChart>

    @POST("dispatches/api/v1/get_actual_sold_products_for_bar_char/")
    @Headers("Accept: application/json", "Content-type:application/json")
    fun getActualSoldProductForBarChar(@Body params: User) : Call<ArrayList<SoldProduct>>

    @GET("dispatches/api/v1/get_api_gps_odometer/")
    @Headers("Accept: application/json", "Content-type:application/json")
    fun getApiGPS() : Call<ArrayList<LocationGps>>

    @POST("sales/api/v1/get_users_by_gang/")
    @Headers("Accept: application/json", "Content-type:application/json")
    fun getUsersByGang(@Body params: Gang) : Call<ArrayList<User>>

    @GET("accounting/api/v1/all_sellers/")
    @Headers("Accept: application/json", "Content-type:application/json")
    fun getAllSellers() : Call<ArrayList<User>>

    companion object {
//        var BASE_URL = "http://192.168.1.20:9017/"
        var BASE_URL = "https://www.deldiadistribuciones.nom.pe/"
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        fun create(): UserApiService {
            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build()

            return retrofit.create(UserApiService::class.java);
        }
    }
}
