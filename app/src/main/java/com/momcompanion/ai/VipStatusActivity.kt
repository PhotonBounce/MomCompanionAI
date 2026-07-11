package com.friendai

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.LinearLayout
import android.view.Gravity

class VipStatusActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(40, 40, 40, 40)
        }

        val monetization = MonetizationManager(this)
        val statusText = TextView(this).apply {
            text = if (monetization.isVipUser()) {
                "You are a VIP! Unlimited AI replies and all VIP features are unlocked."
            } else {
                val remaining = monetization.getRemainingAiReplies()
                "Free version: $remaining AI reply${if (remaining == 1) "" else "s"} remaining today.\n\nUpgrade to VIP for unlimited replies and more."
            }
            textSize = 22f
            gravity = Gravity.CENTER
        }

        val upgradeBtn = if (!monetization.isVipUser()) Button(this).apply {
            text = "Upgrade to VIP"
            textSize = 20f
            setAllCaps(false)
            contentDescription = "Upgrade to VIP — unlimited AI replies"
            setOnClickListener {
                startActivity(android.content.Intent(this@VipStatusActivity, VipActivity::class.java))
            }
        } else null

        val closeBtn = Button(this).apply {
            text = "Done"
            textSize = 18f
            setAllCaps(false)
            contentDescription = "Close this screen"
            setOnClickListener { finish() }
        }

        root.addView(statusText)
        if (upgradeBtn != null) root.addView(upgradeBtn)
        root.addView(closeBtn)
        setContentView(root)
    }
}
