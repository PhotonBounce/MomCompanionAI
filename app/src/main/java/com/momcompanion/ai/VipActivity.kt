package com.friendai

import android.app.Activity
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams

class VipActivity : Activity() {
    private lateinit var billingClient: BillingClient
    private lateinit var monetization: MonetizationManager
    private val monthlySku = "vip_monthly"
    private val yearlySku = "vip_yearly"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val isTablet = ResponsiveLayout.isTablet(this)
        val hPad = if (isTablet) 40 else 24

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(hPad), dp(36), dp(hPad), dp(32))
        }

        val title = TextView(this).apply {
            text = "VIP Membership"
            textSize = if (isTablet) 34f else 30f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(UiStyle.ACCENT)
        }

        val desc = TextView(this).apply {
            text = "Unlock unlimited AI replies, extra stories, and priority support.\nAll safety features always remain free."
            textSize = if (isTablet) 20f else 18f
            gravity = Gravity.CENTER
            setTextColor(UiStyle.TEXT_PRI)
            setLineSpacing(0f, 1.35f)
            setPadding(0, dp(16), 0, dp(20))
        }

        val subscribeBtn = Button(this).apply {
            text = "Subscribe Monthly — \$1.99 / month"
            textSize = if (isTablet) 22f else 20f
            setAllCaps(false)
            UiStyle.styleBtn(this, UiStyle.ACCENT)
            setOnClickListener { launchBillingFlow(monthlySku) }
        }

        val yearlyBtn = Button(this).apply {
            text = "Subscribe Yearly — \$4.99 / year"
            textSize = if (isTablet) 22f else 20f
            setAllCaps(false)
            UiStyle.styleBtn(this, UiStyle.VIP_GOLD)
            setOnClickListener { launchBillingFlow(yearlySku) }
        }

        val savingsNote = TextView(this).apply {
            text = "⭐  Save 79 % with the yearly plan!"
            textSize = if (isTablet) 16f else 14f
            gravity = Gravity.CENTER
            setTextColor(UiStyle.VIP_GOLD)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, dp(6), 0, dp(8))
        }

        val restoreBtn = Button(this).apply {
            text = "Restore Purchase"
            textSize = if (isTablet) 20f else 18f
            setAllCaps(false)
            UiStyle.styleBtnSecondary(this)
            setOnClickListener { queryPurchases() }
        }

        val closeBtn = Button(this).apply {
            text = "Done"
            textSize = if (isTablet) 20f else 18f
            setAllCaps(false)
            UiStyle.styleBtnSecondary(this)
            setOnClickListener { finish() }
        }

        fun btnP(topMarginDp: Int = 10): LinearLayout.LayoutParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(if (isTablet) 68 else 60)
            ).apply { setMargins(0, dp(topMarginDp), 0, 0) }

        root.addView(title, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ))
        root.addView(desc, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ))
        root.addView(subscribeBtn, btnP(0))
        root.addView(yearlyBtn, btnP())
        root.addView(savingsNote, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ))
        root.addView(restoreBtn, btnP(24))
        root.addView(closeBtn, btnP(8))

        val scroll = ScrollView(this).apply { isFillViewport = true }
        scroll.addView(root)
        setContentView(scroll)

        monetization = MonetizationManager(this)
        setupBillingClient()
    }

    override fun onDestroy() {
        billingClient.endConnection()
        super.onDestroy()
    }

    private fun setupBillingClient() {
        billingClient = BillingClient.newBuilder(this)
            .setListener { billingResult, purchases ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                    for (purchase in purchases) {
                        handlePurchase(purchase)
                    }
                }
            }
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
            )
            .build()

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryPurchases()
                }
            }
            override fun onBillingServiceDisconnected() {}
        })
    }

    private fun launchBillingFlow(sku: String) {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(sku)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                )
            ).build()
        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && productDetailsList.isNotEmpty()) {
                val productDetails = productDetailsList[0]
                val offerToken = productDetails.subscriptionOfferDetails?.get(0)?.offerToken
                if (offerToken != null) {
                    val flowParams = BillingFlowParams.newBuilder()
                        .setProductDetailsParamsList(
                            listOf(
                                BillingFlowParams.ProductDetailsParams.newBuilder()
                                    .setProductDetails(productDetails)
                                    .setOfferToken(offerToken)
                                    .build()
                            )
                        )
                        .build()
                    billingClient.launchBillingFlow(this, flowParams)
                } else {
                    Toast.makeText(this, "No offer available", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Product not found", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged) {
            val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    monetization.setVip(true)
                    runOnUiThread {
                        Toast.makeText(this, "VIP Activated! Unlimited AI replies are now enabled.", Toast.LENGTH_LONG).show()
                    }
                }
            }
        } else if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            monetization.setVip(true)
            runOnUiThread {
                Toast.makeText(this, "VIP Active", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun queryPurchases() {
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()
        ) { billingResult, purchasesList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                var found = false
                for (purchase in purchasesList) {
                    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                        handlePurchase(purchase)
                        found = true
                    }
                }
                if (!found) {
                    runOnUiThread {
                        Toast.makeText(this, "No active subscription found. If you purchased VIP, ensure you're signed into the same Google account.", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
