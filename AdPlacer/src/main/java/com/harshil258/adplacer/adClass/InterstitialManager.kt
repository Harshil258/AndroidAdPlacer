package com.harshil258.adplacer.adClass

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.text.TextUtils
import android.util.Log
import android.view.ViewGroup
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.harshil258.adplacer.R
import com.harshil258.adplacer.interfaces.InterAdCallBack
import com.harshil258.adplacer.utils.Constants.isAppInForeground
import com.harshil258.adplacer.utils.GlobalUtils
import com.harshil258.adplacer.utils.GlobalUtils.Companion.checkMultipleClick
import com.harshil258.adplacer.utils.Logger
import com.harshil258.adplacer.utils.Logger.ADSLOG
import com.harshil258.adplacer.utils.commonFunctions.logCustomEvent
import com.harshil258.adplacer.utils.extentions.isInterstitialEmpty
import com.harshil258.adplacer.utils.sharedpreference.SecureStorageManager.Companion.sharedPrefConfig

class InterstitialManager {
    var TAG: String = "Interstitial"

    private fun isCounterSatisfy(activity: Activity): Boolean {
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

        if (checkMultipleClick(750)) return
        if (AppOpenManager.isAdShowing) return
        if (isAdLoading) {
            showLoadingDialog(activity)
            startTimerForContinueFlow(activity, 5000, callBack)
            return
        }
        if (isAdShowing) {
            stopLoadingdialog()
            return
        }
        if (mInterstitialAd != null) {
            isAdLoading = false
            if (isCounterSatisfy(activity)) {
                startTimerForContinueFlow(activity, 5000, callBack)
            } else if (isAppInForeground) {
                callBack.onContinueFlow()
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
        if (!AppOpenManager.isAdShowing && isCounterSatisfy(activity)) {
            loadInterAd(activity, callBack)
            showLoadingDialog(activity)
            startTimerForContinueFlow(activity, 5000, callBack)
        } else if (isAppInForeground) {
            callBack.onContinueFlow()
        }
    }

    fun loadAndShowInterAfterReward(
        activity: Activity,
        dialog: Dialog?,
        callBack: InterAdCallBack
    ) {
        isAppInForeground = true

        if (AppOpenManager.isAdShowing) {
            return
        }
        if (isAdLoading) {
            if (dialog == null) {
                showLoadingDialog(activity)
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
            if (isCounterSatisfy(activity)) {
                startTimerForContinueFlow(activity, 5000, callBack)
            } else {
                if (isAppInForeground) {
                    callBack.onContinueFlow()
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
        if (!AppOpenManager.isAdShowing) {
            if (isCounterSatisfy(activity)) {
                loadInterAd(activity, callBack)
                if (dialog == null) {
                    showLoadingDialog(activity)
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
                    Log.i(ADSLOG, "onAdLoaded: Interstitial")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    isAdLoading = false
                    mInterstitialAd = null
                    stopLoadingdialog()
                    Logger.e(TAG, "onAdFailedToLoad: loadAdError inter ${loadAdError}")

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
                Logger.d(TAG, "timer cancel 1")

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
        timer = object : com.harshil258.adplacer.app.CountDownTimer(duration, 500L) {
            override fun onTick(millisUntilFinished: Long) {
                if (AppOpenManager.isAdShowing || !isAppInForeground) {
                    timer!!.pause()
                    stopLoadingdialog()
                } else if (mInterstitialAd != null && !isAdLoading) {
                    showInterAd(activity, callBack)
                    timer!!.pause()
                    stopLoadingdialog()
                }

            }

            override fun onFinish() {
                stopLoadingdialog()
                Logger.d(TAG, "loadAndShowInter: onFinish   1111111")
                if (!AppOpenManager.isAdShowing) {
                    Logger.d(TAG, "loadAndShowInter: onFinish")
                    callBack.onContinueFlow()
                }
            }
        }
        Logger.d(TAG, "loadAndShowInter: TIMER STARTED")
        timer!!.start()
    }

    private var dialog: Dialog? = null

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
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
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

    fun showInterAd(activity: Activity?, callBack: InterAdCallBack) {
        if (isAdLoading) {
            stopLoadingdialog()
            callBack.onContinueFlow()
            return
        }
        if (isAdShowing) {
            stopLoadingdialog()
            return
        }

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
                    Logger.d(TAG, "timer cancel 2")
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
                Log.i(ADSLOG, "onAdImpression: Intersitital")
                val eventParams = mapOf("ADIMPRESSION" to "INTERSTITIAL")
                activity?.let { logCustomEvent(it, "ADS_EVENT", eventParams) }
            }

            override fun onAdShowedFullScreenContent() {
                isAdShowing = true
                stopLoadingdialog()
                mInterstitialAd = null
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
