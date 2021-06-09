package io.linkfive.purchases.models

import com.android.billingclient.api.SkuDetails

data class LinkFiveSubscriptionData(
    val linkFiveSkuData: List<LinkFiveSkuData>? = null,
    val attributes: String? = null,
    val error: String? = null
)

data class LinkFiveSkuData(
    val skuDetails: SkuDetails,
)