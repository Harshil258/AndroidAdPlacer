package com.harshil258.adplacer.utils

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Uri
import android.os.SystemClock
import android.widget.Toast
import com.zeel_enterprise.shreekhodalkotlin.common.SecureStorageManager.Companion.sharedPrefConfig


class GlobalUtils {
    fun isNetworkAvailable(context: Context): Boolean {
        try {
            val manager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val networkInfo = manager.activeNetworkInfo
            var isAvailable = false
            if (networkInfo != null && networkInfo.isConnectedOrConnecting) {
                isAvailable = true
            }
            return isAvailable
        } catch (e: Exception) {
            return false
        }
    }


    fun share(context: Activity, app_name: String, pacakge: String) {
        if (checkMultipleClick(1200)) {
            return
        }
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.setType("text/plain")
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, app_name)
        var shareMessage = "here $app_name "
        shareMessage =
            shareMessage + "https://play.google.com/store/apps/details?id=" + pacakge + "\n\n"
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage)
        context.startActivity(Intent.createChooser(shareIntent, "choose one"))
    }

    fun rateUs(context: Activity) {
        if (checkMultipleClick(750)) {
            return
        }
        try {
            val url = "https://play.google.com/store/apps/details?id=" + context.packageName
            val i = Intent(Intent.ACTION_VIEW)
            i.setData(Uri.parse(url))
            context.startActivity(i)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, " unable to find market app", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
        }
    }

    fun openLinkInBrowser(context: Context, link: String?) {
        try {
            if (link == null || link.trim { it <= ' ' }.isEmpty()) {
                Toast.makeText(context, "Invalid link", Toast.LENGTH_SHORT).show()
                return
            }
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
            context.startActivity(intent)
        } catch (e: java.lang.Exception) {
            // Handle exception
        }
    }

    fun privacy(context: Activity) {
        if (!checkMultipleClick(2000)) {
            val appDetail = sharedPrefConfig.appDetails
            if (appDetail.privacyPolicyUrl != "" && appDetail.privacyPolicyUrl.isNotEmpty()
            ) {
                val i = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(appDetail.privacyPolicyUrl)
                )
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                i.setPackage("com.android.chrome")
                try {
                    context.startActivity(i)
                } catch (e: ActivityNotFoundException) {
                    i.setPackage(null)
                    context.startActivity(i)
                }
            } else {
                Toast.makeText(context, "Unable to open", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        private var mLastClickTime: Long = 0

        @JvmStatic
        fun checkMultipleClick(duration: Long): Boolean {
            if (SystemClock.elapsedRealtime() - mLastClickTime < duration) {
                return true
            }
            mLastClickTime = SystemClock.elapsedRealtime()
            return false
        }

        private var mLastClickTime2: Long = 0

        @JvmStatic
        fun checkMultipleClick2(duration: Long): Boolean {
            if (SystemClock.elapsedRealtime() - mLastClickTime2 < duration) {
                return true
            }
            mLastClickTime2 = SystemClock.elapsedRealtime()
            return false
        }
    }
}
