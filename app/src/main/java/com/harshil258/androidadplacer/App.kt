package com.harshil258.androidadplacer


import android.app.Activity
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.harshil258.adplacer.interfaces.AdCallback
import com.harshil258.adplacer.adViews.BannerViewMedium
import com.harshil258.adplacer.adViews.NativeBigView
import com.harshil258.adplacer.interfaces.DialogCallBack
import com.harshil258.adplacer.interfaces.MessagingCallback
import com.harshil258.adplacer.models.ADTYPE
import com.harshil258.adplacer.models.TYPE_OF_RESPONSE
import com.harshil258.adplacer.utils.Constants.showLogs
import com.harshil258.adplacer.utils.GlobalUtils
import com.harshil258.adplacer.utils.GlobalUtils.Companion.checkMultipleClick
import com.harshil258.adplacer.utils.Logger

import com.harshil258.adplacer.R
import com.harshil258.adplacer.adClass.AppOpenAdManager
import com.harshil258.adplacer.adClass.InterstitialManager
import com.harshil258.adplacer.app.AdPlacerApplication
import com.harshil258.adplacer.utils.Constants
import com.harshil258.adplacer.utils.Constants.isAppInForeground
import com.harshil258.adplacer.utils.Constants.isSplashScreenRunning
import com.harshil258.adplacer.utils.Constants.shouldProceedWithoutInternet
import com.harshil258.adplacer.utils.Constants.testDeviceIds
import com.harshil258.adplacer.utils.DialogUtil.createSimpleDialog
import com.harshil258.adplacer.utils.sharedpreference.SecureStorageManager.Companion.sharedPrefConfig


//@Obfuscate
class App : Application(), LifecycleObserver, ActivityLifecycleCallbacks, MessagingCallback {


    var adPlacerApplication: AdPlacerApplication? = null



    override fun onCreate() {
        super.onCreate()
        context = applicationContext
        showLogs = true
        testDeviceIds.add("07273c52-e840-4a96-8996-b34b55560af5")


        FirebaseMessaging.getInstance().subscribeToTopic("testingnotifications")
        FirebaseMessaging.getInstance().subscribeToTopic("IPO_INDIA_COMMONNOTIFICATIONS")


        Logger.i("TAGCOMMON", "Application   onCreate:  FIRST LOG")
        registerActivityLifecycleCallbacks(this)

        applicationInstance = this
        adPlacerApplication = AdPlacerApplication(this)
        adPlacerApplication?.registerLifecycleObserver(this)
        adPlacerApplication?.registerMessagingListener(this)
        StrictMode.setThreadPolicy(ThreadPolicy.Builder().detectAll().build())


    }


    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onStart() {
        if (!isSplashScreenRunning) {
            adPlacerApplication?.showAppOpenAd()
        } else if (isSplashScreenRunning && adPlacerApplication?.appOpenAdManager?.isAdAvailable == true && !AppOpenAdManager.isAdShowing
        ) {
            adPlacerApplication?.showAppOpenAd()
        } else {
        }
    }


    override fun onActivityPreCreated(activity: Activity, savedInstanceState: Bundle?) {
        super.onActivityPreCreated(activity, savedInstanceState)
        Constants.currentActivity = activity
        if (activity is LauncherActivity) {
            isSplashScreenRunning = true
        }
    }


    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        // activity.getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        Constants.currentActivity = activity
        Logger.i(
            "TAGCOMMON",
            "onActivityCreated  Constants.currentActivity  ${Constants.currentActivity?.localClassName}"
        )

