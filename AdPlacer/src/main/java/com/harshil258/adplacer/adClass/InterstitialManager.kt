package com.harshil258.adplacer.adClass

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.text.TextUtils
import android.util.Log
import android.view.ViewGroup
import com.harshil258.adplacer.interfaces.InterAdCallBack
import com.harshil258.adplacer.utils.GlobalUtils
import com.harshil258.adplacer.utils.GlobalUtils.Companion.checkMultipleClick
import com.harshil258.adplacer.utils.SharedPrefConfig.Companion.sharedPrefConfig
import com.harshil258.adplacer.utils.extentions.isInterstitialEmpty
import com.harshil258.adplacer.R
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.harshil258.adplacer.app.AdPlacerApplication
import com.harshil258.adplacer.utils.Constants.isAppInForeground
import com.harshil258.adplacer.utils.Logger

class InterstitialManager {
    var TAG: String = "Interstitial"


    fun isCounterSatisfy(activity: Activity): Boolean {
        val appDetail = sharedPrefConfig.appDetails
        try {
            if (!isInterstitialEmpty()) {
                val counter = appDetail.interstitialAdFrequency.takeIf {
                    it.isNotEmpty() && TextUtils.isDigitsOnly(it)
                }?.toInt() ?: 3

                if (clickCounts == counter - 1) {
                    preloadInterstitialAd(activity)
                }
                if (clickCounts >= counter) {
                    return true
                } else {
                    clickCounts++
                    return false
                }
            } else {
                clickCounts = 3
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return false
    }


    fun loadAndShowInter(activity: Activity, callBack: InterAdCallBack) {
        isAppInForeground = true
        if (checkMultipleClick(750)) {
            return
        }
        if (com.harshil258.adplacer.adClass.AppOpenManager.isAdShowing) {
            return
        }
        if (isAdLoading) {
            showLoadingDialog(activity, callBack)
            startTimerForContinueFlow(activity, 5000, callBack)


            return
        }
        if (isAdShowing) {
            stopLoadingdialog()
            return
        }
        if (mInterstitialAd != null) {
            isAdLoading = false
            if (!com.harshil258.adplacer.adClass.AppOpenManager.isAdShowing) {
                if (isCounterSatisfy(activity)) {
                    startTimerForContinueFlow(activity, 5000, callBack)
                } else {
                    if (isAppInForeground) {
                        callBack.onContinueFlow()
                    }
                }
            }
            return
        }
        if (isInterstitialEmpty()) {
            stopLoadingdialog()
            callBack.onContinueFlow()
            return
        }

        if (!GlobalUtils().isNetworkAvailable(activity.applicationContext)) {
            stopLoadingdialog()
            callBack.onContinueFlow()
            return
        }
        if (!com.harshil258.adplacer.adClass.AppOpenManager.isAdShowing) {
            if (isCounterSatisfy(activity)) {
                loadInterAd(activity, callBack)
                showLoadingDialog(activity, callBack)
                startTimerForContinueFlow(activity, 5000, callBack)
            } else {
                if (isAppInForeground) {
                    callBack.onContinueFlow()
                }
            }
        }
    }

    fun loadAndShowInterAfterReward(
        activity: Activity,
        dialog: Dialog?,
        callBack: InterAdCallBack
    ) {
        isAppInForeground = true

        if (com.harshil258.adplacer.adClass.AppOpenManager.isAdShowing) {
            return
        }
        if (isAdLoading) {
            if (dialog == null) {
                showLoadingDialog(activity, callBack)
            }
            startTimerForContinueFlow(activity, 5000, callBack)


            return
        }
        if (isAdShowing) {
            stopLoadingdialog()
            return
        }
        if (mInterstitialAd != null) {
            isAdLoading = false
            if (!com.harshil258.adplacer.adClass.AppOpenManager.isAdShowing) {
                if (isCounterSatisfy(activity)) {
                    startTimerForContinueFlow(activity, 5000, callBack)
                } else {
                    if (isAppInForeground) {
                        callBack.onContinueFlow()
                    }
                }
            }
            return
        }
        if (isInterstitialEmpty()) {
            stopLoadingdialog()
            callBack.onContinueFlow()
            return
        }

        if (!GlobalUtils().isNetworkAvailable(activity.applicationContext)) {
            stopLoadingdialog()
            callBack.onContinueFlow()
            return
        }
        if (!com.harshil258.adplacer.adClass.AppOpenManager.isAdShowing) {
            if (isCounterSatisfy(activity)) {
                loadInterAd(activity, callBack)
                if (dialog == null) {
                    showLoadingDialog(activity, callBack)
                }
                startTimerForContinueFlow(activity, 5000, callBack)
            } else {
                if (isAppInForeground) {
                    callBack.onContinueFlow()
                }
            }
        }
    }

    private fun loadInterAd(activity: Activity, callBack: InterAdCallBack?) {
        if (isAdLoading || isAdShowing) {
            return
        }

        val adRequest = AdRequest.Builder().build()
        val AD_UNIT: String = sharedPrefConfig.appDetails.admobInterstitialAd
        Logger.e("ADIDSSSS", "INTERSTITIAL   ${AD_UNIT}")

        isAdLoading = true

        InterstitialAd.load(activity, AD_UNIT, adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    isAdLoading = false
                    mInterstitialAd = interstitialAd
                    Logger.e(TAG, "onAdLoaded: AAAA")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    isAdLoading = false
                    mInterstitialAd = null
                    stopLoadingdialog()
                    Logger.e(TAG, "onAdFailedToLoad: loadAdError AAAAA ${loadAdError}")

                    if (callBack != null) {
                        if (isAppInForeground) {
                            callBack.onContinueFlow()
                        }
                    }
                }
            })
    }

