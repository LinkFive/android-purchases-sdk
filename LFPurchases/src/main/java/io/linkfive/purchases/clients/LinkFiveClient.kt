package io.linkfive.purchases.clients

import com.android.billingclient.api.Purchase
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.*
import com.github.kittinunf.fuel.core.extensions.authentication
import com.github.kittinunf.fuel.coroutines.awaitStringResponse
import io.linkfive.purchases.exceptions.WrongApiKeyException
import io.linkfive.purchases.models.*
import io.linkfive.purchases.util.Logger
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlin.reflect.KClass


class LinkFiveClient(block: LFClientConfig.() -> Unit) {

    class LFClientConfig {
        var host: String = "https://api.staging.linkfive.io"
        var apiKey: String? = null
        var appData: AppData = AppData()
        val platform = "GOOGLE"
        var acknowledgeLocally : Boolean = false
    }

    var config: LFClientConfig = LFClientConfig().apply(block)

    // parser to parse Json
    val parser = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()
    var subscriptionResponseParser: JsonAdapter<LinkFiveSubscriptionResponse> =
        parser.adapter(LinkFiveSubscriptionResponse::class.java)
    var subscriptionDetailResponseParser: JsonAdapter<LinkFiveSubscriptionDetailResponse> =
        parser.adapter(LinkFiveSubscriptionDetailResponse::class.java)
    var purchaseRequestParser: JsonAdapter<LinkFivePurchasesRequest> =
        parser.adapter(LinkFivePurchasesRequest::class.java)

    var LinkFiveErrorParser: JsonAdapter<LinkFiveError> = parser.adapter(LinkFiveError::class.java)

    suspend fun getSubscriptionList(): LinkFiveSubscriptionResponseData {
        try {
            Logger.d("Fetch subscription from LinkFive")
            val subscriptionResponse: Triple<Request, Response, String> =
                Fuel.get("${config.host}${HostPaths.GET_SUBSCRIPTIONS.path}")
                    .authentication().bearer("${config.apiKey}")
                    .header(getHeaders())
                    .awaitStringResponse()

            val response = subscriptionResponse.second
            val body = subscriptionResponse.third

            Logger.v("Request Done")
            Logger.v("Response StatusCode: ${response.statusCode}")
            Logger.v("Response Data: ${body}")

            val subscriptionList = subscriptionResponseParser.fromJson(body)
                ?: throw IllegalStateException("LinkFive Success Response Klaxon is not able to parse it: status->${response.statusCode} body->${body}")

            Logger.v("Data Object: ${subscriptionList.toString()}")

            return subscriptionList.data


        } catch (e: FuelError) {
            throw handleError(
                statusCode = e.response.statusCode,
                errorString = String(e.errorData)
            )
        }
    }

    /**
     * send purchase to server
     * Request sku
     */
    suspend fun onGooglePurchase(purchases: List<Purchase>): LinkFiveSubscriptionDetailResponseData {
        Logger.d("Receiving purchases: ${purchases.size}")
        postPurchaseToServer(purchases)
        return fetchPurchaseDetail(purchases)
    }

    private suspend fun postPurchaseToServer(purchases: List<Purchase>) {
        Logger.d("Posting to server")
        try {
            val body = purchaseRequestParser.toJson(
                LinkFivePurchasesRequest(
                    purchases,
                    sameConstructorOverload = true
                )
            )
            Logger.d("Sending data: $body")
            val response: Triple<Request, Response, String> =
                Fuel.post("${config.host}${HostPaths.POST_PURCHASES.path}")
                    .body(body)
                    .authentication().bearer("${config.apiKey}")
                    .header(getHeaders())
                    .appendHeader("Content-Type" to "application/json")
                    .awaitStringResponse()

            Logger.d("Sending Purchases done, statusCode: ${response.second.statusCode}")
        } catch (e: FuelError) {
            Logger.e("Fuel error on sending Purchase to LinkFive")
            Logger.e("Error code: ${e.response.statusCode} data: ${String(e.errorData)}")
        }
    }

    private suspend fun fetchPurchaseDetail(purchases: List<Purchase>): LinkFiveSubscriptionDetailResponseData {
        try {
            Logger.d("Fetch Purchase Details from LinkFive")
            val url = "${config.host}${HostPaths.GET_SUBSCRIPTION_DETAIL.path}?${
                purchases.joinToString("&") { purchase ->
                    purchase.skus.joinToString("&") {
                        "sku=${it}"
                    }
                }
            }"
            Logger.v("Request to $url")
            val subscriptionResponse: Triple<Request, Response, String> =
                Fuel.get(url)
                    .authentication().bearer("${config.apiKey}")
                    .header(getHeaders())
                    .awaitStringResponse()

            val response = subscriptionResponse.second
            val body = subscriptionResponse.third

            Logger.v("Request Done")
            Logger.v("Response StatusCode: ${response.statusCode}")
            Logger.v("Response Data: ${body}")

            val subscriptionDetailList = subscriptionDetailResponseParser.fromJson(body)
                ?: throw IllegalStateException("LinkFive Success Response Klaxon is not able to parse it: status->${response.statusCode} body->${body}")

            Logger.v("Data Object: $subscriptionDetailList")

            return subscriptionDetailList.data


        } catch (e: FuelError) {
            throw handleError(
                statusCode = e.response.statusCode,
                errorString = String(e.errorData)
            )
        }
    }

    @Throws(WrongApiKeyException::class)
    private fun handleError(statusCode: Int, errorString: String): Exception {
        val errorData = LinkFiveErrorParser.fromJson(errorString)
            ?: return IllegalStateException("LinkFive Error Response is invalid, errorString: $errorString status: $statusCode")
        when (errorData.error) {
            "WRONG_API_KEY" -> return WrongApiKeyException()
        }
        return IllegalStateException("Error not known: ${statusCode} ${errorString}")
    }

    private fun getHeaders(): Map<String, Any> {
        return mapOf<String, Any>(
            "X-App-Version" to config.appData.appVersion,
            "X-Country" to config.appData.country,
            "X-Platform" to config.platform
        )
    }

    enum class HostPaths(val path: String, val responseClass: KClass<*>) {
        GET_SUBSCRIPTIONS("/api/v1/subscriptions", LinkFiveSubscriptionResponse::class),
        GET_SUBSCRIPTION_DETAIL("/api/v1/subscription/sku", LinkFiveSubscriptionDetailResponse::class),
        POST_PURCHASES("/api/v1/purchases/google", LinkFivePurchasesRequest::class)
    }
}
