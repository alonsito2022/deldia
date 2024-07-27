package com.example.deldia.models

class FirebaseGang() {
    // var users: MutableList<User> = mutableListOf()
    var users: MutableMap<Int, User> = mutableMapOf()
    class User(){
        var userID: Int=0
        var userName: String=""
        var lastPickingDate: String=""
        var clientFullName: String = ""
        var total: Double = 0.0

        // var details: MutableList<Product> = mutableListOf()
        var details: MutableMap<String, Product> = mutableMapOf()
    }
}