package io.linkfive.purchases.models

import android.util.Base64
import androidx.lifecycle.MutableLiveData
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.SkuDetails
import io.linkfive.purchases.util.LinkFiveLogger
import kotlinx.coroutines.flow.MutableStateFlow

data class LinkFiveStore(
    var rawLastResponse: LinkFiveSubscriptionResponseData? = null,
    var rawSkuDetailList: List<SkuDetails>? = null,
    var lastSubscriptionList: LinkFiveSubscriptionData? = null,

    /**
     * Android Live Data and Kotlin Flow
     */
    // LinkFive Subscription Response data
    val linkFiveSubscriptionResponseLiveData: MutableLiveData<LinkFiveSubscriptionResponseData> = MutableLiveData(),
    val linkFiveSubscriptionResponseFlow: MutableStateFlow<LinkFiveSubscriptionResponseData?> = MutableStateFlow(null),

    // LinkFive Attributes combined with Google Billing Subscription Data
    val linkFiveSubscriptionLiveData: MutableLiveData<LinkFiveSubscriptionData> = MutableLiveData(),
    val linkFiveSubscriptionFlow: MutableStateFlow<LinkFiveSubscriptionData?> = MutableStateFlow(null),

    // Active Subscriptions enhanced with LinkFive data
    val linkFiveActiveSubscriptionLiveData: MutableLiveData<LinkFiveVerifiedPurchases> = MutableLiveData(),
    val linkFiveActiveSubscriptionFlow: MutableStateFlow<LinkFiveVerifiedPurchases?> = MutableStateFlow(null),


) {

    fun onNewSubscriptionData(data: LinkFiveSubscriptionResponseData) {
        rawLastResponse = data
        linkFiveSubscriptionResponseLiveData.postValue(data)
        linkFiveSubscriptionResponseFlow.value = data
    }

    /**
     * New Subscription Playout received
     */
    fun onNewSubscriptionSkuDetails(
        skuList: List<SkuDetails>?
    ) {
        if (skuList == null || skuList.isEmpty()) {
            LinkFiveLogger.d("No Sku Details found")
            linkFiveSubscriptionLiveData.postValue(
                LinkFiveSubscriptionData(
                    error = "NO_SKU_FOUND"
                )
            )
            linkFiveSubscriptionFlow.value = LinkFiveSubscriptionData(
                error = "NO_SKU_FOUND"
            )
            return
        }
        parseSkuDetails(skuList)
    }

    private fun parseSkuDetails(
        googleSkuList: List<SkuDetails>
    ) {
        rawSkuDetailList = googleSkuList

        // prepare subscription data
        lastSubscriptionList =
            LinkFiveSubscriptionData(
                linkFiveSkuData = googleSkuList.map {
                    LinkFiveSkuData(
                        skuDetails = it,
                    )
                },
                attributes = rawLastResponse?.attributes?.let {
                    Base64.encodeToString(
                        it.toByteArray(),
                        Base64.DEFAULT
                    )
                }
            )

        LinkFiveLogger.v("Push Subscription to LiveData")
        linkFiveSubscriptionLiveData.postValue(lastSubscriptionList)
        linkFiveSubscriptionFlow.value = lastSubscriptionList
    }

    /**
     * On New Active Subscription
     */
    fun onNewActivePurchases(
        linkFiveVerifiedPurchases: LinkFiveVerifiedPurchases?
    ) {
        if(linkFiveVerifiedPurchases == null){
            linkFiveActiveSubscriptionLiveData.postValue(LinkFiveVerifiedPurchases(purchases = emptyList()))
            return
        }

        LinkFiveLogger.v("found active Purchases: ${linkFiveVerifiedPurchases.purchases.count()} $linkFiveVerifiedPurchases")

        // Posting to liveData and kotlin Flow
        linkFiveActiveSubscriptionLiveData.postValue(linkFiveVerifiedPurchases)
        linkFiveActiveSubscriptionFlow.value = linkFiveVerifiedPurchases
    }

    fun getLinkFiveSubscriptionList(): List<LinkFiveSubscriptionResponseDataSubscription> {
        return rawLastResponse?.subscriptionList ?: emptyList()
    }

}