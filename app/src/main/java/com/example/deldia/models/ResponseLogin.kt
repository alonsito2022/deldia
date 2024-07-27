package com.example.deldia.models

class ResponseLogin {
    var access: Boolean=false
    var timezone: String=""
    var user_id: Int=0
    var first_name: String=""
    var email: String=""
    var document: String=""
    var is_superuser: Boolean=false
    var is_staff: Boolean=false
    var lastLogin: String=""
    var groups: MutableList<Group> = mutableListOf()
    var subsidiary: Subsidiary=Subsidiary()
    var vehicle: Vehicle=Vehicle()
    class Group(){
        var group_id:Int=0
        var name:String=""
    }
    class Subsidiary(){
        var subsidiary_id:Int=0
        var name:String=""
        var businessName:String=""
    }
    class Vehicle(){
        var vehicleID:Int=0
        var gangID:Int=0
        var vehicleLicensePlate:String=""
        var gangName:String=""
        var gangSerial:String=""
    }
}