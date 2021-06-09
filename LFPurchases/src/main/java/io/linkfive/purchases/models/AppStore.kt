package io.linkfive.purchases.models

import android.util.Base64
import androidx.lifecycle.MutableLiveData
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.SkuDetails
import io.linkfive.purchases.util.Logger

data class LinkFiveStore(
    var rawLastResponse: LinkFiveSubscriptionResponseData? = null,
    var rawSkuDetailList: List<SkuDetails>? = null,
    var lastSubscriptionList: LinkFiveSubscriptionData? = null,

    // LinkFive Subscription Response data
    val linkFiveSubscriptionResponseLiveData: MutableLiveData<LinkFiveSubscriptionResponseData> = MutableLiveData(),
    // LinkFive Attributes combined with Google Billing Subscription Data
    val linkFiveSubscriptionLiveData: MutableLiveData<LinkFiveSubscriptionData> = MutableLiveData(),

    val linkFiveActiveSubscriptionLiveData: MutableLiveData<LinkFiveActiveSubscriptionData> = MutableLiveData(),

) {

    fun onNewSubscriptionData(data: LinkFiveSubscriptionResponseData) {
        rawLastResponse = data
        linkFiveSubscriptionResponseLiveData.postValue(data)
    }

    /**
     * New Subscription Playout received
     */
    fun onNewSubscriptionSkuDetails(
        skuList: List<SkuDetails>?
    ) {
        if (skuList == null || skuList.isEmpty()) {
            Logger.d("No Sku Details found")
            linkFiveSubscriptionLiveData.postValue(
                LinkFiveSubscriptionData(
                    error = "NO_SKU_FOUND"
                )
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

        Logger.v("Push Subscription to LiveData")
        linkFiveSubscriptionLiveData.postValue(lastSubscriptionList)
    }

    /**
     * On New Active Subscription
     */
    fun onNewActivePurchases(
        googlePurchases: List<Purchase>,
        linkFiveSubscriptionList: LinkFiveSubscriptionDetailResponseData
    ) {
        val activePurchases = googlePurchases.map { purchase ->
            LinkFivePurchaseDetail(
                purchase = purchase,
                subscriptionList = linkFiveSubscriptionList.subscriptionList
            )
        }.toList()

        Logger.v("found active Purchases: ${activePurchases.count()} $activePurchases")

        // Posting to liveData
        linkFiveActiveSubscriptionLiveData.postValue(LinkFiveActiveSubscriptionData(activePurchases))

    }

    fun getLinkFiveSubscriptionList(): List<LinkFiveSubscriptionResponseDataSubscription> {
        return rawLastResponse?.subscriptionList ?: emptyList()
    }

}