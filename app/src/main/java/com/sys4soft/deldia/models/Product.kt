package com.sys4soft.deldia.models

class Product() {
    var firebaseID: Int? = null
    var productID: Int? = null
    var productPurchaseName: String = ""
    var productShortName: String = ""
    var productSize: String = ""
    var productSaleName: String = ""
    var productSku: String = ""
    var productPath: String = ""
    var productCode: String = ""
    var productBrandID: Int = 0
    var quantity: Int = 0
    var positionInAdapter: Int = 0
    var productBrandName: String = ""
    var stock: Double = 0.0
    var stockInitial: Double = 0.0
    var stockStarted: Double = 0.0
    var stockSupplied: Double = 0.0
    var stockTotalDelivered: Double = 0.0
    var stockResiduary: Double = 0.0
    var stockReadjustedInput: Double = 0.0
    var stockReadjustedOutput: Double = 0.0
    var stockChecked: Double = 0.0
    var stockSold: Double = 0.0
    var priceSale: Double = 0.0
    var subtotal: Double = 0.0
    var batchSalePrice: Double = 0.0
    var batchCode: String = ""
    var batchStock: Double = 0.0
    var unitName: String = ""
    var productTariffID: Int = 0
    var remainingQuantityInTariffs: ArrayList<ProductTariff> = arrayListOf()

    var showImage: Boolean = true

    class ProductTariff(){
        var productTariffID: Int? = null
        var unitID: Int? = null
        var unitName: String = ""
        var priceSale: Double = 0.0
        var pricePurchase: Double = 0.0
        var percentageDiscount: Double = 0.0
        var quantityMinimum: Double = 0.0
        var stock: Double = 0.0
    }
}