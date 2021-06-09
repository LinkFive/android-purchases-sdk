package io.linkfive.purchases.models

data class LinkFiveSubscriptionResponse(
    val data: LinkFiveSubscriptionResponseData
)

data class LinkFiveSubscriptionResponseData(
    val platform: String,
    val subscriptionList: List<LinkFiveSubscriptionResponseDataSubscription>,
    val attributes: String?
)

data class LinkFiveSubscriptionResponseDataSubscription(
    val sku: String
)

