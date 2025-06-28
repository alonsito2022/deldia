package com.sys4soft.deldia.models

class Person() {

    var personID: Int = 0
    var name: String = ""
    var fullName: String = ""
    var firstSurname: String = ""
    var secondSurname: String = ""
    var fiscalAddress: String = ""
    var phone: String = ""
    var cellphone: String = ""
    var isSupplier: Boolean = false
    var isClient: Boolean = true
    var isEnabled: Boolean = true
    var visitDay: Int = 0
    var visitDayDisplay: String = ""
    var gangID: Int = 0
    var showcases: Int = 0
    var customerType: Int = 0
    var physicalDistribution: String = "NA"
    var physicalDistributionDisplay: String = ""
    var physicalDistributionDisplaySaved: String = ""
    var showcasesDisplay: String = ""
    var customerTypeDisplay: String = ""
    var gangName: String = ""
    var observation: String = ""
    var comment: String = ""
    var purchaseVolume: String = ""
    var purchaseVolumeDisplay: String = ""
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

    var totalCharged: Double = 0.0
    var totalDischarged: Double = 0.0
    var totalBalance: Double = 0.0
    var routeStatus: String = ""
    var routeObservation: String = ""
    var routeStatusDisplay: String = ""
    var routeDate: String = ""
    var routeDispatchID: Int = 0
    var routeDispatchTotalSold: Double = 0.0

    var userID: Int = 0
}
