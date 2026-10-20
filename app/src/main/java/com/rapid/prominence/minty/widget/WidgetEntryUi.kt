package com.rapid.prominence.minty.widget

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.widget.TextView
import com.rapid.prominence.minty.R

/** Home and exit entry points corresponding to EnMusic's widget tip and exit retention. */
object WidgetEntryUi {
    fun requestFromNotification(activity: Activity, intent: Intent) {
        if (!intent.getBooleanExtra("local_push_request_widget_pin", false)) return
        intent.removeExtra("local_push_request_widget_pin")
        activity.window.decorView.post {
            if (!activity.isFinishing && !activity.isDestroyed) {
                WidgetPinHelper.requestPin(activity, WidgetPinSource.LOCAL_PUSH)
            }
        }
    }

    fun refreshHomeTip(activity: Activity, tip: View) {
        tip.visibility = if (WidgetPinHelper.canRequestPin(activity)) View.VISIBLE else View.GONE
        tip.setOnClickListener { WidgetPinHelper.requestPin(activity, WidgetPinSource.HOME_TIP) }
    }

    fun showExitRetain(activity: Activity, onExit: () -> Unit) {
        if (activity.isFinishing || activity.isDestroyed) return
        if (WidgetPinHelper.canRequestPin(activity)) {
            showWidgetRetainDialog(activity, onExit)
        } else {
            AlertDialog.Builder(activity)
                .setTitle("Exit Minty?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Exit") { _, _ -> onExit() }
                .show()
        }
    }

    private fun showWidgetRetainDialog(activity: Activity, onExit: () -> Unit) {
        val content = activity.layoutInflater.inflate(R.layout.dialog_widget_exit_retain, null)
        val dialog = AlertDialog.Builder(activity).setView(content).create()
        content.findViewById<TextView>(R.id.widgetExitButton).setOnClickListener {
            dialog.dismiss()
            activity.window.decorView.post {
                if (!activity.isFinishing && !activity.isDestroyed) onExit()
            }
        }
        content.findViewById<TextView>(R.id.widgetAddButton).setOnClickListener {
            dialog.dismiss()
            activity.window.decorView.post {
                if (!activity.isFinishing && !activity.isDestroyed) {
                    WidgetPinHelper.requestPin(activity, WidgetPinSource.EXIT)
                }
            }
        }
        dialog.setOnShowListener {
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
        if (!activity.isFinishing && !activity.isDestroyed) dialog.show()
    }
}
