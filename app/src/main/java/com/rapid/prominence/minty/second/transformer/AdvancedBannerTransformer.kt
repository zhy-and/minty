package com.rapid.prominence.minty.second.transformer

import android.view.View
import androidx.viewpager2.widget.ViewPager2

class AdvancedBannerTransformer(
    private val minAlpha: Float = 0.5f,
    private val maxAlpha: Float = 1.0f,
    private val minScale: Float = 0.85f,
    private val revealWidth: Int = 0  // 传入你的 revealWidth 值
) : ViewPager2.PageTransformer {

    override fun transformPage(page: View, position: Float) {
        val clampedPosition = position.coerceIn(-1.0f, 1.0f)
        val absPosition = kotlin.math.abs(clampedPosition)

        // 计算透明度
        val alphaRange = maxAlpha - minAlpha
        val alpha = maxAlpha - (alphaRange * absPosition * absPosition)
        page.alpha = alpha.coerceIn(minAlpha, maxAlpha)

        // 保持原有的缩放效果
        val scaleRange = 1.0f - minScale
        val scale = 1.0f - (scaleRange * absPosition)
        page.scaleX = scale
        page.scaleY = scale

        // 保持间距效果
        if (revealWidth > 0) {
            // 根据 revealWidth 计算位移，保持原有的视觉间距
            val translationX = clampedPosition * revealWidth * 0.8f
            page.translationX = translationX
        }
    }
}