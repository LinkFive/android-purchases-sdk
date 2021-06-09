package io.linkfive.purchases.models

data class LinkFiveSubscriptionDetailResponse(
    val data: LinkFiveSubscriptionDetailResponseData
)

data class LinkFiveSubscriptionDetailResponseData(
    val subscriptionList: List<LinkFiveSubscriptionDetailResponseDataSubscription>,
)

data class LinkFiveSubscriptionDetailResponseDataSubscription(
    val sku: String,
    val familyName: String?,
    val attributes: String?,
)

