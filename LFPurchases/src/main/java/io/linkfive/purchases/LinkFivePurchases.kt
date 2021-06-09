package io.linkfive.purchases

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import io.linkfive.purchases.clients.GoogleBillingClient
import io.linkfive.purchases.clients.LinkFiveClient
import io.linkfive.purchases.models.*
import io.linkfive.purchases.util.Logger
import kotlinx.coroutines.*
import java.lang.IllegalStateException

object LinkFivePurchases {

    private lateinit var client: LinkFiveClient
    private val linkFiveStore = LinkFiveStore()
    private lateinit var billingClient: GoogleBillingClient

    /**
     * LinkFive Api Response Live Data
     */
    fun linkFiveSubscriptionResponseLiveData() = linkFiveStore.linkFiveSubscriptionResponseLiveData

    /**
     * Google Billing plus LinkFive Subscription Attribute data
     */
    fun linkFiveSubscriptionLiveData() = linkFiveStore.linkFiveSubscriptionLiveData

    /**
     * Active Purchases plus LinkFive data
     */
    fun linkFiveActivePurchasesLiveData() = linkFiveStore.linkFiveActiveSubscriptionLiveData

    /**
     * Initialize the client
     * @param acknowledgeLocally if set to true, the subscription will be acknowledged by
     * the sdk instead of the server
     */
    fun init(apiKey: String, context: Context, acknowledgeLocally: Boolean = false) {
        Logger.d(apiKey)
        if(apiKey.isBlank()){
            throw IllegalStateException("Init LinkFive with no api Key")
        }
        Logger.d("Got apiKey: ${apiKey.substring(0, 5)}..")
        client = LinkFiveClient {
            this.apiKey = apiKey
            this.appData = AppData(context)
            this.acknowledgeLocally = acknowledgeLocally
        }
    }

    /**
     * Initialize Google Billing Client and fetches all Subscriptions
     */
    fun fetch(context: Context) {
        GlobalScope.launch {
            fetchSubscriptions()
            billingClient = GoogleBillingClient(
                context = context,
                linkFiveStore = linkFiveStore,
                linkFiveClient = client
            )
        }
    }

    fun purchase(skuDetails: SkuDetails, activity: Activity) {
        billingClient.purchase(skuDetails, activity)
    }

    private suspend fun fetchSubscriptions() {
        val subscriptionList = client.getSubscriptionList()
        linkFiveStore.onNewSubscriptionData(subscriptionList)
    }
}