        if (activity is LauncherActivity) {
            isSplashScreenRunning = true
            if (!GlobalUtils().isNetworkAvailable(applicationContext)) {
                if (shouldProceedWithoutInternet) {
                    adPlacerApplication?.startContinueFlowTimer(10)
                    return
                }
                adPlacerApplication?.initializeClickCounts()
                adPlacerApplication?.showInternetDialog()
                return
            } else {
                Logger.i("srherhse", "apiResponse called")
                var type = TYPE_OF_RESPONSE.API
                type.value = "APPDETAILS12"
                adPlacerApplication?.fetchApiResponse(type)
                adPlacerApplication?.startContinueFlowTimer(12000)
            }
        }
    }

    override fun showNetworkDialog() {
        createSimpleDialog(
            Constants.currentActivity,
            title = "No Internet",
            description = "No internet connection!\nCheck your internet connection",
            negativeButtonText = "Exit",
            positiveButtonText = "Retry",
            dialogCallback = object : DialogCallBack {
                override fun onPositiveClicked(dialog: DialogInterface) {
                    if (GlobalUtils().isNetworkAvailable(Constants.currentActivity!!.applicationContext)) {
                        dialog.cancel()
                        showSplashLoader()
                        adPlacerApplication?.fetchApiResponse(TYPE_OF_RESPONSE.API)
                        adPlacerApplication?.startContinueFlowTimer(6000)
                    } else {
                        if (!checkMultipleClick(2000)) {
                            Toast.makeText(
                                Constants.currentActivity!!.applicationContext,
                                "Please connect to internet!",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }

                override fun onNegativeClicked(dialog: DialogInterface) {
                    Constants.currentActivity!!.finishAffinity()
                    dialog.cancel()
                }

                override fun onDialogCancelled() {

                }

                override fun onDialogDismissed() {
                }
            },
            isCancelable = false
        )
    }

    override fun onFirebaseResponseReceived(firebaseConfig: FirebaseRemoteConfig) {

    }

    override fun startScreenTransitionTimer() {
    }

    override fun onApiResponseSaved() {
    }

    override fun exitApplication() {
        Constants.currentActivity?.finishAffinity()
    }

    override fun onActivityStarted(activity: Activity) {
        Constants.currentActivity = activity
    }

    override fun onActivityResumed(activity: Activity) {
        Constants.currentActivity = activity
    }


    override fun onActivityPaused(activity: Activity) {
        isAppInForeground = false
        try {
            if (InterstitialManager.flowTimer != null) {
                InterstitialManager.flowTimer?.pause()
                InterstitialManager.flowTimer?.cancel()
                Logger.d("Interstitial", "timer cancel 4")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        adPlacerApplication?.interstitialAdManager?.stopLoadingDialog()
    }

    override fun onActivityStopped(activity: Activity) {
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
    }

    override fun onActivityDestroyed(activity: Activity) {
    }


    override fun hideSplashLoader() {
        Handler(Looper.getMainLooper()).postDelayed({
            if (LauncherActivity.instance != null) {
                try {
//                    LauncherActivity.instance.runOnUiThread(Runnable {
//                        LauncherActivity.instance.findViewById(
//                            com.harshil258.adplacer.R.id.animationLoading
//                        ).setVisibility(View.GONE)
//                    })
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }, 1000)
    }

    override fun showSplashLoader() {
        if (LauncherActivity.instance != null) {
            try {
//                LauncherActivity.instance.runOnUiThread(Runnable {
//                    LauncherActivity.instance.findViewById(
//                        R.id.animationLoading
//                    ).setVisibility(View.VISIBLE)
//                })
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override val isExitActivity: Boolean
        get() = false


    override fun openStartActivity() {
//        val intent = Intent(Constants.currentActivity, StartActivity::class.java)
//        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//        startActivity(intent)
//        Constants.currentActivity!!.finish()
    }

    override fun openHomeActivity() {
        Logger.e("TAGCOMMON", "MainActivity")

        Constants.currentActivity!!.finish()
        val intent = Intent(Constants.currentActivity, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)




    }




    override fun openHowToUseActivity() {
//        val intent = Intent(Constants.currentActivity, MainActivity::class.java)
//        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//        startActivity(intent)
//        Constants.currentActivity!!.finish()
    }


    override fun openExtraStartActivity() {
//        val intent = Intent(Constants.currentActivity, StartActivity::class.java)
//        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//        startActivity(intent)
//        Constants.currentActivity!!.finish()
    }

    fun showExitActivityOrDialog() {
        if (sharedPrefConfig.appDetails.whichScreenToGo != "" && TextUtils.isDigitsOnly(
                sharedPrefConfig.appDetails.whichScreenToGo
            )
        ) {
            var count = 0
            try {
                count = sharedPrefConfig.appDetails
                    .whichScreenToGo.toInt()
            } catch (e: Exception) {
                count = 0
                e.printStackTrace()
            }
            if (count >= 2) {
                openHomeActivity()
//                val intent = Intent(Constants.currentActivity, ExitActivity::class.java)
//                startActivity(intent)
            } else if (count >= 1) {
//                val intent = Intent(Constants.currentActivity, ExitActivity::class.java)
//                startActivity(intent)
            } else {
                showExitDialog(ADTYPE.BANNER)
                //                messagingCallback.openHomeActivity();
            }
        } else {
            showExitDialog(ADTYPE.BANNER)
            //          messagingCallback.openHomeActivity();
        }
    }

    var dialogExit: Dialog? = null

    fun showExitDialog(type: ADTYPE) {
        if (dialogExit?.isShowing == true) return

        Constants.currentActivity?.let { activity ->
            dialogExit = Dialog(activity).apply {
                setContentView(R.layout.layout_dialog_exit)
                window?.setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                window?.setBackgroundDrawableResource(android.R.color.transparent)
                setCancelable(true)

                val txtPositive = findViewById<TextView>(R.id.dialogPositiveButton)
                val txtNegative = findViewById<TextView>(R.id.dialogNegativeButton)
                val mediumBanner = findViewById<BannerViewMedium>(R.id.mediumBanner)
                val myAdViewBig = findViewById<NativeBigView>(R.id.myAdViewBig)

                txtPositive.setOnClickListener {
                    cancel()
                    activity.finishAffinity()
                }
                txtNegative.setOnClickListener {
                    cancel()
                }

                if (!activity.isFinishing) {
                    show()
                    when (type) {
                        ADTYPE.NATIVE -> {
                            mediumBanner.visibility = View.GONE
                            myAdViewBig.visibility = View.VISIBLE
                            adPlacerApplication?.nativeAdManager?.callBigOnly(
                                activity,
                                myAdViewBig,
                                object :
                                    AdCallback {
                                    override fun onAdDisplayed(isDisplayed: Boolean) {
                                    }
                                })
                        }

                        else -> {
                            mediumBanner.visibility = View.VISIBLE
                            myAdViewBig.visibility = View.GONE
                            mediumBanner.loadAd(activity)
                        }
                    }
                }
            }
        } ?: run {
            Logger.e("showExitDialog", "Running activity is null, cannot show dialog.")
        }
    }


    companion object {
        var context: Context? = null
        var applicationInstance: App? = null
    }
}