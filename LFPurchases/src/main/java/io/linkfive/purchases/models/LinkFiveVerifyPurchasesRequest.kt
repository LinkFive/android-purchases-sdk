package io.linkfive.purchases.models

import com.android.billingclient.api.Purchase

data class LinkFiveVerifyPurchasesRequest(
    val purchases: List<LinkFiveVerifyPurchasesRequestPurchase>
){
    constructor(purchaseList: List<Purchase>, sameConstructorOverload: Boolean = false): this(
        purchases = purchaseList.map {
            LinkFiveVerifyPurchasesRequestPurchase(it)
        }.toList()
    )
}

data class LinkFiveVerifyPurchasesRequestPurchase(
    val packageName: String,
    val purchaseToken: String,
    val orderId: String,
    val purchaseTime: Long,
    val sku: String
) {

    constructor(purchase: Purchase): this(
        packageName = purchase.packageName,
        purchaseToken = purchase.purchaseToken,
        orderId = purchase.orderId,
        purchaseTime = purchase.purchaseTime,
        sku = purchase.skus.first()
    )
}
