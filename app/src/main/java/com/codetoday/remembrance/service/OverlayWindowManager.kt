package com.codetoday.remembrance.service

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
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
import androidx.core.content.res.ResourcesCompat
import com.codetoday.remembrance.data.model.OverlayPosition
import com.codetoday.remembrance.data.model.ReminderItem
import com.codetoday.remembrance.data.model.SupportedLanguage

class OverlayWindowManager(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val mainHandler = Handler(Looper.getMainLooper())

    private var currentOverlayView: View? = null
    private var dismissRunnable: Runnable? = null
    var onDismissCallback: (() -> Unit)? = null

    fun showReminderOverlay(reminder: ReminderItem, onComplete: () -> Unit) {
        if (!Settings.canDrawOverlays(context)) {
            onComplete()
            return
        }

        mainHandler.post {
            dismissCurrentOverlay()

            val overlayView = createSquircleOverlayView(reminder)
            val layoutParams = buildLayoutParams(reminder.position)

            try {
                windowManager.addView(overlayView, layoutParams)
                currentOverlayView = overlayView

                // Fade-in animation
                overlayView.alpha = 0f
                overlayView.scaleX = 0.9f
                overlayView.scaleY = 0.9f
                overlayView.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(250)
                    .start()

                // Schedule auto-dismiss
                dismissRunnable = Runnable {
                    animateDismiss(overlayView) {
                        onComplete()
                    }
                }
                mainHandler.postDelayed(dismissRunnable!!, reminder.durationSeconds * 1000L)

                // Optional tap to dismiss immediately
                overlayView.setOnClickListener {
                    dismissRunnable?.let { mainHandler.removeCallbacks(it) }
                    animateDismiss(overlayView) {
                        onComplete()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete()
            }
        }
    }

    private fun createSquircleOverlayView(reminder: ReminderItem): View {
        val root = FrameLayout(context).apply {
            clipChildren = false
            clipToPadding = false
        }

        val paddingHorizontal = dpToPx(16f)
        val paddingVertical = dpToPx(10f)

        val textView = TextView(context).apply {
            text = reminder.text
            setTextSize(TypedValue.COMPLEX_UNIT_SP, reminder.fontSizeSp)
            setTextColor(parseColorOrDefault(reminder.textColorHex, Color.BLACK))
            gravity = Gravity.CENTER
            setPadding(paddingHorizontal, paddingVertical, paddingHorizontal, paddingVertical)

            // RTL support for Arabic, preserve Unicode ligatures for Bengali & English
            if (reminder.language == SupportedLanguage.ARABIC) {
                textDirection = View.TEXT_DIRECTION_RTL
            } else {
                textDirection = View.TEXT_DIRECTION_LOCALE
            }
        }

        // Squircle background shape with border and alpha
        val backgroundDrawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            val cornerPx = dpToPx(reminder.cornerRadiusDp).toFloat()
            cornerRadius = cornerPx

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

        val cardLayoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.CENTER
        }

        root.addView(textView, cardLayoutParams)
        return root
    }

    private fun buildLayoutParams(position: OverlayPosition): WindowManager.LayoutParams {
        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN

        val displayMetrics = context.resources.displayMetrics
        val screenHeight = displayMetrics.heightPixels

        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            flags,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = position.gravity
            x = 0
            y = (screenHeight * position.marginPercentY).toInt()
        }
    }

    private fun animateDismiss(view: View, onDone: () -> Unit) {
        view.animate()
            .alpha(0f)
            .scaleX(0.9f)
            .scaleY(0.9f)
            .setDuration(250)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    try {
                        windowManager.removeViewImmediate(view)
                    } catch (e: Exception) {
                        // ignore if already removed
                    }
                    if (currentOverlayView == view) {
                        currentOverlayView = null
                    }
                    onDismissCallback?.invoke()
                    onDone()
                }
            })
            .start()
    }

    fun dismissCurrentOverlay() {
        dismissRunnable?.let { mainHandler.removeCallbacks(it) }
        currentOverlayView?.let { view ->
            try {
                windowManager.removeViewImmediate(view)
            } catch (e: Exception) {
                // ignore
            }
            currentOverlayView = null
            onDismissCallback?.invoke()
        }
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
}
