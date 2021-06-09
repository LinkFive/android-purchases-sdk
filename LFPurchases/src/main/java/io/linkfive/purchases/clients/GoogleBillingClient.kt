package io.linkfive.purchases.clients

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import io.linkfive.purchases.models.LinkFiveStore
import io.linkfive.purchases.util.Logger
import kotlinx.coroutines.*

class GoogleBillingClient(
    context: Context,
    val linkFiveStore: LinkFiveStore,
    val linkFiveClient: LinkFiveClient
) :
    PurchasesResponseListener {

    private val purchasesUpdatedListener =
        PurchasesUpdatedListener { billingResult, purchases ->
            Logger.d("Billing update: ", billingResult, purchases)
            onPurchasesUpdated(billingResult, purchases)
        }

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases()
        .build()

    /**
     * Starts the billing client connection and queries all SKUs
     */
    init {
        Logger.v("Start Connection to Google Billing")
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    // The BillingClient is ready. You can query purchases here.
                    Logger.v("Google Billing Connected")

                    GlobalScope.launch {
                        querySkuDetails()
                        fetchExistingPurchases()
                    }
                } else {
                    Logger.d("Google connection Response with ${billingResult.responseCode} ${billingResult.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
                Logger.v("Google Billing Disconnected")
            }
        })
    }

    suspend fun querySkuDetails() {
        val skuList = ArrayList<String>()
        linkFiveStore.getLinkFiveSubscriptionList().forEach {
            skuList.add(it.sku)
        }
        Logger.d("test")
        val params = SkuDetailsParams.newBuilder()
        params.setSkusList(skuList).setType(BillingClient.SkuType.SUBS)

        // leverage querySkuDetails Kotlin extension function
        val skuDetailsResult = withContext(Dispatchers.IO) {
            billingClient.querySkuDetails(params.build())
        }

        Logger.d("message: ${skuDetailsResult.billingResult.debugMessage} code: ${skuDetailsResult.billingResult.responseCode}")

        val skuDetailsList = skuDetailsResult.skuDetailsList
        Logger.d("Google SkuDetailList: $skuDetailsList")

        if (skuDetailsList.isNullOrEmpty()) {
            Logger.d("No subscriptions found. intended?")
            return
        }

        linkFiveStore.onNewSubscriptionSkuDetails(
            skuList = skuDetailsList
        )
    }

    fun purchase(skuDetails: SkuDetails, activity: Activity) {
        // Retrieve a value for "skuDetails" by calling querySkuDetailsAsync().
        Logger.v("Launch Billing Flow with SKU: ", skuDetails)
        val flowParams = BillingFlowParams.newBuilder()
            .setSkuDetails(skuDetails)
            .build()
        val responseCode = billingClient.launchBillingFlow(activity, flowParams).responseCode
        Logger.v("Billing ResponseCode: $responseCode")
    }

    private fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: List<Purchase>?
    ) {
        runBlocking {
            onPurchase(billingResult, purchases = purchases)
        }
    }

    /**
     * Fetch all existing purchases
     */
    fun fetchExistingPurchases() {
        Logger.v("Billing Fetch Purchases Async")
        billingClient.queryPurchasesAsync(BillingClient.SkuType.SUBS, this)
    }

    /**
     * gets called from queryPurchasesAsync when fetchExistingPurchases
     */
    override fun onQueryPurchasesResponse(
        billingResult: BillingResult,
        purchases: List<Purchase>
    ) {
        runBlocking {
            onPurchase(billingResult = billingResult, purchases = purchases)
        }
    }

    private suspend fun onPurchase(billingResult: BillingResult, purchases: List<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && !purchases.isNullOrEmpty()) {
            handlePurchase(purchases)
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            Logger.d("User Canceled")
            // Handle an error caused by a user cancelling the purchase flow.
        } else {
            Logger.d("Other error code: ${billingResult.responseCode} ${billingResult.debugMessage}. no purchase found")
            // Handle any other error codes.
        }
    }

    private suspend fun handlePurchase(googlePurchases: List<Purchase>) {
        Logger.d("User purchased orderID: ${googlePurchases.map { it.orderId }} in $googlePurchases")

        if (linkFiveClient.config.acknowledgeLocally) {
            googlePurchases.forEach { purchase ->
                if (purchase.isAcknowledged.not()) {
                    Logger.d("Purchase not Acknowledged, will consume now")
                    val consumeParams =
                        AcknowledgePurchaseParams.newBuilder()
                            .setPurchaseToken(purchase.purchaseToken)
                            .build()
                    val consumeResult = withContext(Dispatchers.IO) {
                        billingClient.acknowledgePurchase(consumeParams)
                    }

                    Logger.d(
                        "Purchase consumed. " +
                                "code: ${consumeResult.responseCode} " +
                                "message: ${consumeResult.debugMessage}"
                    )
                }
            }
        } else {
            Logger.v("Acknowledge locally turned off")
        }

        val linkFiveSubscriptionList = linkFiveClient.onGooglePurchase(googlePurchases)
        linkFiveStore.onNewActivePurchases(googlePurchases, linkFiveSubscriptionList)
    }
}