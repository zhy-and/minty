package com.rapid.prominence.minty.ei.dialog

import android.content.Context
import android.view.Gravity
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDialog
import com.rapid.prominence.minty.R

@JvmOverloads
fun createDialog(
    context: Context,
    layoutResId: Int,
    isCanceled: Boolean,
    gravity: Int = Gravity.CENTER,
    style: Int? = null,
    block: (AppCompatDialog.() -> Unit)? = null
): AppCompatDialog = AppCompatDialog(context, R.style.ShowPopTheme).also { dialog ->
    dialog.setContentView(layoutResId)
    dialog.setCanceledOnTouchOutside(isCanceled)
    val window = dialog.window
    val layoutParams = window?.attributes
    layoutParams?.width = ViewGroup.LayoutParams.MATCH_PARENT
    layoutParams?.height = ViewGroup.LayoutParams.WRAP_CONTENT
    layoutParams?.gravity = gravity
    window?.attributes = layoutParams
    style?.let { window?.setWindowAnimations(it) }
    block?.let { it(dialog) }
}

@JvmOverloads
fun showDialogs(
    context: Context,
    layoutResId: Int,
    isCanceled: Boolean = false,
    gravity: Int = Gravity.CENTER,
    style: Int? = null,
    block: (AppCompatDialog.() -> Unit)? = null
): AppCompatDialog = createDialog(context, layoutResId, isCanceled, gravity, style, block).apply {
    show()
}

@JvmOverloads
fun showSpeDialogs(
    context: Context,
    layoutResId: Int,
    isCanceled: Boolean = false,
    gravity: Int = Gravity.CENTER,
    style: Int? = null,
    block: (AppCompatDialog.() -> Unit)? = null
): AppCompatDialog = createDialog(context, layoutResId, isCanceled, gravity, style, block)

