package io.linkfive.purchases.models

import com.android.billingclient.api.Purchase

data class LinkFiveActiveSubscriptionData(
    val linkFiveSkuData: List<LinkFivePurchaseDetail>? = null,
)

data class LinkFivePurchaseDetail(
    val purchase: Purchase,
    var familyName: String? = null,
    var attributes: String? = null,
) {
    constructor(
        purchase: Purchase,
        subscriptionList: List<LinkFiveSubscriptionDetailResponseDataSubscription>
    ) : this(
        purchase = purchase
    ){
        val subscriptionDetail = subscriptionList.firstOrNull { sub ->
            purchase.skus.find { sku ->
                sku == sub.sku
            } != null
        }
        subscriptionDetail?.let {
            familyName = it.familyName
            attributes = it.attributes
        }
    }
}