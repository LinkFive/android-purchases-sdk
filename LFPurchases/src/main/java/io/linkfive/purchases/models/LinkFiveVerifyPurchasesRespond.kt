package io.linkfive.purchases.models

data class LinkFiveVerifiedPurchasesResponse(
    val data: LinkFiveVerifiedPurchases
)

data class LinkFiveVerifiedPurchases(
    val purchases: List<LinkFiveVerifiedReceipt>
)

data class LinkFiveVerifiedReceipt(
    val sku: String,
    var purchaseId: String? = null,
    val transactionDate: String,
    var validUntilDate: String? = null,
    var isTrial: Boolean? = null,
    val isExpired: Boolean,
    var familyName: String? = null,
    var attributes: String? = null,
    var period: String? = null
)
