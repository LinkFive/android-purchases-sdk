package io.linkfive.purchases.models

import com.android.billingclient.api.Purchase

data class LinkFivePurchasesRequest(
    val purchases: List<LinkFivePurchasesRequestPurchase>
){
    constructor(purchaseList: List<Purchase>, sameConstructorOverload: Boolean = false): this(
        purchases = purchaseList.map {
            LinkFivePurchasesRequestPurchase(it)
        }.toList()
    )

}

data class LinkFivePurchasesRequestPurchase(
    val packageName: String,
    val purchaseToken: String,
    val orderId: String,
    val purchaseTime: Long,
    val skuList: List<String>
) {

    constructor(purchase: Purchase): this(
        packageName = purchase.packageName,
        purchaseToken = purchase.purchaseToken,
        orderId = purchase.orderId,
        purchaseTime = purchase.purchaseTime,
        skuList = purchase.skus
    )
}
