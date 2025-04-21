package com.example.deldia.models

class Operation() {
    var operationID: Int = 0
    var clientID: Int = 0
    var userID: Int = 0
    var warehouseID: Int = 0

    var gangID: Int = 0
    var gangName: String = ""

    var correlative: Int = 0
    var percentageIgv: Double = 0.0
    var conditionIgv: String = "NA"

    var operationStatus: String = "01" // '01':'PENDING', '02':'COMPLETE', '03': 'ANNULLED'
    var operationType: String = "09" // '02': 'SALE', '09': 'ORDER OF SALE'
    var operationDate: String = ""
    var operationStartDate: String = ""
    var operationEndDate: String = ""
    var operationCustomDate: String = ""
    var operationAction: String = "NA" // 'NA': 'NO APPLY', 'E': 'ENTRANCE'
    var observation: String = ""

    var documentType: String = "05" // '01': 'TICKET', '05': 'QUOTATION'
    var physicalDistribution: String = "01" // '01': 'EN TIRAS', '02': 'SUELTAS'
    var physicalDistributionDisplay: String = ""
    var documentNumber: String = ""

    var clientFullName: String = ""
    var clientDocumentType: String = ""
    var clientDocumentNumber: String = ""
    var clientAddress: String = ""
    var clientVisitDayDisplay: String = ""
    var clientObservation: String = ""
    var clientCellphone: String = ""
    var clientGangName: String = ""
    var userFullName: String = ""
    var operationTime: String = ""
    var total: Double = 0.0
    var totalPurchased: Double = 0.0
    var totalPaid: Double = 0.0
    var totalPending: Double = 0.0
    var totalReturned: Double = 0.0
    var numberToCurrency: String = ""
    var documentTypeDisplay: String = ""
    var routeDate: String = ""
    var routeObservation: String = ""
//    var routeDispatchTotalSold: Double = 0.0
    var routeStatus: String = ""
    var messageStatus: String = ""
    var pseSent: Boolean = false

    var paymentMethods: MutableMap<String, Double> = mutableMapOf()
    var details: MutableList<OperationDetail> = mutableListOf()

    class OperationDetail(
        var productTariffID: Int,
        var price: Double,
        var quantity: Int
    ){

        constructor() : this(0, 0.0, 0)

        var productID: Int = 0
        var productSaleName: String = ""
        var subtotal: Double = 0.0

        var stockChecked: Double = 0.0
        var enlisted: Boolean = false
    }
}