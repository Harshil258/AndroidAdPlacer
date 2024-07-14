package com.harshil258.adplacer.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.harshil258.adplacer.adClass.InterstitialManager
import com.harshil258.adplacer.app.AdPlacerApplication
import com.harshil258.adplacer.utils.Constants.adPlacerApplication
import com.harshil258.adplacer.utils.Constants.isAppInForeground

class HomeButtonReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_CLOSE_SYSTEM_DIALOGS) {
            val reason = intent.getStringExtra("reason")

            if (reason != null && (reason == "homekey" || reason == "recentapps")) {
                isAppInForeground = false
                try {
                    if (InterstitialManager.timer != null) {
                        InterstitialManager.timer!!.pause()
                        InterstitialManager.timer!!.cancel()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                adPlacerApplication.interstitialManager.stopLoadingdialog()
            }
        }
    }
}