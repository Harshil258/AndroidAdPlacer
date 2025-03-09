package com.harshil258.adplacer.adClass

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.text.TextUtils
import android.view.ViewGroup
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.harshil258.adplacer.R
import com.harshil258.adplacer.utils.Extensions.isInterstitialAdEmpty
import com.harshil258.adplacer.utils.GlobalUtils
import com.harshil258.adplacer.utils.Logger
import com.harshil258.adplacer.utils.commonFunctions.logCustomEvent
import com.harshil258.adplacer.utils.sharedpreference.SecureStorageManager.Companion.sharedPrefConfig

class InterstitialManager {
    private val TAG = "InterstitialManager"
    private val adTimeoutDuration = 5000L
    private val clickDebounceTime = 750L
    private val defaultInterstitialFrequency = 3

    private var isAppInForeground = true
    private var loadingDialog: Dialog? = null
    private var lastClickTimestamp = 0L

    // Dependency injection for common utilities.
    private val globalUtils = GlobalUtils()

    interface InterstitialAdCallback {
        fun onContinueFlow()
    }


    private fun isMultipleClickDetected(interval: Long): Boolean {
        val currentTimestamp = System.currentTimeMillis()
        val isMultipleClick = currentTimestamp - lastClickTimestamp < interval
        Logger.d(TAG, "Multiple click detected: $isMultipleClick")
        lastClickTimestamp = currentTimestamp
        return isMultipleClick
    }

