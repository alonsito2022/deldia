package com.sys4soft.deldia.localdatabase

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class MySQLiteHelper(context: Context): SQLiteOpenHelper(context, "sales.db", null, 3) {
    override fun onCreate(db: SQLiteDatabase?) {
        val orderCreation = "CREATE TABLE operation (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "client_id INTEGER," +
                "warehouse_id INTEGER," +
                "user_id INTEGER," +
                "document_type TEXT," +

                "operation_date TEXT," +
                "user_full_name TEXT," +
                "client_full_name TEXT," +
                "client_document_type TEXT," +
                "client_document_number TEXT," +
                "client_visit_day_display TEXT," +
                "gang_name TEXT," +
                "total REAL" +
                ")"
        db!!.execSQL(orderCreation)
        val orderDetailCreation = "CREATE TABLE operation_detail (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "operation_id INTEGER," +
                "quantity INTEGER," +
                "price REAL," +
                "product_tariff_id INTEGER NOT NULL," +

                "description TEXT," +
                "subtotal REAL," +
                "foreign key(operation_id) references operation(id)" +
                ")"
        db.execSQL(orderDetailCreation)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        val orderDrop = "DROP TABLE IF EXISTS operation"
        db!!.execSQL(orderDrop)
        val orderDetailDrop = "DROP TABLE IF EXISTS operation_detail"
        db.execSQL(orderDetailDrop)
        onCreate(db)
    }

    fun addOperation(
        clientID: Int, warehouseID: Int, userID: Int, documentType: String,
        operationDate: String, userFullName: String, clientFullName: String, clientDocumentType: String,
        clientDocumentNumber: String, clientVisitDayDisplay: String, gangName: String, total: Double
    ){
        val data = ContentValues()
        data.put("client_id", clientID)
        data.put("warehouse_id", warehouseID)
        data.put("user_id", userID)
        data.put("document_type", documentType)

        data.put("operation_date", operationDate)
        data.put("user_full_name", userFullName)
        data.put("client_full_name", clientFullName)
        data.put("client_document_type", clientDocumentType)
        data.put("client_document_number", clientDocumentNumber)
        data.put("client_visit_day_display", clientVisitDayDisplay)
        data.put("gang_name", gangName)
        data.put("total", total)
        val db = this.writableDatabase
        db.insert("operation", null, data)
        db.close()
    }

    fun lastInsertOperation(): Int{
        val db = this.writableDatabase
        val selectQuery = "SELECT max(id) FROM operation"
        var id: Int = 0
        db.rawQuery(selectQuery, null).use { // .use requires API 16
            if (it.moveToFirst()) {
                id = it.getInt(0)
            }
        }
        return id
    }

    fun addOperationDetail(
        operationID: Int, productTariffID: Int, quantity: Int, price: Double,
        description: String, subtotal: Double
    ){
        val data = ContentValues()
        data.put("operation_id", operationID)
        data.put("product_tariff_id", productTariffID)
        data.put("description", description)
        data.put("quantity", quantity)
        data.put("price", price)
        data.put("subtotal", subtotal)
        val db = this.writableDatabase
        db.insert("operation_detail", null, data)
        db.close()
    }

    fun deleteOperation(operationID: Int){
        val db = this.writableDatabase
        val deleteItemsQuery = "DELETE FROM operation_detail WHERE operation_id='$operationID'"
        db!!.execSQL(deleteItemsQuery)
        val deleteQuery = "DELETE FROM operation WHERE id='$operationID'"
        db.execSQL(deleteQuery)
    }
}