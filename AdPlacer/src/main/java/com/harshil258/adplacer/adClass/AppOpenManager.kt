package com.harshil258.adplacer.adClass

import android.app.Activity
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.appopen.AppOpenAd.AppOpenAdLoadCallback
import com.harshil258.adplacer.interfaces.AdCallback
import com.harshil258.adplacer.utils.Constants.adPlacerApplication
import com.harshil258.adplacer.utils.Constants.isSplashRunning
import com.harshil258.adplacer.utils.Logger
import com.harshil258.adplacer.utils.Logger.ADSLOG
import com.harshil258.adplacer.utils.Logger.TAG
import com.harshil258.adplacer.utils.commonFunctions.logCustomEvent
import com.harshil258.adplacer.utils.extentions.isAppOpenEmpty
import com.zeel_enterprise.shreekhodalkotlin.common.SecureStorageManager.Companion.sharedPrefConfig
import java.util.Date

class AppOpenManager {
    private var appOpenAdLoadCallback: AppOpenAdLoadCallback? = null
    fun showAppOpen(activity: Activity, callBack: AdCallback) {
        if (!isAppOpenEmpty()) {
            if (isAdAvailable || isAdShowing) {
                if (!isAdShowing) {
                    isAdShowing = true
                    appOpenAd!!.fullScreenContentCallback = object : FullScreenContentCallback() {

                        override fun onAdDismissedFullScreenContent() {
                            appOpenAd = null
                            isAdShowing = false
                            callBack.adDisplayedCallback(true)
                            loadAppOpen(activity, callBack)
                        }

                        override fun onAdImpression() {
                            super.onAdImpression()
                            val eventParams = mapOf("ADIMPRESSION" to "APPOPEN")
                            logCustomEvent(activity, "ADS_EVENT", eventParams)
                            Log.i(ADSLOG, "onAdImpression: AppOpenAd")

                        }

                        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                            isAdShowing = false
                        }

                        override fun onAdShowedFullScreenContent() {
                            isAdShowing = true
                            try {
                                adPlacerApplication.messagingCallback!!.hideSplashLoader()
                            } catch (e: Exception) {
                                Logger.e(TAG, "onAdShowedFullScreenContent: ${e.message}")
                            }
                        }
                    }
                    appOpenAd!!.show(activity)
                }
            } else {
                loadAppOpen(activity, callBack)
            }
        } else {
            isAdShowing = false
        }
    }

    fun loadAppOpen(activity: Activity, callBack: AdCallback) {
        if (isAdLoading) {
            return
        }
        if (isAppOpenEmpty()) {
            return
        }

        if (appOpenAd != null) {
            return
        }

        appOpenAdLoadCallback = object : AppOpenAdLoadCallback() {
            override fun onAdLoaded(ad: AppOpenAd) {

                Log.i(ADSLOG, "onAdLoaded: AppOpenAd")
                appOpenAd = ad
                isAdLoading = false
                loadTime = Date().time

                if (isSplashRunning) showAppOpen(activity, callBack)
            }

            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
            }
        }

        try {
            appOpenAdLoadCallback?.apply {
                val build = AdRequest.Builder().build()

                Logger.e("ADIDSSSS", "APP OPEN   ${sharedPrefConfig.appDetails.admobAppOpenAd}")

                AppOpenAd.load(
                    activity.applicationContext,
                    sharedPrefConfig.appDetails.admobAppOpenAd,
                    build,
                    this
                )
            }

            isAdLoading = true
        } catch (e: Exception) {
            isAdLoading = false
        }
    }


    val isAdAvailable: Boolean
        get() = appOpenAd != null && wasLoadTimeLessThanNHoursAgo(4)

    private fun wasLoadTimeLessThanNHoursAgo(numHours: Long): Boolean {
        val dateDifference = Date().time - loadTime
        val numMilliSecondsPerHour: Long = 3600000
        return (dateDifference < (numMilliSecondsPerHour * numHours))
    }

    companion object {
        var appOpenAd: AppOpenAd? = null
        @JvmField
        var isAdShowing: Boolean = false
        var isAdLoading: Boolean = false

        var shouldStopAppOpen: Boolean = false
        private var loadTime: Long = 0
    }
}
