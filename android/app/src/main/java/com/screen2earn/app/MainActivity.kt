package com.screen2earn.app

import android.app.Activity
import android.app.AppOpsManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {

    private lateinit var mainLayout: LinearLayout
    private lateinit var usageText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showScreen()
    }

    override fun onResume() {
        super.onResume()
        if (::usageText.isInitialized) {
            updateUsage()
        }
    }

    private fun showScreen() {
        mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 48, 32, 32)
            setBackgroundColor(android.graphics.Color.rgb(12, 17, 39))
        }

        val title = TextView(this).apply {
            text = "Screen2Earn"
            textSize = 30f
            setTextColor(android.graphics.Color.WHITE)
            gravity = Gravity.CENTER
        }

        val subtitle = TextView(this).apply {
            text = "Your Screen Time Has Value."
            textSize = 16f
            setTextColor(android.graphics.Color.LTGRAY)
            gravity = Gravity.CENTER
        }

        usageText = TextView(this).apply {
            textSize = 18f
            setTextColor(android.graphics.Color.WHITE)
            setPadding(0, 40, 0, 40)
        }

        val permissionButton = Button(this).apply {
            text = "Enable Usage Access"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            }
        }

        val refreshButton = Button(this).apply {
            text = "Refresh Screen Time"
            setOnClickListener { updateUsage() }
        }

        mainLayout.addView(title)
        mainLayout.addView(subtitle)
        mainLayout.addView(usageText)
        mainLayout.addView(permissionButton)
        mainLayout.addView(refreshButton)

        setContentView(mainLayout)
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

    private fun updateUsage() {
        if (!hasUsagePermission()) {
            usageText.text =
                "Usage access is not enabled yet.\n\nTap the button below, find Screen2Earn, and allow Usage Access."
            return
        }

        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()

        val manager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

        val stats: List<UsageStats> = manager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        )

        val totalTime = stats.sumOf { it.totalTimeInForeground }
        val hours = totalTime / 3600000
        val minutes = (totalTime % 3600000) / 60000

        val topApps = stats
            .filter { it.totalTimeInForeground > 0 }
            .sortedByDescending { it.totalTimeInForeground }
            .take(8)

        val formatter = SimpleDateFormat("hh:mm a", Locale.getDefault())

        val result = StringBuilder()
        result.append("TODAY'S APP USAGE\n\n")
        result.append("Reported app foreground time:\n")
        result.append("${hours}h ${minutes}m\n\n")
        result.append("TOP USED APPS\n\n")

        if (topApps.isEmpty()) {
            result.append("No app usage data available yet.")
        } else {
            for (app in topApps) {
                val appHours = app.totalTimeInForeground / 3600000
                val appMinutes = (app.totalTimeInForeground % 3600000) / 60000

                val appName = try {
                    val info = packageManager.getApplicationInfo(
                        app.packageName, 0
                    )
                    packageManager.getApplicationLabel(info).toString()
                } catch (e: Exception) {
                    app.packageName
                }

                result.append("$appName — ${appHours}h ${appMinutes}m\n")
            }
        }

        result.append("\nUpdated: ${formatter.format(Date())}")
        result.append("\n\nThese are app foreground-time totals, not an exact device screen-on measurement.")

        usageText.text = result.toString()
    }
}
