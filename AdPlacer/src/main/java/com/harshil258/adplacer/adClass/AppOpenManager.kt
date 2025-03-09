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
import com.harshil258.adplacer.utils.Constants.adPlacerInstance
import com.harshil258.adplacer.utils.Constants.isSplashScreenRunning
import com.harshil258.adplacer.utils.Extensions.isAppOpenAdEmpty
import com.harshil258.adplacer.utils.Logger
import com.harshil258.adplacer.utils.commonFunctions.logCustomEvent
import com.harshil258.adplacer.utils.sharedpreference.SecureStorageManager.Companion.sharedPrefConfig
import java.util.Date

class AppOpenAdManager {
    private var appOpenAdLoadCallback: AppOpenAdLoadCallback? = null

    /**
     * Displays the App Open ad if available and not already showing.
     * If the ad is unavailable or fails to load, a new ad is loaded.
     */
    fun showAppOpenAd(activity: Activity, adCallback: AdCallback) {
        Logger.d(TAG, "showAppOpenAd called")
        if (isAppOpenAdEmpty()) {
            Logger.d(TAG, "Ad unit ID is empty; not showing App Open ad.")
            isAdShowing = false
            return
        }

        if (isAdAvailable) {
            // Only show the ad if it's not already being shown.
            if (!isAdShowing) {
                Logger.d(TAG, "Ad is available and not currently showing. Displaying App Open ad.")
                isAdShowing = true
                currentAppOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        Logger.i(TAG, "App Open ad dismissed.")
                        currentAppOpenAd = null
                        isAdShowing = false
                        adCallback.onAdDisplayed(true)
                        loadAppOpenAd(activity, adCallback)
                    }

                    override fun onAdImpression() {
                        super.onAdImpression()
                        val eventParams = mapOf("ADIMPRESSION" to "APPOPEN")
                        logCustomEvent(activity, "ADS_EVENT", eventParams)
                        Logger.i(ADS_LOG_TAG, "onAdImpression: AppOpenAd")
                    }

                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                        Logger.e(TAG, "Failed to show App Open ad: ${adError.message}")
                        isAdShowing = false
                    }

                    override fun onAdShowedFullScreenContent() {
                        Logger.i(TAG, "App Open ad is shown full screen.")
                        isAdShowing = true
                        try {
                            adPlacerInstance.messagingListener?.hideSplashLoader()
                        } catch (e: Exception) {
                            Logger.e(TAG, "onAdShowedFullScreenContent error: ${e.message}")
                        }
                    }
                }
                currentAppOpenAd?.show(activity)
            } else {
                Logger.d(TAG, "App Open ad is already showing; skipping display.")
            }
        } else {
            Logger.d(TAG, "No App Open ad available; attempting to load one.")
            loadAppOpenAd(activity, adCallback)
        }
    }

    /**
     * Loads a new App Open ad if one is not already loaded or in the process of loading.
     */
    fun loadAppOpenAd(activity: Activity, adCallback: AdCallback) {
        if (isAdLoading || isAppOpenAdEmpty() || currentAppOpenAd != null) {
            Logger.d(TAG, "Ad is already loading, available, or ad unit ID is empty; skipping load.")
            return
        }

        appOpenAdLoadCallback = object : AppOpenAdLoadCallback() {
            override fun onAdLoaded(ad: AppOpenAd) {
                Logger.i(ADS_LOG_TAG, "onAdLoaded: AppOpenAd loaded successfully.")
                currentAppOpenAd = ad
                isAdLoading = false
                adLoadTime = Date().time

                // If the splash screen is still running, show the ad immediately.
                if (isSplashScreenRunning) {
                    Logger.d(TAG, "Splash screen is running; displaying App Open ad immediately.")
                    showAppOpenAd(activity, adCallback)
                }
            }

            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                Logger.e(ADS_LOG_TAG, "Failed to load App Open ad: ${loadAdError.message}")
                isAdLoading = false
            }
        }

        try {
            val adRequest = AdRequest.Builder().build()
            Logger.d(TAG, "Loading App Open ad with unit ID: ${sharedPrefConfig.appDetails.admobAppOpenAd}")
            AppOpenAd.load(
                activity.applicationContext,
                sharedPrefConfig.appDetails.admobAppOpenAd,
                adRequest,
                appOpenAdLoadCallback!!
            )
            isAdLoading = true
        } catch (e: Exception) {
            Logger.e(TAG, "Exception loading App Open ad: ${e.message}")
            isAdLoading = false
        }
    }

    /**
     * Returns true if an App Open ad is loaded and it was loaded within the specified number of hours.
     */
    val isAdAvailable: Boolean
        get() = currentAppOpenAd != null && wasAdLoadedWithinHours(4)

    private fun wasAdLoadedWithinHours(hours: Long): Boolean {
        val timeSinceLoad = Date().time - adLoadTime
        val millisecondsPerHour: Long = 3600000
        val loadedWithinTime = timeSinceLoad < (millisecondsPerHour * hours)
        Logger.d(TAG, "wasAdLoadedWithinHours: $loadedWithinTime (loaded $timeSinceLoad ms ago)")
        return loadedWithinTime
    }

    companion object {
        var currentAppOpenAd: AppOpenAd? = null
        var isAdShowing: Boolean = false
        var isAdLoading: Boolean = false

        var shouldStopAppOpenAd: Boolean = false
        private var adLoadTime: Long = 0
        private const val ADS_LOG_TAG = "AppOpenAdManager"
        private const val TAG = "AppOpenAdManager"
        // isSplashScreenRunning should be maintained by your splash screen logic.
    }
}
