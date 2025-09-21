package com.sys4soft.deldia.models

class SaleBySeller() {
    var operationID: Int = 0
    var operationType: String = ""
    var operationStatus: String = ""
    var registerTime: String = ""
    var registerDate: String = ""
    var receiptSerial: String = ""
    var receiptNumber: String = ""
    var userID: Int = 0
    var userName: String = ""
    var clientID: Int = 0
    var clientGangName: String = ""
    var clientObservation: String = ""
    var clientVisitDay: Int = 0
    var clientVisitDayDisplay: String = ""
    var clientName: String = ""
    var clientPhysicalDistribution: String = ""
    var clientPhysicalDistributionDisplay: String = ""
    var clientDocumentType: String = ""
    var clientAddress: String = ""
    var totalPurchased: Double = 0.0
    var totalPaid: Double = 0.0
    var totalPaidInCash: Double = 0.0
    var totalPaidInYape: Double = 0.0
    var totalInCredit: Double = 0.0
    var totalPending: Double = 0.0
    var cashFlowSet: ArrayList<CashFlowItem> = arrayListOf()
    var dailyRouteStatus: String = ""
    var dailyRouteDisplayStatus: String = ""
    var totalReturned: Double = 0.0

    class CashFlowItem() {
        var id: Int = 0
        var transactionDate: String = ""
        var typeDisplay: String = ""
        var type: String = ""
        var total: Double = 0.0
    }
}
