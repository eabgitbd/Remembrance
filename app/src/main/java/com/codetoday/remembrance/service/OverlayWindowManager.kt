package com.codetoday.remembrance.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.codetoday.remembrance.R
import com.codetoday.remembrance.data.model.OverlayPosition
import com.codetoday.remembrance.data.model.ReminderItem
import com.codetoday.remembrance.data.model.SupportedLanguage
import com.codetoday.remembrance.domain.typography.FontCatalog

class OverlayWindowManager(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val mainHandler = Handler(Looper.getMainLooper())

    private var currentOverlayView: View? = null
    private var dismissRunnable: Runnable? = null
    private var safetyWatchdogRunnable: Runnable? = null
    var onDismissCallback: (() -> Unit)? = null

    init {
        createHeadsUpNotificationChannel()
    }

    private fun createHeadsUpNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                HEADS_UP_CHANNEL_ID,
                "Remembrance Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Shows heads-up alert when a remembrance triggers"
                enableVibration(false)
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showReminderOverlay(reminder: ReminderItem, onComplete: () -> Unit) {
        mainHandler.post {
            dismissCurrentOverlay()

            // Post a high-priority notification to guarantee system heads-up over other apps
            postHeadsUpNotification(reminder)

            if (!Settings.canDrawOverlays(context)) {
                onDismissCallback?.invoke()
                onComplete()
                return@post
            }

            val overlayView = createSquircleOverlayView(reminder)
            val layoutParams = buildLayoutParams(reminder.position)

            try {
                windowManager.addView(overlayView, layoutParams)
                currentOverlayView = overlayView

                overlayView.alpha = 0f
                overlayView.scaleX = 0.94f
                overlayView.scaleY = 0.94f
                overlayView.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(220)
                    .start()

                val durationMs = (reminder.durationSeconds * 1000L).coerceAtLeast(2000L)

                dismissRunnable = Runnable {
                    safelyRemoveOverlayView(overlayView, onComplete)
                }
                mainHandler.postDelayed(dismissRunnable!!, durationMs)

                // Safety watchdog runnable
                safetyWatchdogRunnable = Runnable {
                    if (currentOverlayView == overlayView) {
                        safelyRemoveOverlayView(overlayView, onComplete)
                    }
                }
                mainHandler.postDelayed(safetyWatchdogRunnable!!, durationMs + 800L)

                overlayView.setOnClickListener {
                    dismissRunnable?.let { mainHandler.removeCallbacks(it) }
                    safetyWatchdogRunnable?.let { mainHandler.removeCallbacks(it) }
                    safelyRemoveOverlayView(overlayView, onComplete)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                safelyRemoveOverlayView(overlayView, onComplete)
            }
        }
    }

    private fun postHeadsUpNotification(reminder: ReminderItem) {
        try {
            val title = when (reminder.language) {
                SupportedLanguage.BENGALI -> "স্মরণ"
                SupportedLanguage.ARABIC -> "الذكر"
                SupportedLanguage.ENGLISH -> "Remembrance"
            }

            val notification = NotificationCompat.Builder(context, HEADS_UP_CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(reminder.text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(reminder.text))
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(true)
                .setTimeoutAfter(reminder.durationSeconds * 1000L)
                .build()

            NotificationManagerCompat.from(context).notify(REMINDER_NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            // Permission might be pending
        }
    }

    private fun safelyRemoveOverlayView(view: View, onComplete: () -> Unit) {
        dismissRunnable?.let { mainHandler.removeCallbacks(it) }
        safetyWatchdogRunnable?.let { mainHandler.removeCallbacks(it) }

        try {
            view.animate().alpha(0f).scaleX(0.94f).scaleY(0.94f).setDuration(180).start()
        } catch (e: Exception) {
            // ignore
        }

        mainHandler.postDelayed({
            try {
                if (view.isAttachedToWindow) {
                    windowManager.removeView(view)
                }
            } catch (e: Exception) {
                // Already removed
            }
            if (currentOverlayView == view) {
                currentOverlayView = null
            }
            onDismissCallback?.invoke()
            onComplete()
        }, 200L)
    }

    fun dismissCurrentOverlay() {
        dismissRunnable?.let { mainHandler.removeCallbacks(it) }
        safetyWatchdogRunnable?.let { mainHandler.removeCallbacks(it) }

        currentOverlayView?.let { view ->
            try {
                if (view.isAttachedToWindow) {
                    windowManager.removeView(view)
                }
            } catch (e: Exception) {
                // ignore
            }
            currentOverlayView = null
            onDismissCallback?.invoke()
        }
    }

    private fun createSquircleOverlayView(reminder: ReminderItem): View {
        val root = FrameLayout(context).apply {
            clipChildren = false
            clipToPadding = false
        }

        val paddingH = dpToPx(20f)
        val paddingV = dpToPx(14f)

        val textView = TextView(context).apply {
            text = reminder.text
            setTextSize(TypedValue.COMPLEX_UNIT_SP, reminder.fontSizeSp)
            setTextColor(parseColorOrDefault(reminder.textColorHex, Color.BLACK))
            gravity = Gravity.CENTER
            setPadding(paddingH, paddingV, paddingH, paddingV)
            maxWidth = (context.resources.displayMetrics.widthPixels * 0.90f).toInt()

            // Apply selected custom Typeface directly to TextView
            typeface = FontCatalog.getTypeface(reminder.fontName, reminder.language)

            // Multiline text handling without truncation
            setSingleLine(false)
            maxLines = 15
            includeFontPadding = true
            setLineSpacing(dpToPx(6f).toFloat(), 1.25f)

            if (reminder.language == SupportedLanguage.ARABIC) {
                textDirection = View.TEXT_DIRECTION_RTL
            } else {
                textDirection = View.TEXT_DIRECTION_LOCALE
            }
        }

        val backgroundDrawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dpToPx(reminder.cornerRadiusDp).toFloat()

            val baseColor = parseColorOrDefault(reminder.backgroundColorHex, Color.parseColor("#32CD32"))
            val alphaInt = (reminder.backgroundOpacity.coerceIn(0f, 1f) * 255).toInt()
            val colorWithAlpha = Color.argb(
                alphaInt,
                Color.red(baseColor),
                Color.green(baseColor),
                Color.blue(baseColor)
            )
            setColor(colorWithAlpha)

            val borderWidthPx = dpToPx(reminder.borderWidthDp)
            val borderColor = parseColorOrDefault(reminder.borderColorHex, Color.BLACK)
            setStroke(borderWidthPx, borderColor)
        }

        textView.background = backgroundDrawable

        val lp = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.CENTER
        }

        root.addView(textView, lp)
        return root
    }

    private fun buildLayoutParams(position: OverlayPosition): WindowManager.LayoutParams {
        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        // Essential flags for displaying reliably above other apps without tapjacking blocks
        val flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN

        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            flags,
            PixelFormat.TRANSLUCENT
        )

        when (position) {
            OverlayPosition.TOP -> {
                lp.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                lp.y = dpToPx(52f)
            }
            OverlayPosition.SEMI_TOP -> {
                lp.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                lp.y = dpToPx(140f)
            }
            OverlayPosition.MIDDLE -> {
                lp.gravity = Gravity.CENTER
                lp.y = 0
            }
            OverlayPosition.SEMI_BOTTOM -> {
                lp.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                lp.y = dpToPx(140f)
            }
            OverlayPosition.BOTTOM -> {
                lp.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                lp.y = dpToPx(52f)
            }
        }

        return lp
    }

    private fun dpToPx(dp: Float): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            context.resources.displayMetrics
        ).toInt()
    }

    private fun parseColorOrDefault(hex: String, defaultColor: Int): Int {
        return try {
            Color.parseColor(hex)
        } catch (e: Exception) {
            defaultColor
        }
    }

    companion object {
        const val HEADS_UP_CHANNEL_ID = "remembrance_heads_up_channel"
        const val REMINDER_NOTIFICATION_ID = 2002
    }
}
