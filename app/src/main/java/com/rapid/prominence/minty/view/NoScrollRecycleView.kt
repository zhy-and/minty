package com.rapid.prominence.minty.view
import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.recyclerview.widget.RecyclerView

/**
 * zivon
 * 解决viewPager和recycleView的滑动冲突
 */

open class NoScrollRecycleView(context: Context, attrs: AttributeSet?) : RecyclerView(context, attrs) {

    private val globalRect: Rect = Rect() // 临时数据

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        var x = ev?.rawX?.toInt()
        var y = ev?.rawY?.toInt()
        getGlobalVisibleRect(globalRect)
        if (ev?.action == MotionEvent.ACTION_MOVE) {
            if (globalRect.contains(x!!, y!!)) {
                parent.requestDisallowInterceptTouchEvent(true)
            }
        }
        return super.dispatchTouchEvent(ev)
    }
}