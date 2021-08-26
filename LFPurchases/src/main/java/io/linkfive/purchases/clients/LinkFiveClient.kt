package io.linkfive.purchases.clients

import com.android.billingclient.api.Purchase
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.*
import com.github.kittinunf.fuel.core.extensions.authentication
import com.github.kittinunf.fuel.coroutines.awaitStringResponse
import io.linkfive.purchases.exceptions.WrongApiKeyException
import io.linkfive.purchases.models.*
import io.linkfive.purchases.util.LinkFiveLogger
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlin.reflect.KClass


class LinkFiveClient(block: LFClientConfig.() -> Unit) {
    private val productionURL = "https://api.linkfive.io"
    private val stagingUrl = "https://api.staging.linkfive.io"

    class LFClientConfig {
        var linkFiveEnvironment: LinkFiveEnvironment = LinkFiveEnvironment.PRODUCTION
        var host: String = "https://api.linkfive.io"
        var apiKey: String? = null
        var appData: AppData = AppData()
        val platform: String = "GOOGLE"
        var utmSource: String? = null
        var userId: String? = null
        var environment: String? = null
    }

    var config: LFClientConfig = LFClientConfig()
        .apply(block)
        .apply {
            if (this.linkFiveEnvironment == LinkFiveEnvironment.STAGING) {
                this.host = stagingUrl
            } else {
                this.host = productionURL
            }
        }

    // parser to parse Json
    private val parser = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()
    private var subscriptionResponseParser: JsonAdapter<LinkFiveSubscriptionResponse> =
        parser.adapter(LinkFiveSubscriptionResponse::class.java)
    private var verifyPurchaseRequestParser: JsonAdapter<LinkFiveVerifyPurchasesRequest> =
        parser.adapter(LinkFiveVerifyPurchasesRequest::class.java)
    private var verifyPurchaseRespondParser: JsonAdapter<LinkFiveVerifiedPurchasesResponse> =
        parser.adapter(LinkFiveVerifiedPurchasesResponse::class.java)

    private var LinkFiveErrorParser: JsonAdapter<LinkFiveError> = parser.adapter(LinkFiveError::class.java)

    suspend fun getSubscriptionList(): LinkFiveSubscriptionResponseData {
        try {
            LinkFiveLogger.d("Fetch subscription from LinkFive")
            val subscriptionResponse: Triple<Request, Response, String> =
                Fuel.get("${config.host}${HostPaths.GET_SUBSCRIPTIONS.path}")
                    .authentication().bearer("${config.apiKey}")
                    .header(getHeaders())
                    .awaitStringResponse()

            val response = subscriptionResponse.second
            val body = subscriptionResponse.third

            LinkFiveLogger.v("Request Done")
            LinkFiveLogger.v("Response StatusCode: ${response.statusCode}")
            LinkFiveLogger.v("Response Data: ${body}")

            val subscriptionList = subscriptionResponseParser.fromJson(body)
                ?: throw IllegalStateException("LinkFive Success Response Klaxon is not able to parse it: status->${response.statusCode} body->${body}")

            LinkFiveLogger.v("Data Object: ${subscriptionList.toString()}")

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
    suspend fun verifyGooglePurchase(purchases: List<Purchase>): LinkFiveVerifiedPurchases? {
        LinkFiveLogger.d("Receiving purchases: ${purchases.size}")
        return postVerifyPurchaseToServer(purchases)
    }

    private suspend fun postVerifyPurchaseToServer(purchases: List<Purchase>): LinkFiveVerifiedPurchases? {
        LinkFiveLogger.d("Verify Google Purchases")
        try {
            val body = verifyPurchaseRequestParser.toJson(
                LinkFiveVerifyPurchasesRequest(
                    purchases,
                    sameConstructorOverload = true
                )
            )
            LinkFiveLogger.v("Sending data: $body")
            val responseTriple: Triple<Request, Response, String> =
                Fuel.post("${config.host}${HostPaths.VERIFY_PURCHASE.path}")
                    .body(body)
                    .authentication().bearer("${config.apiKey}")
                    .header(getHeaders())
                    .appendHeader("Content-Type" to "application/json")
                    .awaitStringResponse()

            LinkFiveLogger.d("Sending Purchases done, statusCode: ${responseTriple.second.statusCode}")
            val responseString = responseTriple.third

            return verifyPurchaseRespondParser.fromJson(responseString)?.data
        } catch (e: FuelError) {
            LinkFiveLogger.e("Fuel error on sending Purchase to LinkFive")
            LinkFiveLogger.e("Error code: ${e.response.statusCode} data: ${String(e.errorData)}")
        }
        return null
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
            "X-Platform" to config.platform,
            "X-User-Id" to config.userId.let { it ?: "" },
            "X-Utm-Source" to config.utmSource.let { it ?: "" },
            "X-Environment" to config.environment.let { it ?: "" }
        )
    }

    enum class HostPaths(val path: String, val responseClass: KClass<*>) {
        GET_SUBSCRIPTIONS("/api/v1/subscriptions", LinkFiveSubscriptionResponse::class),
        VERIFY_PURCHASE("/api/v1/purchases/google/verify", LinkFiveVerifyPurchasesRequest::class)
    }
}
