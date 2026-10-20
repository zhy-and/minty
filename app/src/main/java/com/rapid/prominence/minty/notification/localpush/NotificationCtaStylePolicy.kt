package com.rapid.prominence.minty.notification.localpush

enum class NotificationCtaAnimation {
    NONE,
    SCALE_BLUE,
    SCALE_GREEN,
    SCALE_RED,
    SCALE_YELLOW,
    SCALE_BLACK,
    SHIMMER,
}

data class NotificationCtaStyle(
    val animation: NotificationCtaAnimation,
    val usesTintableBackground: Boolean,
)

object NotificationCtaStylePolicy {
    fun resolve(buttonBackground: Int, buttonAnimation: String?): NotificationCtaStyle {
        val animation = when (buttonAnimation?.trim()?.lowercase()) {
            "none", "off", "false", "0" -> NotificationCtaAnimation.NONE
            else -> when (buttonBackground) {
                0xFF0B7CFF.toInt() -> NotificationCtaAnimation.SCALE_BLUE
                0xFF14B86A.toInt() -> NotificationCtaAnimation.SCALE_GREEN
                0xFFFF5A3D.toInt() -> NotificationCtaAnimation.SCALE_RED
                0xFFFFE66D.toInt() -> NotificationCtaAnimation.SCALE_YELLOW
                0xFF000000.toInt() -> NotificationCtaAnimation.SCALE_BLACK
                else -> NotificationCtaAnimation.SHIMMER
            }
        }
        return NotificationCtaStyle(
            animation = animation,
            usesTintableBackground = animation == NotificationCtaAnimation.SHIMMER ||
                (animation == NotificationCtaAnimation.NONE && !isPresetColor(buttonBackground)),
        )
    }

    private fun isPresetColor(color: Int): Boolean {
        return color == 0xFF0B7CFF.toInt() ||
            color == 0xFF14B86A.toInt() ||
            color == 0xFFFF5A3D.toInt() ||
            color == 0xFFFFE66D.toInt()
    }
}
