package com.codetoday.remembrance.data.model

import android.view.Gravity

enum class OverlayPosition(
    val label: String,
    val gravity: Int,
    val marginPercentY: Float
) {
    TOP("Top", Gravity.TOP or Gravity.CENTER_HORIZONTAL, 0.05f),
    SEMI_TOP("Semi Top", Gravity.TOP or Gravity.CENTER_HORIZONTAL, 0.20f),
    MIDDLE("Middle", Gravity.CENTER, 0.00f),
    SEMI_BOTTOM("Semi Bottom", Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, 0.20f),
    BOTTOM("Bottom", Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, 0.05f);
}
