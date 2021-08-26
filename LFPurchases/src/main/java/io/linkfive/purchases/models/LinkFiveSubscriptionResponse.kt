package io.linkfive.purchases.models

data class LinkFiveSubscriptionResponse(
    val data: LinkFiveSubscriptionResponseData
)

data class LinkFiveSubscriptionResponseData(
    val platform: String,
    val attributes: String?,
    val subscriptionList: List<LinkFiveSubscriptionResponseDataSubscription>
)

data class LinkFiveSubscriptionResponseDataSubscription(
    val sku: String
)

