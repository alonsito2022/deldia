package com.sys4soft.deldia.models

class ClientRouteResponse {
    var personID: Int = 0
    var name: String = ""
    var fullName: String = ""
    var firstSurname: String = ""
    var secondSurname: String = ""
    var fiscalAddress: String? = null
    var phone: String? = null
    var cellphone: String = ""
    var showcases: Int = 0
    var showcasesDisplay: String = ""
    var customerType: Int = 0
    var customerTypeDisplay: String = ""
    var physicalDistribution: String = ""
    var physicalDistributionDisplay: String = ""
    var physicalDistributionDisplaySaved: String = ""
    var comment: String = ""
    var purchaseVolume: String = ""
    var purchaseVolumeDisplay: String = ""
    var isSupplier: Boolean = false
    var isClient: Boolean = false
    var isEnabled: Boolean = false
    var visitDay: Int = 0
    var visitDayDisplay: String = ""
    var gangID: Int = 0
    var gangName: String = ""
    var observation: String = ""
    var documentType: String = ""
    var documentTypeDisplay: String = ""
    var documentNumber: String = ""
    var address: String = ""
    var latitude: Double = 0.0
    var longitude: Double = 0.0
    var district: String = ""
    var districtDisplay: String = ""
    var message: String = ""
    var error: Boolean = false
    var routeStatus: String = ""
    var routeStatusDisplay: String = ""
    var routeDate: String = ""
    var recentOps: ArrayList<Operation> = ArrayList()
    var totalBalance: Double = 0.0
    var routeDispatchTotalSold: Double = 0.0
}
