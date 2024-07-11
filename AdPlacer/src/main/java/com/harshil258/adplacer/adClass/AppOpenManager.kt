package com.harshil258.adplacer.adClass

import android.app.Activity
import com.harshil258.adplacer.interfaces.AdCallback
import com.harshil258.adplacer.utils.SharedPrefConfig.Companion.sharedPrefConfig
import com.harshil258.adplacer.utils.extentions.isAppOpenEmpty
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.appopen.AppOpenAd.AppOpenAdLoadCallback
import com.harshil258.adplacer.app.AdPlacerApplication
import java.util.Date

class AppOpenManager {
    private var appOpenAdLoadCallback: AppOpenAdLoadCallback? = null
    fun showAppOpen(activity: Activity, callBack: AdCallback) {
        if (!isAppOpenEmpty()) {
            if (isAdAvailable || com.harshil258.adplacer.adClass.AppOpenManager.Companion.isAdShowing) {
                if (!com.harshil258.adplacer.adClass.AppOpenManager.Companion.isAdShowing) {
                    com.harshil258.adplacer.adClass.AppOpenManager.Companion.isAdShowing = true
                    com.harshil258.adplacer.adClass.AppOpenManager.Companion.appOpenAd!!.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdClicked() {
                            super.onAdClicked()
                        }

                        override fun onAdDismissedFullScreenContent() {
                            com.harshil258.adplacer.adClass.AppOpenManager.Companion.appOpenAd = null
                            com.harshil258.adplacer.adClass.AppOpenManager.Companion.isAdShowing = false
                            callBack.adDisplayedCallback(true)
                            loadAppOpen(activity, callBack)
                        }

                        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                            com.harshil258.adplacer.adClass.AppOpenManager.Companion.isAdShowing = false
                        }

                        override fun onAdImpression() {
                            super.onAdImpression()
                        }

                        override fun onAdShowedFullScreenContent() {
                            com.harshil258.adplacer.adClass.AppOpenManager.Companion.isAdShowing = true
                            try {
                                AdPlacerApplication.adPlacerApplication.messagingCallback!!.hideSplashLoader()
                            } catch (e: Exception) {
                            }
                        }
                    }
                    com.harshil258.adplacer.adClass.AppOpenManager.Companion.appOpenAd!!.show(activity)
                }
            } else {
                loadAppOpen(activity, callBack)
            }
        } else {
            com.harshil258.adplacer.adClass.AppOpenManager.Companion.isAdShowing = false
        }
    }

    private fun loadAppOpen(activity: Activity, callBack: AdCallback) {
        if (com.harshil258.adplacer.adClass.AppOpenManager.Companion.isAdLoading) {
            return
        }
        if (isAppOpenEmpty()) {
            return
        }

        if (com.harshil258.adplacer.adClass.AppOpenManager.Companion.appOpenAd != null) {
            return
        }
        appOpenAdLoadCallback = object : AppOpenAdLoadCallback() {
            override fun onAdLoaded(ad: AppOpenAd) {
                com.harshil258.adplacer.adClass.AppOpenManager.Companion.appOpenAd = ad
                com.harshil258.adplacer.adClass.AppOpenManager.Companion.isAdLoading = false
                com.harshil258.adplacer.adClass.AppOpenManager.Companion.loadTime = Date().time

                if (AdPlacerApplication.isSplashRunning) showAppOpen(activity, callBack)
            }

            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
            }
        }

        try {
            appOpenAdLoadCallback?.apply {
                val build = AdRequest.Builder().build()
                AppOpenAd.load(
                    activity.applicationContext,
                    sharedPrefConfig.appDetails.admobAppOpenAd.toString(),
                    build,
                    AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT,
                    this
                )
            }

            com.harshil258.adplacer.adClass.AppOpenManager.Companion.isAdLoading = true
        } catch (e: Exception) {
            com.harshil258.adplacer.adClass.AppOpenManager.Companion.isAdLoading = false
        }
    }


    val isAdAvailable: Boolean
        get() = com.harshil258.adplacer.adClass.AppOpenManager.Companion.appOpenAd != null && wasLoadTimeLessThanNHoursAgo(4)

    private fun wasLoadTimeLessThanNHoursAgo(numHours: Long): Boolean {
        val dateDifference = Date().time - com.harshil258.adplacer.adClass.AppOpenManager.Companion.loadTime
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
