
package com.screen2earn.app

import android.app.Activity
import android.app.AppOpsManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.util.Calendar

class MainActivity : Activity() {

    private lateinit var content: LinearLayout
    private lateinit var usageText: TextView
    private lateinit var appsLayout: LinearLayout

    private val backgroundColor = Color.rgb(12, 17, 39)
    private val cardColor = Color.rgb(27, 36, 66)
    private val accentColor = Color.rgb(30, 210, 160)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildDashboard()
    }

    override fun onResume() {
        super.onResume()
        if (::usageText.isInitialized) updateUsage()
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun rounded(color: Int, radius: Int = 18): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
        }
    }

    private fun text(
        value: String,
        size: Float,
        color: Int,
        bold: Boolean = false
    ): TextView {
        return TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            if (bold) setTypeface(null, Typeface.BOLD)
        }
    }

    private fun buildDashboard() {

        val scroll = ScrollView(this).apply {
            setBackgroundColor(backgroundColor)
            isFillViewport = true
        }

        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(35), dp(22), dp(35))
        }

        scroll.addView(content)

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(0, dp(12), 0, dp(28))
        }

        header.addView(
            text("Screen2Earn", 32f, Color.WHITE, true)
        )

        header.addView(
            text("YOUR SCREEN TIME HAS VALUE", 13f, accentColor, true)
        )

        content.addView(header)

        content.addView(
            text("Your Dashboard", 23f, Color.WHITE, true)
        )

        val todayCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(22), dp(22), dp(22))
            background = rounded(cardColor)
        }

        todayCard.addView(
            text("TODAY'S APP USAGE", 13f, Color.LTGRAY, true)
        )

        usageText = text("Loading usage data...", 32f, accentColor, true)
        usageText.setPadding(0, dp(12), 0, dp(8))
        todayCard.addView(usageText)

        todayCard.addView(
            text("Total reported app foreground time", 13f, Color.LTGRAY)
        )

        val cardParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        cardParams.setMargins(0, dp(18), 0, dp(25))
        content.addView(todayCard, cardParams)

        content.addView(
            text("Your Most Used Apps", 20f, Color.WHITE, true)
        )

        appsLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        content.addView(appsLayout)

        val permissionButton = Button(this).apply {
            text = "ENABLE USAGE ACCESS"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            }
        }

        val refreshButton = Button(this).apply {
            text = "REFRESH SCREEN TIME"
            setOnClickListener { updateUsage() }
        }

        content.addView(permissionButton)
        content.addView(refreshButton)

        val note = text(
            "Screen2Earn reports app foreground time. " +
                "This is not an exact device screen-on measurement.",
            12f,
            Color.LTGRAY
        )
        note.setPadding(0, dp(20), 0, 0)
        content.addView(note)

        setContentView(scroll)
        updateUsage()
    }

    private fun hasUsagePermission(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager

        return appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            packageName
        ) == AppOpsManager.MODE_ALLOWED
    }

    private fun formatTime(milliseconds: Long): String {
        val hours = milliseconds / 3600000
        val minutes = (milliseconds % 3600000) / 60000
        return "${hours}h ${minutes}m"
    }

    private fun getAppName(packageName: String): String {
        return try {
            val info = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(info).toString()
        } catch (e: Exception) {
            packageName
        }
    }

    private fun updateUsage() {

        if (!hasUsagePermission()) {
            usageText.text = "Permission needed"
            appsLayout.removeAllViews()
            appsLayout.addView(
                text(
                    "Enable Usage Access below to see your app usage.",
                    15f,
                    Color.LTGRAY
                )
            )
            return
        }

        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        val manager =
            getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

        val stats: List<UsageStats> = manager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            calendar.timeInMillis,
            System.currentTimeMillis()
        )

        val total = stats.sumOf { it.totalTimeInForeground }

        usageText.text = formatTime(total)

        appsLayout.removeAllViews()

        val topApps = stats
            .filter { it.totalTimeInForeground > 0 }
            .sortedByDescending { it.totalTimeInForeground }
            .take(8)

        if (topApps.isEmpty()) {
            appsLayout.addView(
                text("No app usage data available yet.", 15f, Color.LTGRAY)
            )
            return
        }

        for ((index, app) in topApps.withIndex()) {

            val appCard = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(16), dp(15), dp(16), dp(15))
                background = rounded(cardColor, 14)
            }

            val nameColumn = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
            }

            nameColumn.addView(
                text("${index + 1}. ${getAppName(app.packageName)}",
                    15f, Color.WHITE, true)
            )

            nameColumn.addView(
                text(app.packageName, 10f, Color.LTGRAY)
            )

            appCard.addView(nameColumn)

            appCard.addView(
                text(formatTime(app.totalTimeInForeground),
                    14f, accentColor, true)
            )

            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, dp(10), 0, 0)

            appsLayout.addView(appCard, params)
        }
    }
}
