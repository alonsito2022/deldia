package com.sys4soft.deldia.models

class User(){
    var userID: Int=0
    var fullName: String=""
    var firstName: String=""
    var lastName: String=""
    var email: String=""
    var document: String=""
    var password: String=""
    var searchDate: String=""
    var routeDate: String=""
    var operationDate: String=""
    var phone: String=""
    var isSuperuser: Boolean = false
    var isStaff: Boolean = false
    var isPresale: Boolean = true
    var lastLogin: String=""
    var groups: MutableList<Group> = mutableListOf()
    var subsidiary: Subsidiary = Subsidiary()
    var gang: Gang = Gang()

    class Group(){
        var groupID: Int = 0
        var name: String = ""
    }

    class Gang(){
        var gangID: Int = 0
        var gangName: String = ""
        var gangSerial: String = ""
        var warehouseID: Int = 0
        var warehouseName: String = ""
    }

    class Subsidiary(){
        var subsidiaryID: Int = 0
        var name: String = ""
        var businessName: String = ""
    }
}
