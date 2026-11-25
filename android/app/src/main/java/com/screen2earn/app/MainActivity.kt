
package com.screen2earn.app

import android.app.Activity
import android.app.AppOpsManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {

    private lateinit var usageText: TextView
    private lateinit var appsLayout: LinearLayout
    private lateinit var goalText: TextView
    private lateinit var goalProgress: ProgressBar
    private lateinit var goalStatus: TextView
    private lateinit var weeklyLayout: LinearLayout

    private val backgroundColor = Color.rgb(12, 17, 39)
    private val cardColor = Color.rgb(27, 36, 66)
    private val accentColor = Color.rgb(30, 210, 160)
    private val blueColor = Color.rgb(100, 150, 255)

    private val preferences by lazy {
        getSharedPreferences("screen2earn_settings", Context.MODE_PRIVATE)
    }

    private var dailyGoalMinutes = 240

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dailyGoalMinutes = preferences.getInt("daily_goal", 240)
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

    private fun formatTime(milliseconds: Long): String {
        val hours = milliseconds / 3600000
        val minutes = (milliseconds % 3600000) / 60000
        return "${hours}h ${minutes}m"
    }

    private fun formatGoal(minutes: Int): String {
        val hours = minutes / 60
        val remaining = minutes % 60
        return if (remaining == 0) "${hours}h"
        else "${hours}h ${remaining}m"
    }

    private fun makeCard(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(20))
            background = rounded(cardColor)
        }
    }

    private fun addSectionSpace(parent: LinearLayout, bottom: Int = 20) {
        val space = TextView(this)
        parent.addView(
            space,
            LinearLayout.LayoutParams(1, dp(bottom))
        )
    }

    private fun buildDashboard() {
        val scroll = ScrollView(this).apply {
            setBackgroundColor(backgroundColor)
            isFillViewport = true
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(35), dp(22), dp(35))
        }

        scroll.addView(content)

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(0, dp(12), 0, dp(28))
        }

        header.addView(text("Screen2Earn", 32f, Color.WHITE, true))
        header.addView(
            text("YOUR SCREEN TIME HAS VALUE", 13f, accentColor, true)
        )
        content.addView(header)

        content.addView(text("Your Dashboard", 23f, Color.WHITE, true))
        addSectionSpace(content, 16)

        // Today's usage
        val todayCard = makeCard()
        todayCard.addView(
            text("TODAY'S APP USAGE", 13f, Color.LTGRAY, true)
        )

        usageText = text("Loading usage data...", 32f, accentColor, true)
        usageText.setPadding(0, dp(12), 0, dp(8))
        todayCard.addView(usageText)
        todayCard.addView(
            text("Total reported app foreground time", 13f, Color.LTGRAY)
        )
        content.addView(todayCard)
        addSectionSpace(content, 20)

        // Daily goal
        val goalCard = makeCard()
        goalCard.addView(
            text("YOUR DAILY SCREEN-TIME GOAL", 14f, Color.LTGRAY, true)
        )

        goalText = text("", 25f, Color.WHITE, true)
        goalText.setPadding(0, dp(12), 0, dp(8))
        goalCard.addView(goalText)

        goalProgress = ProgressBar(
            this, null, android.R.attr.progressBarStyleHorizontal
        ).apply {
            max = 100
            progressTintList = ColorStateList.valueOf(blueColor)
            progressBackgroundTintList =
                ColorStateList.valueOf(Color.rgb(55, 65, 95))
        }

        val progressParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(12)
        )
        progressParams.setMargins(0, dp(8), 0, dp(8))
        goalCard.addView(goalProgress, progressParams)

        goalStatus = text("", 12f, accentColor, true)
        goalCard.addView(goalStatus)

        goalCard.addView(
            text("Adjust your daily target:", 13f, Color.LTGRAY)
        )

        val goalButtons = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val decreaseButton = Button(this).apply {
            text = "− 30 MIN"
            setOnClickListener {
                dailyGoalMinutes = (dailyGoalMinutes - 30).coerceAtLeast(30)
                saveGoal()
                updateUsage()
            }
        }

        val increaseButton = Button(this).apply {
            text = "+ 30 MIN"
            setOnClickListener {
                dailyGoalMinutes = (dailyGoalMinutes + 30).coerceAtMost(960)
                saveGoal()
                updateUsage()
            }
        }

        goalButtons.addView(
            decreaseButton,
            LinearLayout.LayoutParams(0, dp(52), 1f)
        )
        goalButtons.addView(
            increaseButton,
            LinearLayout.LayoutParams(0, dp(52), 1f)
        )
        goalCard.addView(goalButtons)
        content.addView(goalCard)
        addSectionSpace(content, 24)

        // Weekly report
        content.addView(
            text("Your Weekly Report", 20f, Color.WHITE, true)
        )
        addSectionSpace(content, 12)

        val weeklyCard = makeCard()
        weeklyCard.addView(
            text("LAST 7 DAYS", 13f, Color.LTGRAY, true)
        )

        weeklyLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        weeklyCard.addView(weeklyLayout)

        content.addView(weeklyCard)
        addSectionSpace(content, 24)

        // Wallet
        content.addView(
            text("Your Wallet", 20f, Color.WHITE, true)
        )
        addSectionSpace(content, 12)

        val walletCard = makeCard()
        walletCard.addView(
            text("AVAILABLE BALANCE", 13f, Color.LTGRAY, true)
        )
        walletCard.addView(
            text("₦0.00", 32f, accentColor, true).apply {
                setPadding(0, dp(10), 0, dp(4))
            }
        )
        walletCard.addView(
            text("Estimated rewards: ₦0.00", 15f, Color.WHITE, true)
        )
        walletCard.addView(
            text(
                "Demo wallet. Real rewards will appear after funded " +
                    "advertising campaigns are connected.",
                12f,
                Color.LTGRAY
            ).apply {
                setPadding(0, dp(12), 0, dp(16))
            }
        )

        val walletButton = Button(this).apply {
            text = "EARNINGS HISTORY"
            isEnabled = false
        }
        walletCard.addView(walletButton)

        content.addView(walletCard)
        addSectionSpace(content, 24)

        // Most used apps
        content.addView(
            text("Your Most Used Apps", 20f, Color.WHITE, true)
        )
        addSectionSpace(content, 12)

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

        content.addView(
            text(
                "Screen2Earn reports app foreground time. " +
                    "This is not an exact device screen-on measurement.",
                12f,
                Color.LTGRAY
            ).apply {
                setPadding(0, dp(20), 0, 0)
            }
        )

        setContentView(scroll)
        updateUsage()
    }

    private fun saveGoal() {
        preferences.edit()
            .putInt("daily_goal", dailyGoalMinutes)
            .apply()
    }

    private fun hasUsagePermission(): Boolean {
        val appOps =
            getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager

        return appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            packageName
        ) == AppOpsManager.MODE_ALLOWED
    }

    private fun getAppName(packageName: String): String {
        return try {
            val info = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(info).toString()
        } catch (e: Exception) {
            packageName
        }
    }

    private fun getAppIcon(packageName: String): android.graphics.drawable.Drawable? {
        return try {
            packageManager.getApplicationIcon(packageName)
        } catch (e: Exception) {
            null
        }
    }

    private fun getManager(): UsageStatsManager {
        return getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    }

    private fun getWeeklyUsage(): List<Pair<String, Long>> {
        val result = mutableListOf<Pair<String, Long>>()
        val manager = getManager()
        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())

        for (daysAgo in 6 downTo 0) {
            val start = Calendar.getInstance()
            start.add(Calendar.DAY_OF_YEAR, -daysAgo)
            start.set(Calendar.HOUR_OF_DAY, 0)
            start.set(Calendar.MINUTE, 0)
            start.set(Calendar.SECOND, 0)
            start.set(Calendar.MILLISECOND, 0)

            val end = start.clone() as Calendar
            end.add(Calendar.DAY_OF_YEAR, 1)

            val stats = manager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                start.timeInMillis,
                minOf(end.timeInMillis, System.currentTimeMillis())
            )

            val total = stats.sumOf { it.totalTimeInForeground }
            result.add(Pair(dayFormat.format(start.time), total))
        }

        return result
    }

    private fun displayWeeklyReport() {
        weeklyLayout.removeAllViews()

        val days = getWeeklyUsage()
        val maximum = days.maxOfOrNull { it.second } ?: 0L

        for ((day, milliseconds) in days) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(12), 0, dp(12))
            }

            row.addView(
                text(day, 13f, Color.WHITE, true),
                LinearLayout.LayoutParams(dp(42), dp(30))
            )

            val bar = ProgressBar(
                this, null, android.R.attr.progressBarStyleHorizontal
            ).apply {
                max = 100
                progress = if (maximum > 0) {
                    ((milliseconds * 100) / maximum).toInt()
                } else {
                    0
                }
                progressTintList = ColorStateList.valueOf(accentColor)
                progressBackgroundTintList =
                    ColorStateList.valueOf(Color.rgb(55, 65, 95))
            }

            row.addView(
                bar,
                LinearLayout.LayoutParams(0, dp(9), 1f)
            )

            val timeLabel = text(
                formatTime(milliseconds),
                11f,
                Color.LTGRAY
            )
            timeLabel.gravity = Gravity.END

            row.addView(
                timeLabel,
                LinearLayout.LayoutParams(dp(65), dp(30))
            )

            weeklyLayout.addView(row)
        }
    }

    private fun updateUsage() {
        if (!hasUsagePermission()) {
            usageText.text = "Permission needed"
            goalText.text = "Enable Usage Access"
            goalProgress.progress = 0
            goalStatus.text = "Usage permission is required"
            weeklyLayout.removeAllViews()
            appsLayout.removeAllViews()
            appsLayout.addView(
                text(
                    "Enable Usage Access to see your app usage.",
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

        val stats: List<UsageStats> = getManager().queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            calendar.timeInMillis,
            System.currentTimeMillis()
        )

        val total = stats.sumOf { it.totalTimeInForeground }
        usageText.text = formatTime(total)

        val goalMilliseconds = dailyGoalMinutes.toLong() * 60000
        val goalPercent = if (goalMilliseconds > 0) {
            ((total * 100) / goalMilliseconds).toInt()
        } else {
            0
        }

        goalText.text =
            "${formatTime(total)} / ${formatGoal(dailyGoalMinutes)}"
        goalProgress.progress = goalPercent.coerceIn(0, 100)

        goalStatus.text = if (total >= goalMilliseconds) {
            "Daily goal reached!"
        } else {
            "${100 - goalPercent}% remaining to reach your goal"
        }

        displayWeeklyReport()
        displayApps(stats, total)
    }

    private fun displayApps(stats: List<UsageStats>, total: Long) {
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
                orientation = LinearLayout.VERTICAL
                setPadding(dp(16), dp(15), dp(16), dp(15))
                background = rounded(cardColor, 14)
            }

            val topRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            val iconView = ImageView(this).apply {
                setImageDrawable(getAppIcon(app.packageName))
                scaleType = ImageView.ScaleType.FIT_CENTER
            }

            val iconParams = LinearLayout.LayoutParams(dp(44), dp(44))
            iconParams.setMargins(0, 0, dp(13), 0)
            topRow.addView(iconView, iconParams)

            val nameColumn = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
            }

            nameColumn.addView(
                text(
                    "${index + 1}. ${getAppName(app.packageName)}",
                    15f,
                    Color.WHITE,
                    true
                )
            )
            nameColumn.addView(
                text(app.packageName, 10f, Color.LTGRAY)
            )
            topRow.addView(nameColumn)

            topRow.addView(
                text(
                    formatTime(app.totalTimeInForeground),
                    14f,
                    accentColor,
                    true
                )
            )

            appCard.addView(topRow)

            val percent = if (total > 0) {
                ((app.totalTimeInForeground * 100) / total)
                    .toInt().coerceIn(0, 100)
            } else {
                0
            }

            val bar = ProgressBar(
                this, null, android.R.attr.progressBarStyleHorizontal
            ).apply {
                max = 100
                progress = percent
                progressTintList = ColorStateList.valueOf(accentColor)
                progressBackgroundTintList =
                    ColorStateList.valueOf(Color.rgb(55, 65, 95))
            }

            val barParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(7)
            )
            barParams.setMargins(0, dp(13), 0, dp(5))
            appCard.addView(bar, barParams)

            appCard.addView(
                text("$percent% of reported app usage", 11f, Color.LTGRAY)
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
