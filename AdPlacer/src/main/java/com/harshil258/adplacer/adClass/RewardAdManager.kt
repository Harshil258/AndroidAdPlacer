package com.harshil258.adplacer.adClass

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.text.TextUtils
import android.view.ViewGroup
import android.widget.Toast
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.harshil258.adplacer.R
import com.harshil258.adplacer.interfaces.RewardAdCallBack
import com.harshil258.adplacer.utils.Constants.isAppInForeground
import com.harshil258.adplacer.utils.Extensions.isRewardAdEmpty
import com.harshil258.adplacer.utils.GlobalUtils
import com.harshil258.adplacer.utils.GlobalUtils.Companion.checkMultipleClick2
import com.harshil258.adplacer.utils.Logger
import com.harshil258.adplacer.utils.commonFunctions.logCustomEvent
import com.harshil258.adplacer.utils.sharedpreference.SecureStorageManager.Companion.sharedPrefConfig

class RewardAdManager {
    var TAG: String = "RewardAdManager"

    fun isCounterSatisfy(): Boolean {
        Logger.d(TAG, "isCounterSatisfy: Entered")
        try {
            val appDetail = sharedPrefConfig.appDetails
            if (!isRewardAdEmpty()) {
                if (appDetail.rewardAdFrequency.isEmpty() || appDetail.rewardAdFrequency == "" ||
                    !TextUtils.isDigitsOnly(appDetail.rewardAdFrequency)) {
                    counter = 3
                    Logger.d(TAG, "isCounterSatisfy: Invalid frequency. Setting counter to 3")
                } else {
                    try {
                        counter = appDetail.rewardAdFrequency.toInt()
                        Logger.d(TAG, "isCounterSatisfy: Parsed counter = $counter")
                    } catch (e: Exception) {
                        counter = 3
                        Logger.e(TAG, "isCounterSatisfy: Exception parsing frequency, defaulting counter to 3 $e")
                    }
                }
                return if (clickCounts >= counter) {
                    Logger.d(TAG, "isCounterSatisfy: Condition satisfied (clickCounts: $clickCounts, counter: $counter)")
                    true
                } else {
                    clickCounts++
                    Logger.d(TAG, "isCounterSatisfy: Condition not met, incrementing clickCounts to $clickCounts")
                    false
                }
            } else {
                counter = 3
                clickCounts = counter
                Logger.d(TAG, "isCounterSatisfy: Reward ad is empty. Resetting counter and clickCounts")
                return false
            }
        } catch (e: Exception) {
            Logger.e(TAG, "isCounterSatisfy: Exception occurred $e")
            return false
        }
    }

    fun loadAndShowReward(activity: Activity, callBack: RewardAdCallBack) {
        Logger.i(TAG, "loadAndShowReward: Entered")
        isAppInForeground = true
        if (checkMultipleClick2(2500)) {
            Logger.d(TAG, "loadAndShowReward: Multiple clicks detected, aborting")
            return
        }
        if (AppOpenAdManager.isAdShowing) {
            Logger.d(TAG, "loadAndShowReward: Another ad is already showing, aborting")
            return
        }
        if (isAdLoading) {
            Logger.i(TAG, "loadAndShowReward: Ad is loading. Showing loading dialog and starting timer")
            showLoadingDialog(activity)
            startTimerForContinueFlow(activity, 5000, callBack)
            return
        }
        if (isAdShowing) {
            Logger.i(TAG, "loadAndShowReward: Ad is already showing, stopping loading dialog")
            stopLoadingdialog()
            return
        }
        if (mRewardAd != null) {
            isAdLoading = false
            Logger.d(TAG, "loadAndShowReward: mRewardAd exists")
            if (isCounterSatisfy()) {
                startTimerForContinueFlow(activity, 5000, callBack)
            } else {
                if (isAppInForeground) {
                    Logger.d(TAG, "loadAndShowReward: Counter condition not satisfied, continuing flow")
                    callBack.onContinueFlow()
                }
            }
            return
        }
        if (isRewardAdEmpty()) {
            Logger.w(TAG, "loadAndShowReward: Reward ad is empty, notifying callback")
            callBack.onAdNotAvailable(dialog)
            return
        }
        if (!GlobalUtils().isNetworkAvailable(activity.applicationContext)) {
            Logger.e(TAG, "loadAndShowReward: Network unavailable")
            stopLoadingdialog()
            if (!checkMultipleClick2(2000)) {
                Toast.makeText(activity, "Please Connect To the internet", Toast.LENGTH_SHORT).show()
            }
            return
        }
        if (!AppOpenAdManager.isAdShowing) {
            if (isCounterSatisfy()) {
                Logger.i(TAG, "loadAndShowReward: Loading reward ad")
                loadRewardAd(activity, callBack)
                showLoadingDialog(activity)
                startTimerForContinueFlow(activity, 5000, callBack)
            } else {
                if (isAppInForeground) {
                    Logger.d(TAG, "loadAndShowReward: Counter condition not met, ad not available")
                    callBack.onAdNotAvailable(dialog)
                }
            }
        }
        Logger.i(TAG, "loadAndShowReward: Exiting")
    }

