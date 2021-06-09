package io.linkfive.purchases

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import io.linkfive.purchases.models.LinkFiveActiveSubscriptionData
import io.linkfive.purchases.models.LinkFiveSkuData
import io.linkfive.purchases.models.LinkFiveSubscriptionData
import io.linkfive.purchases.util.Logger
import io.tnx.keller_app.BuildConfig
import io.tnx.keller_app.R
import io.tnx.keller_app.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val subscriptionDataObserver = Observer<LinkFiveSubscriptionData> { data ->
        // Update the UI, in this case, a TextView.
        Logger.d("Playout data, yay", data.toString())
        buildSubscription(data)
    }
    private val activeSubscriptionDataObserver = Observer<LinkFiveActiveSubscriptionData> { data ->
        // Update the UI, in this case, a TextView.
        Logger.d("Active Subscription, yay", data.toString())
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
        initSubscriptions()
    }

    fun initContent(){
        binding.version.text = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
    }

    fun initButtons() {
        binding.refreshBtn.setOnClickListener {
            LinkFivePurchases.fetch(this)
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

    fun initSubscriptions() {
        LinkFivePurchases.init(
            apiKey = BuildConfig.LINKFIVE_API_KEY,
            context = this
        )
        LinkFivePurchases.fetch(context = this)
        Logger.logLiveData.observe(this, linkFiveLoggerObserver)
        LinkFivePurchases.linkFiveSubscriptionLiveData().observe(this, subscriptionDataObserver)
        LinkFivePurchases.linkFiveActivePurchasesLiveData()
            .observe(this, activeSubscriptionDataObserver)
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
                    Log.d("qwe", "qwe")
                    LinkFivePurchases.purchase(skuDetail.skuDetails, this)
                }

                binding.subLayout.addView(subView)
            }
        }
    }

    fun handleActiveSubscription(data: LinkFiveActiveSubscriptionData) {

        binding.activeSubscription.text =
            data.linkFiveSkuData?.mapIndexed { index, linkFivePurchaseDetail ->
                "$index: ${linkFivePurchaseDetail.purchase.orderId} \n" +
                        "  family: ${linkFivePurchaseDetail.familyName} \n" +
                        "  attri butes: ${linkFivePurchaseDetail.attributes} \n" +
                        "  skus: ${linkFivePurchaseDetail.purchase.skus.joinToString(",")}"
            }?.joinToString("\n") ?: "got no active subscription"
    }

    fun handleLog(s: String) {
        binding.log.append("$s \n")
    }
}