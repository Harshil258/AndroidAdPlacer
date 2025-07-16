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
import com.harshil258.adplacer.utils.Constants.isAppInForeground
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

    private var loadingDialog: Dialog? = null
    private var lastClickTimestamp = 0L

    // Add callback tracking to prevent multiple calls
    private var currentCallback: InterstitialAdCallback? = null
    private var isCallbackExecuted = false
    private var currentRequestId: String? = null

    // Dependency injection for common utilities.
    private val globalUtils = GlobalUtils()

    interface InterstitialAdCallback {
        fun onContinueFlow()
    }

    private fun generateRequestId(): String {
        return "req_${System.currentTimeMillis()}_${hashCode()}"
    }

    private fun executeCallbackOnce(requestId: String, reason: String) {
        Logger.d(TAG, "executeCallbackOnce called - RequestId: $requestId, Reason: $reason, CurrentRequestId: $currentRequestId, IsCallbackExecuted: $isCallbackExecuted")

        if (requestId != currentRequestId) {
            Logger.w(TAG, "RequestId mismatch - Expected: $currentRequestId, Got: $requestId. Ignoring callback.")
            return
        }

        if (isCallbackExecuted) {
            Logger.w(TAG, "Callback already executed for request $requestId. Ignoring duplicate call from: $reason")
            return
        }

        isCallbackExecuted = true
        Logger.i(TAG, "Executing callback for request $requestId - Reason: $reason")

        try {
            currentCallback?.onContinueFlow()
        } catch (e: Exception) {
            Logger.e(TAG, "Error executing callback: ${e.message}")
        } finally {
            cleanupCurrentRequest()
        }
    }

    private fun cleanupCurrentRequest() {
        Logger.d(TAG, "cleanupCurrentRequest called - RequestId: $currentRequestId")
        currentCallback = null
        isCallbackExecuted = false
        currentRequestId = null
        stopLoadingDialog()
    }

    private fun isMultipleClickDetected(interval: Long): Boolean {
        val currentTimestamp = System.currentTimeMillis()
        val isMultipleClick = currentTimestamp - lastClickTimestamp < interval
        Logger.d(TAG, "Multiple click detected: $isMultipleClick (Current: $currentTimestamp, Last: $lastClickTimestamp, Diff: ${currentTimestamp - lastClickTimestamp}ms)")
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

                Logger.d(TAG, "Click count check - Current: $currentClickCount, Frequency: $frequency")

                if (currentClickCount == frequency - 1) {
                    Logger.d(TAG, "Preloading interstitial ad due to click count reaching frequency-1")
                    preloadInterstitialAd(activity)
                }

                return if (currentClickCount >= frequency) {
                    Logger.d(TAG, "Click count sufficient - reached frequency threshold")
                    true
                } else {
                    currentClickCount++
                    Logger.d(TAG, "Click count incremented to $currentClickCount (not sufficient yet)")
                    false
                }
            } else {
                currentClickCount = defaultInterstitialFrequency
                Logger.d(TAG, "Interstitial ad unit empty, setting click count to default: $defaultInterstitialFrequency")
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Error in isClickCountSufficient: ${e.message}")
        }
        return false
    }

    fun loadAndDisplayInterstitialAd(activity: Activity, callback: InterstitialAdCallback) {
        val requestId = generateRequestId()
        Logger.d(TAG, "loadAndDisplayInterstitialAd invoked - RequestId: $requestId")

        // Setup new request
        currentRequestId = requestId
        currentCallback = callback
        isCallbackExecuted = false

        if (!isStateValidForInterstitial(activity, requestId)) {
            Logger.d(TAG, "State not valid for interstitial - RequestId: $requestId")
            return
        }

        if (currentInterstitialAd != null) {
            Logger.d(TAG, "Existing interstitial ad available - RequestId: $requestId")
            handleExistingInterstitialAd(activity, requestId)
            return
        }

        if (isInterstitialAdEmpty() || !globalUtils.isNetworkAvailable(activity.applicationContext)) {
            Logger.d(TAG, "Ad unit empty or network unavailable - RequestId: $requestId")
            executeCallbackOnce(requestId, "AD_UNIT_EMPTY_OR_NO_NETWORK")
            return
        }

        if (isClickCountSufficient(activity)) {
            Logger.d(TAG, "Click count sufficient, loading interstitial ad - RequestId: $requestId")
            loadInterstitialAd(activity, requestId)
            showLoadingDialog(activity)
            startTimerToProceedFlow(activity, adTimeoutDuration, requestId)
        } else {
            Logger.d(TAG, "Click count not sufficient, proceeding without ad - RequestId: $requestId")
            executeCallbackOnce(requestId, "CLICK_COUNT_NOT_SUFFICIENT")
        }
    }

    private fun isStateValidForInterstitial(activity: Activity, requestId: String): Boolean {
        isAppInForeground = true

        Logger.d(TAG, "Validating state for interstitial - RequestId: $requestId")

        if (isMultipleClickDetected(clickDebounceTime)) {
            Logger.d(TAG, "Multiple clicks detected, aborting - RequestId: $requestId")
            cleanupCurrentRequest()
            return false
        }

        if (AppOpenAdManager.isAdShowing) {
            Logger.d(TAG, "App Open ad is showing, aborting - RequestId: $requestId")
            cleanupCurrentRequest()
            return false
        }

        if (isInterstitialAdLoading) {
            Logger.d(TAG, "Interstitial ad is loading, showing loading dialog - RequestId: $requestId")
            showLoadingDialog(activity)
            startTimerToProceedFlow(activity, adTimeoutDuration, requestId)
            return false
        }

        if (isInterstitialAdShowing) {
            Logger.d(TAG, "Interstitial ad is already showing, aborting - RequestId: $requestId")
            cleanupCurrentRequest()
            return false
        }

        Logger.d(TAG, "State validation passed - RequestId: $requestId")
        return true
    }

    private fun handleExistingInterstitialAd(activity: Activity, requestId: String) {
        isInterstitialAdLoading = false
        Logger.d(TAG, "Handling existing interstitial ad - RequestId: $requestId")

        if (isClickCountSufficient(activity)) {
            Logger.d(TAG, "Click count sufficient, starting timer for existing ad - RequestId: $requestId")
            startTimerToProceedFlow(activity, adTimeoutDuration, requestId)
        } else {
            Logger.d(TAG, "Click count not sufficient for existing ad - RequestId: $requestId")
            executeCallbackOnce(requestId, "EXISTING_AD_CLICK_COUNT_NOT_SUFFICIENT")
        }
    }

    private fun loadInterstitialAd(activity: Activity, requestId: String?) {
        if (isInterstitialAdLoading || isInterstitialAdShowing) {
            Logger.d(TAG, "Interstitial ad is loading or showing, skipping load - RequestId: $requestId")
            return
        }

        val adRequest = AdRequest.Builder().build()
        val adUnitId = sharedPrefConfig.appDetails.admobInterstitialAd
        Logger.d(TAG, "Loading interstitial ad with unit: $adUnitId - RequestId: $requestId")

        isInterstitialAdLoading = true

        InterstitialAd.load(activity, adUnitId, adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                isInterstitialAdLoading = false
                currentInterstitialAd = interstitialAd
                Logger.i(TAG, "Interstitial ad loaded successfully - RequestId: $requestId")

                // Show ad immediately if timer is still running and request is still active
                if (flowTimer != null && isAppInForeground && requestId != null && requestId == currentRequestId && !isCallbackExecuted) {
                    Logger.d(TAG, "Ad loaded while timer running, displaying immediately - RequestId: $requestId")
                    displayInterstitialAd(activity, requestId)
                } else {
                    Logger.d(TAG, "Ad loaded but conditions not met for immediate display - RequestId: $requestId, CurrentRequestId: $currentRequestId, IsCallbackExecuted: $isCallbackExecuted")
                }
            }

            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                isInterstitialAdLoading = false
                currentInterstitialAd = null
                Logger.e(TAG, "Failed to load interstitial ad: ${loadAdError.message} - RequestId: $requestId")

                // Only execute callback if this is for the current request
                if (requestId != null && requestId == currentRequestId) {
                    executeCallbackOnce(requestId, "AD_LOAD_FAILED")
                } else {
                    Logger.w(TAG, "Ad load failed for different request - RequestId: $requestId, CurrentRequestId: $currentRequestId")
                }
            }
        })
    }

    fun preloadInterstitialAd(activity: Activity) {
        Logger.d(TAG, "Preloading interstitial ad")

        if (isInterstitialAdLoading || isInterstitialAdShowing) {
            Logger.d(TAG, "Interstitial ad is loading or showing, skipping preload")
            return
        }

        if (currentInterstitialAd != null) {
            Logger.d(TAG, "Interstitial ad already exists, skipping preload")
            return
        }

        if (isInterstitialAdEmpty()) {
            Logger.d(TAG, "Interstitial ad unit is empty, skipping preload")
            return
        }

        if (!globalUtils.isNetworkAvailable(activity.applicationContext)) {
            Logger.d(TAG, "Network unavailable, skipping interstitial preload")
            return
        }

        loadInterstitialAd(activity, null)
    }

    private fun startTimerToProceedFlow(
        activity: Activity,
        duration: Long,
        requestId: String
    ) {
        Logger.d(TAG, "Starting timer for ${duration}ms - RequestId: $requestId")

        // Cancel any existing timer
        flowTimer?.cancel()

        flowTimer = object : com.harshil258.adplacer.app.CountDownTimer(duration, 500L) {
            override fun onTick(millisUntilFinished: Long) {
                Logger.d(TAG, "Timer tick: ${millisUntilFinished}ms remaining - RequestId: $requestId, CurrentRequestId: $currentRequestId")

                // Check if this timer is for the current request
                if (requestId != currentRequestId) {
                    Logger.w(TAG, "Timer tick for different request, pausing - RequestId: $requestId, CurrentRequestId: $currentRequestId")
                    flowTimer?.pause()
                    return
                }

                if (AppOpenAdManager.isAdShowing || !isAppInForeground) {
                    Logger.d(TAG, "Timer paused due to ad showing or app not in foreground - RequestId: $requestId")
                    flowTimer?.pause()
                    stopLoadingDialog()
                } else if (currentInterstitialAd != null && !isInterstitialAdLoading && !isCallbackExecuted) {
                    Logger.d(TAG, "Ad ready during timer tick, displaying - RequestId: $requestId")
                    displayInterstitialAd(activity, requestId)
                    flowTimer?.pause()
                }
            }

            override fun onFinish() {
                Logger.d(TAG, "Timer finished - RequestId: $requestId, CurrentRequestId: $currentRequestId")

                // Only proceed if this is for the current request
                if (requestId == currentRequestId && !AppOpenAdManager.isAdShowing && isAppInForeground) {
                    executeCallbackOnce(requestId, "TIMER_FINISHED")
                } else {
                    Logger.w(TAG, "Timer finished but conditions not met - RequestId: $requestId, CurrentRequestId: $currentRequestId, AppOpenAdShowing: ${AppOpenAdManager.isAdShowing}, AppInForeground: $isAppInForeground")
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
            flowTimer = null
            Logger.d(TAG, "Timer canceled and nullified")
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

    fun displayInterstitialAd(activity: Activity, requestId: String) {
        Logger.d(TAG, "displayInterstitialAd called - RequestId: $requestId, CurrentRequestId: $currentRequestId")

        if (requestId != currentRequestId) {
            Logger.w(TAG, "Display called for different request - RequestId: $requestId, CurrentRequestId: $currentRequestId")
            return
        }

        if (isCallbackExecuted) {
            Logger.w(TAG, "Callback already executed, skipping display - RequestId: $requestId")
            return
        }

        if (isInterstitialAdLoading) {
            Logger.d(TAG, "Ad still loading, executing callback - RequestId: $requestId")
            executeCallbackOnce(requestId, "AD_STILL_LOADING")
            return
        }

        if (isInterstitialAdShowing) {
            Logger.d(TAG, "Ad already showing, stopping loading dialog - RequestId: $requestId")
            stopLoadingDialog()
            return
        }

        if (currentInterstitialAd == null) {
            Logger.d(TAG, "No interstitial ad available, executing callback - RequestId: $requestId")
            executeCallbackOnce(requestId, "NO_AD_AVAILABLE")
            return
        }

        Logger.d(TAG, "Displaying interstitial ad - RequestId: $requestId")

        currentInterstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdClicked() {
                Logger.d(TAG, "Interstitial ad clicked - RequestId: $requestId")
            }

            override fun onAdDismissedFullScreenContent() {
                Logger.d(TAG, "Interstitial ad dismissed - RequestId: $requestId")
                isInterstitialAdShowing = false
                currentInterstitialAd = null

                // Cancel timer since ad was shown successfully
                try {
                    flowTimer?.cancel()
                    flowTimer = null
                    Logger.d(TAG, "Timer canceled on ad dismiss - RequestId: $requestId")
                } catch (e: Exception) {
                    Logger.e(TAG, "Error canceling timer on ad dismiss: ${e.message}")
                }

                executeCallbackOnce(requestId, "AD_DISMISSED")
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Logger.e(TAG, "Failed to show interstitial ad: ${adError.message} - RequestId: $requestId")
                isInterstitialAdShowing = false
                currentInterstitialAd = null
                executeCallbackOnce(requestId, "AD_SHOW_FAILED")
            }

            override fun onAdImpression() {
                Logger.i(TAG, "Interstitial ad impression recorded - RequestId: $requestId")
                val eventParams = mapOf("ADIMPRESSION" to "INTERSTITIAL")
                activity.let { logCustomEvent(it, "ADS_EVENT", eventParams) }
            }

            override fun onAdShowedFullScreenContent() {
                Logger.d(TAG, "Interstitial ad shown full screen - RequestId: $requestId")
                isInterstitialAdShowing = true
                stopLoadingDialog()

                // Reset click count after successful ad show
                if (currentClickCount >= currentFrequency) {
                    currentClickCount = 1
                    Logger.d(TAG, "Click count reset to 1 after ad show - RequestId: $requestId")
                }

                // Don't null the ad here, let it be nulled in onAdDismissedFullScreenContent
                Logger.d(TAG, "Ad show setup complete - RequestId: $requestId")
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