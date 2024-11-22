package com.harshil258.adplacer.adClass

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.text.TextUtils
import android.util.Log
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
import com.harshil258.adplacer.utils.GlobalUtils
import com.harshil258.adplacer.utils.GlobalUtils.Companion.checkMultipleClick2
import com.harshil258.adplacer.utils.Logger
import com.harshil258.adplacer.utils.Logger.ADSLOG
import com.harshil258.adplacer.utils.commonFunctions.logCustomEvent
import com.harshil258.adplacer.utils.extentions.isRewardEmpty
import com.zeel_enterprise.shreekhodalkotlin.common.SecureStorageManager.Companion.sharedPrefConfig

class RewardAdManager {
    var TAG: String = "Interstitial"


    fun isCounterSatisfy(): Boolean {
        try {
            val appDetail = sharedPrefConfig.appDetails
            if (!isRewardEmpty()) {
                if (appDetail.rewardAdFrequency.isEmpty() || appDetail.rewardAdFrequency == "" || !TextUtils.isDigitsOnly(appDetail.rewardAdFrequency)
                ) {
                    counter = 3
                } else {
                    try {
                        counter = appDetail.rewardAdFrequency.toInt()
                    } catch (e: Exception) {
                        counter = 3
                        e.printStackTrace()
                    }
                }
                if (clickCounts >= counter) {
                    return true
                } else {
                    clickCounts++
                    return false
                }
            } else {
                counter = 3
                clickCounts = counter
                return false
            }
        } catch (e: Exception) {
            return false
        }
    }

    fun loadAndShowReward(activity: Activity, callBack: RewardAdCallBack) {
        isAppInForeground = true
        if (checkMultipleClick2(2500)) {
            return
        }
        if (AppOpenManager.isAdShowing) {
            return
        }
        if (isAdLoading) {
            showLoadingDialog(activity)
            startTimerForContinueFlow(activity, 5000, callBack)


            return
        }
        if (isAdShowing) {
            stopLoadingdialog()
            return
        }
        if (mRewardAd != null) {
            isAdLoading = false
            if (isCounterSatisfy()) {
                startTimerForContinueFlow(activity, 5000, callBack)
            } else {
                if (isAppInForeground) {
                    callBack.onContinueFlow()
                }
            }
            return
        }
        if (isRewardEmpty()) {
            callBack.onAdNotAvailable(dialog)
            return
        }

        if (!GlobalUtils().isNetworkAvailable(activity.applicationContext)) {
            stopLoadingdialog()


            if (!checkMultipleClick2(2000)) {
                Toast.makeText(activity, "Please Connect To the internet", Toast.LENGTH_SHORT)
                    .show()
            }
            return
        }
        if (!AppOpenManager.isAdShowing) {
            if (isCounterSatisfy()) {
                loadRewardAd(activity, callBack)
                showLoadingDialog(activity)
                startTimerForContinueFlow(activity, 5000, callBack)
            } else {
                if (isAppInForeground) {
                    callBack.onAdNotAvailable(dialog)
                }
            }
        }
    }

    private fun loadRewardAd(activity: Activity, callBack: RewardAdCallBack?) {
        if (isAdLoading || isAdShowing) {
            return
        }

        val adRequest = AdRequest.Builder().build()
        val AD_UNIT: String = sharedPrefConfig.appDetails.admobRewardAd
        isAdLoading = true
        RewardedAd.load(activity, AD_UNIT, adRequest, object : RewardedAdLoadCallback() {
            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                isAdLoading = false
                mRewardAd = null


                if (callBack != null) {
                    if (isAppInForeground) {
                        Logger.d(TAG, "onAdFailedToLoad: AppIsInForeground")
                    }
                }
            }

            override fun onAdLoaded(rewardedAd: RewardedAd) {
                Log.i(ADSLOG, "onAdLoaded: RewardAd")
                isAdLoading = false
                mRewardAd = rewardedAd
            }
        })
    }

    fun preloadRewardAd(activity: Activity) {
        if (isAdLoading || isAdShowing) {
            stopLoadingdialog()
            return
        }
        if (mRewardAd != null) {
            isAdLoading = false
            return
        }
        if (isRewardEmpty()) {
            return
        }

        if (!GlobalUtils().isNetworkAvailable(activity.applicationContext)) {
            return
        }

        loadRewardAd(activity, null)
    }

    fun stopLoadingdialog() {
        try {
            if (dialog != null && dialog?.isShowing == true) {
                dialog?.dismiss()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            if (timer != null) {
                timer!!.pause()
                timer!!.cancel()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startTimerForContinueFlow(
        activity: Activity,
        duration: Long,
        callBack: RewardAdCallBack
    ) {
        timer = object : com.harshil258.adplacer.app.CountDownTimer(duration, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                if (AppOpenManager.isAdShowing || !isAppInForeground) {
                    timer?.pause()
                    stopLoadingdialog()
                } else if (mRewardAd != null && !isAdLoading) {
                    showRewardAd(activity, callBack)
                    timer?.pause()
                    stopLoadingdialog()
                }
            }

            override fun onFinish() {
                if (!AppOpenManager.isAdShowing) {
                    callBack.onAdNotAvailable(dialog)
                }
            }
        }
        timer?.start()
    }

    var dialog: Dialog? = null

    private fun showLoadingDialog(activity: Activity) {
        try {
            if (dialog != null && dialog?.isShowing == true) {
                dialog?.dismiss()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        dialog = Dialog(activity)
        dialog?.setContentView(R.layout.dialog_ad_loading)
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.setCancelable(false)
        try {
            if (!activity.isFinishing) {
                dialog?.show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun showRewardAd(activity: Activity?, callBack: RewardAdCallBack) {
        if (isAdLoading) {
            stopLoadingdialog()
            callBack.onContinueFlow()
            return
        }
        if (isAdShowing) {
            stopLoadingdialog()
            return
        }

        if (mRewardAd == null) {
            stopLoadingdialog()
            callBack.onContinueFlow()
            return
        }

        mRewardAd!!.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdClicked() {
            }

            override fun onAdDismissedFullScreenContent() {
                isAdShowing = false
                mRewardAd = null
                callBack.onContinueFlow()
                try {
                    if (timer != null) timer!!.cancel()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                stopLoadingdialog()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                isAdShowing = false
                mRewardAd = null
                if (isAppInForeground) {
                    callBack.onContinueFlow()
                }
            }

            override fun onAdImpression() {
                val eventParams = mapOf("ADIMPRESSION" to "REWARD")
                activity?.let { logCustomEvent(it, "ADS_EVENT", eventParams) }
                Log.i(ADSLOG, "onAdImpression: Rewarded")
            }

            override fun onAdShowedFullScreenContent() {
                isAdShowing = true
                stopLoadingdialog()
                mRewardAd = null
                if (clickCounts >= counter) {
                    clickCounts = 1
                }
            }
        }

        if (mRewardAd != null) {
            isAdShowing = true
            mRewardAd!!.show(activity!!) { callBack.onRewardGranted() }
        }
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
