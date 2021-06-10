package io.linkfive.purchases

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import io.linkfive.purchases.clients.GoogleBillingClient
import io.linkfive.purchases.clients.LinkFiveClient
import io.linkfive.purchases.models.*
import io.linkfive.purchases.util.Logger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.asStateFlow
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
     * LinkFive Api Response Kotlin Flow.
     *
     * Example usage would be:
     *
     * ```
     * LinkFivePurchases.linkFiveSubscriptionResponseFlow().collect {
     *   Log.d("LinkFive Data", "data: $it")
     * }
     * ```
     */
    fun linkFiveSubscriptionResponseFlow() =
        linkFiveStore.linkFiveSubscriptionResponseFlow.asStateFlow()

    /**
     * Google Billing plus LinkFive Subscription Attribute data Live Data
     */
    fun linkFiveSubscriptionLiveData() = linkFiveStore.linkFiveSubscriptionLiveData

    /**
     * Google Billing plus LinkFive Subscription Attribute data Kotlin Flow
     *
     * Example usage would be:
     *
     * ```
     * LinkFivePurchases.linkFiveSubscriptionFlow().collect {
     *   Log.d("LinkFive Data", "data: $it")
     * }
     * ```
     */
    fun linkFiveSubscriptionFlow() = linkFiveStore.linkFiveSubscriptionFlow.asStateFlow()

    /**
     * Active Purchases plus LinkFive data as Live Data
     */
    fun linkFiveActivePurchasesLiveData() = linkFiveStore.linkFiveActiveSubscriptionLiveData

    /**
     * Active Purchases plus LinkFive data as Kotlin Flow
     *
     * Example usage would be:
     * ```
     * LinkFivePurchases.linkFiveActivePurchasesFlow().collect {
     *   Log.d("LinkFive Data", "data: $it")
     * }
     * ```
     */
    fun linkFiveActivePurchasesFlow() = linkFiveStore.linkFiveActiveSubscriptionFlow.asStateFlow()

    /**
     * Initialize the client
     * @param acknowledgeLocally if set to true, the subscription will be acknowledged by
     * the sdk instead of the server
     */
    fun init(apiKey: String, context: Context, acknowledgeLocally: Boolean = false) {
        Logger.d(apiKey)
        if (apiKey.isBlank()) {
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