    fun preloadInterstitialAd(activity: Activity) {
        if (isAdLoading || isAdShowing) {
            stopLoadingdialog()
            return
        }
        if (mInterstitialAd != null) {
            isAdLoading = false
            return
        }
        if (isInterstitialEmpty()) {
            return
        }

        if (!GlobalUtils().isNetworkAvailable(activity.applicationContext)) {
            return
        }

        loadInterAd(activity, null)
    }

    fun stopLoadingdialog() {
        try {
            if (dialog != null && dialog!!.isShowing) {
                dialog!!.dismiss()
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
        callBack: InterAdCallBack
    ) {
        timer = object : com.harshil258.adplacer.app.CountDownTimer(duration, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                if (com.harshil258.adplacer.adClass.AppOpenManager.isAdShowing || !isAppInForeground) {
                    timer!!.pause()
                    stopLoadingdialog()
                } else if (isAppInForeground && mInterstitialAd != null && !isAdLoading) {
                    if (!com.harshil258.adplacer.adClass.AppOpenManager.isAdShowing) {
                        showInterAd(activity, callBack)
                    }
                    timer!!.pause()
                    stopLoadingdialog()
                }
            }

            override fun onFinish() {
                stopLoadingdialog()
                if (!com.harshil258.adplacer.adClass.AppOpenManager.isAdShowing) {
                    callBack.onContinueFlow()
                }
            }
        }
        timer?.start()
    }

    var dialog: Dialog? = null

    private fun showLoadingDialog(activity: Activity, callBack: InterAdCallBack) {
        try {
            if (dialog != null && dialog!!.isShowing) {
                dialog!!.dismiss()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        dialog = Dialog(activity)
        dialog!!.setContentView(R.layout.dialog_ad_loading)
        dialog!!.window!!.setLayout(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog!!.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog!!.setCancelable(false)
        try {
            if (!activity.isFinishing) {
                dialog!!.show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun showInterAd(activity: Activity?, callBack: InterAdCallBack) {
        Logger.e(TAG, "showInterAd: 1")
        if (isAdLoading) {
            stopLoadingdialog()
            callBack.onContinueFlow()
            return
        }
        Logger.e(TAG, "showInterAd: 2")
        if (isAdShowing) {
            stopLoadingdialog()
            return
        }
        Logger.e(TAG, "showInterAd: 3")

        if (mInterstitialAd == null) {
            stopLoadingdialog()
            callBack.onContinueFlow()
            return
        }

        Logger.e(TAG, "showInterAd: 4")
        mInterstitialAd!!.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdClicked() {
            }

            override fun onAdDismissedFullScreenContent() {
                isAdShowing = false
                mInterstitialAd = null
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
                mInterstitialAd = null
                if (isAppInForeground) {
                    callBack.onContinueFlow()
                }
            }

            override fun onAdImpression() {
                Logger.e(TAG, "showInterAd: 5 impression")
            }

            override fun onAdShowedFullScreenContent() {
                Logger.e(TAG, "showInterAd: 6")
                isAdShowing = true
                stopLoadingdialog()
                mInterstitialAd = null
                Logger.e(TAG, "showInterAd: 7  clickCounts  $clickCounts")
                if (clickCounts >= counter) {
                    clickCounts = 1
                }

            }
        }

        if (mInterstitialAd != null) {
            isAdShowing = true
            mInterstitialAd!!.show(activity!!)
        }
    }

    companion object {
        var mInterstitialAd: InterstitialAd? = null

        var isAdShowing: Boolean = false
        private var isAdLoading = false
        var counter: Int = 3
        var clickCounts: Int = 3

        var timer: com.harshil258.adplacer.app.CountDownTimer? = null
    }
}
