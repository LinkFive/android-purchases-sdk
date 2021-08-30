package io.linkfive.purchases

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import io.linkfive.purchases.models.*
import io.linkfive.purchases.util.LinkFiveLogLevel
import io.linkfive.purchases.util.LinkFiveLogger
import io.tnx.keller_app.BuildConfig
import io.tnx.keller_app.R
import io.tnx.keller_app.databinding.ActivityMainBinding
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private lateinit var globalScopeResponseFlowJob: Job
    private lateinit var globalScopeSubFlowJob: Job
    private lateinit var globalScopeActiveFlowJob: Job

    private val subscriptionDataObserver = Observer<LinkFiveSubscriptionData> { data: LinkFiveSubscriptionData ->
        // Update the UI, in this case, a TextView.
        LinkFiveLogger.d("Playout data, yay", data.toString())
        buildSubscription(data)
    }
    private val activeSubscriptionDataObserver = Observer<LinkFiveVerifiedPurchases> { data: LinkFiveVerifiedPurchases ->
        // Update the UI, in this case, a TextView.
        LinkFiveLogger.d("Active Subscription, yay", data.toString())
        handleActiveSubscription(data)
    }

    private val linkFiveLoggerObserver = Observer<String> { data ->
        handleLog(data)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        initContent()
        initButtons()
        initSubscriptionsLiveData()
        initSubscriptionsFlow()
    }

    fun initContent() {
        binding.version.text = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
    }

    fun initButtons() {
        binding.refreshBtn.setOnClickListener {
            LinkFivePurchases.fetchSubscriptions(this)
        }
        binding.sendBtn.setOnClickListener {
            val i = Intent(Intent.ACTION_SENDTO)
            i.data = Uri.parse("mailto:")
            i.putExtra(Intent.EXTRA_EMAIL, arrayOf("a@linkfive.io"))
            i.putExtra(Intent.EXTRA_SUBJECT, "App Log")
            i.putExtra(Intent.EXTRA_TEXT, binding.log.text)
            try {
                startActivity(Intent.createChooser(i, "Send mail..."))
            } catch (ex: ActivityNotFoundException) {
                Toast.makeText(
                    this,
                    "There are no email clients installed.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    fun initSubscriptionsLiveData() {
        LinkFivePurchases.init(
            apiKey = BuildConfig.LINKFIVE_API_KEY,
            logLevel = LinkFiveLogLevel.TRACE,
            linkFiveEnvironment = LinkFiveEnvironment.STAGING,
            context = this
        )
        LinkFivePurchases.fetchSubscriptions(context = this)
        LinkFiveLogger.logLiveData.observe(this, linkFiveLoggerObserver)
        LinkFivePurchases.linkFiveSubscriptionLiveData().observe(this, subscriptionDataObserver)
        LinkFivePurchases.linkFiveActivePurchasesLiveData()
            .observe(this, activeSubscriptionDataObserver)
    }

    fun initSubscriptionsFlow() {
        LinkFiveLogger.d("On Create Flow $this")
        globalScopeResponseFlowJob = GlobalScope.launch {
            LinkFivePurchases.linkFiveSubscriptionResponseFlow().collect {
                LinkFiveLogger.d("FLOW: got data Response: $it")
            }
        }
        globalScopeSubFlowJob = GlobalScope.launch {
            LinkFivePurchases.linkFiveSubscriptionFlow().collect {
                LinkFiveLogger.d("FLOW: got data Subscription: $it")
            }
        }
        globalScopeActiveFlowJob = GlobalScope.launch {
            LinkFivePurchases.linkFiveActivePurchasesFlow().collect {
                LinkFiveLogger.d("FLOW: got data Active: $it")
            }
        }
    }

    fun buildSubscription(data: LinkFiveSubscriptionData) {
        binding.subLayout.removeAllViews()
        val inflater =
            applicationContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        if (data.linkFiveSkuData.isNullOrEmpty()) {
            val textView = TextView(this)
            textView.setText("No Subscription Found")
            binding.subLayout.addView(textView)
        } else {
            data.linkFiveSkuData?.forEach { skuDetail: LinkFiveSkuData ->

                val subView: View = inflater.inflate(R.layout.item_purchase, null)
                val titleView = subView.findViewById<TextView>(R.id.title)
                val priceView = subView.findViewById<TextView>(R.id.price_title)

                titleView.text = skuDetail.skuDetails.subscriptionPeriod.let {
                    when (it) {
                        "P1M" -> "Monthly"
                        "P3M" -> "Quarterly"
                        "P6M" -> "Half Yearly"
                        "P1Y" -> "Yearly"
                        else -> "unknown Duration"
                    }
                }
                priceView.text = skuDetail.let {
                    "${it.skuDetails.price} / ${
                        when (it.skuDetails.subscriptionPeriod) {
                            "P1M" -> "1 Month"
                            "P3M" -> "3 Months"
                            "P6M" -> "6 Months"
                            "P1Y" -> "1 Year"
                            else -> "unknown Duration"
                        }
                    }"
                }

                subView.setOnClickListener {
                    LinkFivePurchases.purchase(skuDetail.skuDetails.sku, this)
                }

                binding.subLayout.addView(subView)
            }
        }
    }

    fun handleActiveSubscription(data: LinkFiveVerifiedPurchases) {

        binding.activeSubscription.text =
            data.purchases.mapIndexed { index, linkFivePurchaseDetail ->
                "$index: ${linkFivePurchaseDetail.purchaseId} \n" +
                        "  family: ${linkFivePurchaseDetail.familyName} \n" +
                        "  attri butes: ${linkFivePurchaseDetail.attributes} \n" +
                        "  skus: ${linkFivePurchaseDetail.sku}"
            }.joinToString("\n") ?: "got no active subscription"
    }

    fun handleLog(s: String) {
        binding.log.append("$s \n")
    }

    override fun onDestroy() {
        super.onDestroy()
        LinkFiveLogger.d("On Destroy global scope $this")
        //globalScopeResponseFlowJob.cancel()
        //globalScopeSubFlowJob.cancel()
        //globalScopeActiveFlowJob.cancel()
    }
}