    private fun isClickCountSufficient(activity: Activity): Boolean {
        val appDetails = sharedPrefConfig.appDetails
        try {
            if (!isInterstitialAdEmpty()) {
                val frequency = appDetails.interstitialAdFrequency.takeIf {
                    it.isNotEmpty() && TextUtils.isDigitsOnly(it)
                }?.toInt() ?: defaultInterstitialFrequency

                Logger.d(TAG, "Current click count: $currentClickCount, Frequency: $frequency")
                if (currentClickCount == frequency - 1) {
                    Logger.d(TAG, "Preloading interstitial ad due to click count")
                    preloadInterstitialAd(activity)
                }

                return if (currentClickCount >= frequency) {
                    Logger.d(TAG, "Click count reached frequency")
                    true
                } else {
                    currentClickCount++
                    Logger.d(TAG, "Incrementing click count to $currentClickCount")
                    false
                }
            } else {
                currentClickCount = defaultInterstitialFrequency
                Logger.d(TAG, "Interstitial ad unit empty, setting click count to default")
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Error in isClickCountSufficient: ${e.message}")
        }
        return false
    }

    fun loadAndDisplayInterstitialAd(activity: Activity, callback: InterstitialAdCallback) {
        Logger.d(TAG, "loadAndDisplayInterstitialAd invoked")
        if (!isStateValidForInterstitial(activity, callback)) return

        if (currentInterstitialAd != null) {
            Logger.d(TAG, "An existing interstitial ad is available. Handling it.")
            handleExistingInterstitialAd(activity, callback)
            return
        }

        if (isInterstitialAdEmpty() || !globalUtils.isNetworkAvailable(activity.applicationContext)) {
            Logger.d(TAG, "Ad unit empty or network unavailable. Proceeding with flow.")
            safelyProceedFlow(callback)
            return
        }

        if (isClickCountSufficient(activity)) {
            Logger.d(TAG, "Click count sufficient. Loading interstitial ad.")
            loadInterstitialAd(activity, callback)
            showLoadingDialog(activity)
            startTimerToProceedFlow(activity, adTimeoutDuration, callback)
        } else {
            Logger.d(TAG, "Click count not sufficient. Proceeding without loading ad.")
            safelyProceedFlow(callback)
        }
    }

    private fun isStateValidForInterstitial(activity: Activity, callback: InterstitialAdCallback): Boolean {
        isAppInForeground = true

        if (isMultipleClickDetected(clickDebounceTime)) {
            Logger.d(TAG, "Multiple clicks detected. Aborting interstitial display.")
            return false
        }
        if (AppOpenAdManager.isAdShowing) {
            Logger.d(TAG, "An App Open ad is showing. Aborting interstitial display.")
            return false
        }
        if (isInterstitialAdLoading) {
            Logger.d(TAG, "Interstitial ad is loading. Showing loading dialog and starting timer.")
            showLoadingDialog(activity)
            startTimerToProceedFlow(activity, adTimeoutDuration, callback)
            return false
        }
        if (isInterstitialAdShowing) {
            Logger.d(TAG, "Interstitial ad is already showing. Aborting display.")
            stopLoadingDialog()
            return false
        }
        return true
    }

    private fun handleExistingInterstitialAd(activity: Activity, callback: InterstitialAdCallback) {
        isInterstitialAdLoading = false
        Logger.d(TAG, "Handling existing interstitial ad")
        if (isClickCountSufficient(activity)) {
            startTimerToProceedFlow(activity, adTimeoutDuration, callback)
        } else {
            safelyProceedFlow(callback)
        }
    }

    private fun safelyProceedFlow(callback: InterstitialAdCallback) {
        Logger.d(TAG, "Safely proceeding with flow without showing interstitial")
        stopLoadingDialog()
        if (isAppInForeground) {
            callback.onContinueFlow()
        }
    }

    private fun loadInterstitialAd(activity: Activity, callback: InterstitialAdCallback?) {
        if (isInterstitialAdLoading || isInterstitialAdShowing) {
            Logger.d(TAG, "Interstitial ad is either loading or already showing. Skipping load.")
            return
        }

        val adRequest = AdRequest.Builder().build()
        val adUnitId = sharedPrefConfig.appDetails.admobInterstitialAd
        Logger.d(TAG, "Loading interstitial ad with unit: $adUnitId")

        isInterstitialAdLoading = true

        InterstitialAd.load(activity, adUnitId, adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                isInterstitialAdLoading = false
                currentInterstitialAd = interstitialAd
                Logger.i(TAG, "Interstitial ad loaded successfully")

                // Show ad immediately if timer is still running
                if (flowTimer != null && isAppInForeground && callback != null) {
                    displayInterstitialAd(activity, callback)
                }
            }

            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                isInterstitialAdLoading = false
                currentInterstitialAd = null
                stopLoadingDialog()
                Logger.e(TAG, "Failed to load interstitial ad: ${loadAdError.message}")

                callback?.let {
                    Logger.e(TAG, "Failed to load interstitial ad: ${loadAdError.message}  isAppInForeground  $isAppInForeground")

                    if (isAppInForeground) {
                        it.onContinueFlow()
                    }
                }
            }
        })
    }

    fun preloadInterstitialAd(activity: Activity) {
        Logger.d(TAG, "Preloading interstitial ad")
        if (isInterstitialAdLoading || isInterstitialAdShowing) {
            stopLoadingDialog()
            Logger.d(TAG, "Interstitial ad is loading or showing. Skipping preload.")
            return
        }
        if (currentInterstitialAd != null) {
            isInterstitialAdLoading = false
            Logger.d(TAG, "Interstitial ad already exists. Skipping preload.")
            return
        }
        if (isInterstitialAdEmpty()) {
            Logger.d(TAG, "Interstitial ad unit is empty. Skipping preload.")
            return
        }
        if (!globalUtils.isNetworkAvailable(activity.applicationContext)) {
            Logger.d(TAG, "Network unavailable. Skipping interstitial preload.")
            return
        }
        loadInterstitialAd(activity, null)
    }

    private fun startTimerToProceedFlow(
        activity: Activity,
        duration: Long,
        callback: InterstitialAdCallback
    ) {
        Logger.d(TAG, "Starting timer for $duration ms")
        flowTimer = object : com.harshil258.adplacer.app.CountDownTimer(duration, 500L) {
            override fun onTick(millisUntilFinished: Long) {
                Logger.d(TAG, "Timer tick: $millisUntilFinished ms remaining")
                if (AppOpenAdManager.isAdShowing || !isAppInForeground) {
                    Logger.d(TAG, "Timer paused due to ad showing or app not in foreground")
                    flowTimer?.pause()
                    stopLoadingDialog()
                } else if (currentInterstitialAd != null && !isInterstitialAdLoading) {
                    Logger.d(TAG, "Ad is ready. Displaying interstitial ad.")
                    displayInterstitialAd(activity, callback)
                    flowTimer?.pause()
                    stopLoadingDialog()
                }
            }

            override fun onFinish() {
                Logger.d(TAG, "Timer finished - proceeding with flow")
                stopLoadingDialog()
                if (!AppOpenAdManager.isAdShowing && isAppInForeground) {
                    callback.onContinueFlow()
                }
            }
        }
        flowTimer?.start()
    }

    fun stopLoadingDialog() {
        try {
            loadingDialog?.let {
                if (it.isShowing) {
                    it.dismiss()
                    Logger.d(TAG, "Loading dialog dismissed")
                }
            }
            loadingDialog = null
        } catch (e: Exception) {
            Logger.e(TAG, "Error dismissing dialog: ${e.message}")
        }

        try {
            flowTimer?.pause()
            flowTimer?.cancel()
            Logger.d(TAG, "Timer canceled")
        } catch (e: Exception) {
            Logger.e(TAG, "Error canceling timer: ${e.message}")
        }
    }

    private fun showLoadingDialog(activity: Activity) {
        try {
            stopLoadingDialog()
            Logger.d(TAG, "Showing loading dialog")
            loadingDialog = Dialog(activity).apply {
                setContentView(R.layout.dialog_ad_loading)
                window?.setLayout(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                setCancelable(false)
            }
            if (!activity.isFinishing) {
                loadingDialog?.show()
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Error showing loading dialog: ${e.message}")
        }
    }

    fun displayInterstitialAd(activity: Activity, callback: InterstitialAdCallback) {
        if (isInterstitialAdLoading) {
            Logger.d(TAG, "Ad is still loading. Stopping loading dialog and continuing flow.")
            stopLoadingDialog()
            callback.onContinueFlow()
            return
        }
        if (isInterstitialAdShowing) {
            Logger.d(TAG, "Ad is already showing. Stopping loading dialog.")
            stopLoadingDialog()
            return
        }
        if (currentInterstitialAd == null) {
            Logger.d(TAG, "No interstitial ad available. Stopping loading dialog and continuing flow.")
            stopLoadingDialog()
            callback.onContinueFlow()
            return
        }

        Logger.d(TAG, "Displaying interstitial ad")
        currentInterstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdClicked() {
                Logger.d(TAG, "Interstitial ad clicked")
            }

            override fun onAdDismissedFullScreenContent() {
                Logger.d(TAG, "Interstitial ad dismissed")
                isInterstitialAdShowing = false
                currentInterstitialAd = null
                callback.onContinueFlow()
                try {
                    flowTimer?.cancel()
                    Logger.d(TAG, "Timer canceled on ad dismiss")
                } catch (e: Exception) {
                    Logger.e(TAG, "Error canceling timer on ad dismiss: ${e.message}")
                }
                stopLoadingDialog()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Logger.e(TAG, "Failed to show interstitial ad: ${adError.message}")
                isInterstitialAdShowing = false
                currentInterstitialAd = null
                stopLoadingDialog()
                if (isAppInForeground) {
                    callback.onContinueFlow()
                }
            }

            override fun onAdImpression() {
                Logger.i(TAG, "Interstitial ad impression recorded")
                val eventParams = mapOf("ADIMPRESSION" to "INTERSTITIAL")
                activity.let { logCustomEvent(it, "ADS_EVENT", eventParams) }
            }

            override fun onAdShowedFullScreenContent() {
                Logger.d(TAG, "Interstitial ad is now shown full screen")
                isInterstitialAdShowing = true
                stopLoadingDialog()
                currentInterstitialAd = null
                if (currentClickCount >= currentFrequency) {
                    currentClickCount = 1
                }
            }
        }
        isInterstitialAdShowing = true
        currentInterstitialAd?.show(activity)
    }

    companion object {
        var currentInterstitialAd: InterstitialAd? = null
        var isInterstitialAdShowing: Boolean = false
        private var isInterstitialAdLoading = false
        var currentFrequency: Int = 3
        var currentClickCount: Int = 3
        var flowTimer: com.harshil258.adplacer.app.CountDownTimer? = null
    }
}