    private fun loadRewardAd(activity: Activity, callBack: RewardAdCallBack?) {
        Logger.i(TAG, "loadRewardAd: Entered")
        if (isAdLoading || isAdShowing) {
            Logger.d(TAG, "loadRewardAd: Already loading or showing an ad, aborting load")
            return
        }

        val adRequest = AdRequest.Builder().build()
        val AD_UNIT: String = sharedPrefConfig.appDetails.admobRewardAd
        isAdLoading = true
        Logger.d(TAG, "loadRewardAd: Loading ad with unit $AD_UNIT")
        RewardedAd.load(activity, AD_UNIT, adRequest, object : RewardedAdLoadCallback() {
            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                isAdLoading = false
                mRewardAd = null
                Logger.e(TAG, "loadRewardAd: onAdFailedToLoad - ${loadAdError.message}")
                callBack?.let {
                    if (isAppInForeground) {
                        Logger.d(TAG, "loadRewardAd: App is in foreground, notifying failure")
                    }
                }
            }

            override fun onAdLoaded(rewardedAd: RewardedAd) {
                Logger.i(TAG, "loadRewardAd: onAdLoaded - Reward ad loaded successfully")
                isAdLoading = false
                mRewardAd = rewardedAd
            }
        })
        Logger.i(TAG, "loadRewardAd: Exiting")
    }

    fun preloadRewardAd(activity: Activity) {
        Logger.i(TAG, "preloadRewardAd: Entered")
        if (isAdLoading || isAdShowing) {
            Logger.d(TAG, "preloadRewardAd: Ad is loading or showing, stopping any loading dialog")
            stopLoadingdialog()
            return
        }
        if (mRewardAd != null) {
            isAdLoading = false
            Logger.d(TAG, "preloadRewardAd: mRewardAd already exists, no need to preload")
            return
        }
        if (isRewardAdEmpty()) {
            Logger.w(TAG, "preloadRewardAd: Reward ad is empty, aborting preload")
            return
        }
        if (!GlobalUtils().isNetworkAvailable(activity.applicationContext)) {
            Logger.e(TAG, "preloadRewardAd: Network unavailable, aborting preload")
            return
        }
        loadRewardAd(activity, null)
        Logger.i(TAG, "preloadRewardAd: Exiting")
    }

    fun stopLoadingdialog() {
        Logger.d(TAG, "stopLoadingdialog: Attempting to stop loading dialog and timer")
        try {
            if (dialog != null && dialog?.isShowing == true) {
                dialog?.dismiss()
                Logger.d(TAG, "stopLoadingdialog: Dialog dismissed")
            }
        } catch (e: Exception) {
            Logger.e(TAG, "stopLoadingdialog: Exception dismissing dialog $e")
        }
        try {
            timer?.let {
                it.pause()
                it.cancel()
                Logger.d(TAG, "stopLoadingdialog: Timer stopped")
            }
        } catch (e: Exception) {
            Logger.e(TAG, "stopLoadingdialog: Exception stopping timer  $e")
        }
    }

    private fun startTimerForContinueFlow(activity: Activity, duration: Long, callBack: RewardAdCallBack) {
        Logger.i(TAG, "startTimerForContinueFlow: Starting timer for $duration ms")
        timer = object : com.harshil258.adplacer.app.CountDownTimer(duration, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                Logger.d(TAG, "startTimerForContinueFlow: Timer tick - $millisUntilFinished ms remaining")
                if (AppOpenAdManager.isAdShowing || !isAppInForeground) {
                    Logger.d(TAG, "startTimerForContinueFlow: Ad showing or app not in foreground, pausing timer")
                    timer?.pause()
                    stopLoadingdialog()
                } else if (mRewardAd != null && !isAdLoading) {
                    Logger.d(TAG, "startTimerForContinueFlow: Reward ad available, showing ad")
                    showRewardAd(activity, callBack)
                    timer?.pause()
                    stopLoadingdialog()
                }
            }

            override fun onFinish() {
                Logger.d(TAG, "startTimerForContinueFlow: Timer finished")
                if (!AppOpenAdManager.isAdShowing) {
                    callBack.onAdNotAvailable(dialog)
                }
            }
        }
        timer?.start()
    }

    var dialog: Dialog? = null

    private fun showLoadingDialog(activity: Activity) {
        Logger.d(TAG, "showLoadingDialog: Attempting to show loading dialog")
        try {
            if (dialog != null && dialog?.isShowing == true) {
                dialog?.dismiss()
                Logger.d(TAG, "showLoadingDialog: Previous dialog dismissed")
            }
        } catch (e: Exception) {
            Logger.e(TAG, "showLoadingDialog: Exception dismissing previous dialog $e")
        }
        dialog = Dialog(activity).apply {
            setContentView(R.layout.dialog_ad_loading)
            window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setCancelable(false)
            try {
                if (!activity.isFinishing) {
                    show()
                    Logger.d(TAG, "showLoadingDialog: Dialog shown")
                }
            } catch (e: Exception) {
                Logger.e(TAG, "showLoadingDialog: Exception showing dialog $e")
            }
        }
    }

    fun showRewardAd(activity: Activity?, callBack: RewardAdCallBack) {
        Logger.i(TAG, "showRewardAd: Entered")
        if (isAdLoading) {
            Logger.d(TAG, "showRewardAd: Ad is loading, stopping dialog and continuing flow")
            stopLoadingdialog()
            callBack.onContinueFlow()
            return
        }
        if (isAdShowing) {
            Logger.d(TAG, "showRewardAd: Ad is already showing, stopping dialog")
            stopLoadingdialog()
            return
        }
        if (mRewardAd == null) {
            Logger.w(TAG, "showRewardAd: mRewardAd is null, continuing flow")
            stopLoadingdialog()
            callBack.onContinueFlow()
            return
        }

        mRewardAd!!.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdClicked() {
                Logger.d(TAG, "showRewardAd: Ad clicked")
            }

            override fun onAdDismissedFullScreenContent() {
                Logger.i(TAG, "showRewardAd: Ad dismissed")
                isAdShowing = false
                mRewardAd = null
                callBack.onContinueFlow()
                try {
                    timer?.cancel()
                    Logger.d(TAG, "showRewardAd: Timer cancelled")
                } catch (e: Exception) {
                    Logger.e(TAG, "showRewardAd: Exception cancelling timer $e")
                }
                stopLoadingdialog()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Logger.e(TAG, "showRewardAd: Failed to show ad - ${adError.message}")
                isAdShowing = false
                mRewardAd = null
                if (isAppInForeground) {
                    callBack.onContinueFlow()
                }
            }

            override fun onAdImpression() {
                val eventParams = mapOf("ADIMPRESSION" to "REWARD")
                activity?.let { logCustomEvent(it, "ADS_EVENT", eventParams) }
                Logger.i(TAG, "showRewardAd: Ad impression recorded")
            }

            override fun onAdShowedFullScreenContent() {
                Logger.i(TAG, "showRewardAd: Ad showed full screen")
                isAdShowing = true
                stopLoadingdialog()
                mRewardAd = null
                if (clickCounts >= counter) {
                    clickCounts = 1
                    Logger.d(TAG, "showRewardAd: Resetting clickCounts to 1")
                }
            }
        }

        mRewardAd?.let {
            isAdShowing = true
            Logger.i(TAG, "showRewardAd: Showing rewarded ad")
            it.show(activity!!) { callBack.onRewardGranted() }
        }
        Logger.i(TAG, "showRewardAd: Exiting")
    }

    companion object {
        var mRewardAd: RewardedAd? = null
        var isAdShowing: Boolean = false
        private var isAdLoading = false
        var counter: Int = 3
        var clickCounts: Int = 3
        var timer: com.harshil258.adplacer.app.CountDownTimer? = null
    }
}
