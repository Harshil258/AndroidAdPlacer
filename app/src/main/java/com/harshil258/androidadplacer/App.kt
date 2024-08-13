package com.harshil258.androidadplacer


import android.app.Activity
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
import androidx.core.content.ContextCompat
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
import com.harshil258.adplacer.utils.Constants.runningActivity
import com.harshil258.adplacer.utils.Constants.showLogs
import com.harshil258.adplacer.utils.GlobalUtils
import com.harshil258.adplacer.utils.GlobalUtils.Companion.checkMultipleClick
import com.harshil258.adplacer.utils.Logger
import com.harshil258.adplacer.utils.SharedPrefConfig.Companion.sharedPrefConfig
import com.harshil258.adplacer.R
import com.harshil258.adplacer.adClass.InterstitialManager
import com.harshil258.adplacer.app.AdPlacerApplication
import com.harshil258.adplacer.utils.Constants.isAppInForeground
import com.harshil258.adplacer.utils.Constants.isSplashRunning
import com.harshil258.adplacer.utils.Constants.shouldGoWithoutInternet
import com.harshil258.adplacer.utils.Constants.testDeviceIds
import com.harshil258.adplacer.utils.DialogUtil.createSimpleDialog


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
        adPlacerApplication?.processLifecycleRegister(this)
        adPlacerApplication?.registerMessagingCallback(this)

        StrictMode.setThreadPolicy(ThreadPolicy.Builder().detectAll().build())


    }


    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onStart() {
        if (!isSplashRunning) {
            adPlacerApplication?.showAppOpenAd()
        } else if (isSplashRunning && adPlacerApplication?.appOpenManager?.isAdAvailable == true && !com.harshil258.adplacer.adClass.AppOpenManager.isAdShowing
        ) {
            adPlacerApplication?.showAppOpenAd()
        } else {
        }
    }


    override fun onActivityPreCreated(activity: Activity, savedInstanceState: Bundle?) {
        super.onActivityPreCreated(activity, savedInstanceState)
        runningActivity = activity
        if (activity is LauncherActivity) {
            isSplashRunning = true
        }
    }

    private fun isAllPermGranted(context: Context, permArr: Array<String>): Boolean {
        var allgranted = true
        for (it in permArr) {
            if (ContextCompat.checkSelfPermission(context, it)
                != PackageManager.PERMISSION_GRANTED
            ) {
                allgranted = false
            }
        }
        return allgranted
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        // activity.getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        runningActivity = activity
        Logger.i(
            "TAGCOMMON",
            "onActivityCreated  runningActivity  ${runningActivity?.localClassName}"
        )

        if (activity is LauncherActivity) {
            isSplashRunning = true
            if (!GlobalUtils().isNetworkAvailable(applicationContext)) {
                if (shouldGoWithoutInternet) {
                    adPlacerApplication?.startTimerForContinueFlow(10)
                    return
                }
                adPlacerApplication?.initClickCounts()
                adPlacerApplication?.showInternetDialog()
                return
            } else {
                Logger.i("srherhse", "apiResponse called")
                var type = TYPE_OF_RESPONSE.API
                type.value = "APPDETAILS12"
                adPlacerApplication?.fetchApiResponse(type)
                adPlacerApplication?.startTimerForContinueFlow(12000)
            }
        }
    }

    override fun showNetworkDialog() {
        createSimpleDialog(
            runningActivity,
            title = "No Internet",
            description = "No internet connection!\nCheck your internet connection",
            negativeButtonText = "Exit",
            positiveButtonText = "Retry",
            dialogCallback = object : DialogCallBack {
                override fun onPositiveClicked(dialog: Dialog) {
                    if (GlobalUtils().isNetworkAvailable(runningActivity!!.applicationContext)) {
                        dialog.cancel()
                        showSplashLoader()
                        adPlacerApplication?.fetchApiResponse(TYPE_OF_RESPONSE.API)
                        adPlacerApplication?.startTimerForContinueFlow(6000)
                    } else {
                        if (!checkMultipleClick(2000)) {
                            Toast.makeText(
                                runningActivity!!.applicationContext,
                                "Please connect to internet!",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }

                override fun onNegativeClicked(dialog: Dialog) {
                    runningActivity!!.finishAffinity()
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

    override fun exitTheApplication() {
        runningActivity?.finish()
    }

    override fun onActivityStarted(activity: Activity) {
        runningActivity = activity
    }

    override fun onActivityResumed(activity: Activity) {
        runningActivity = activity
    }


    override fun onActivityPaused(activity: Activity) {
        isAppInForeground = false
        try {
            if (InterstitialManager.timer != null) {
                InterstitialManager.timer?.pause()
                InterstitialManager.timer?.cancel()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        adPlacerApplication?.interstitialManager?.stopLoadingdialog()
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
//        val intent = Intent(runningActivity, StartActivity::class.java)
//        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//        startActivity(intent)
//        runningActivity!!.finish()
    }

    override fun openHomeActivity() {
        Logger.e("TAGCOMMON", "MainActivity")

//        runningActivity!!.finish()
        val intent = Intent(runningActivity, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)



    }

    override fun gotFirebaseResponse(firebaseRemoteConfig: FirebaseRemoteConfig) {
//        sharedPrefConfig.appDetails.admobNativeAd = "/6499/example/native-video"


    }

    override fun startingTimerToChangeScreen() {
//        sharedPrefConfig.appDetails = sharedPrefConfig.appDetails.copy(adStatus = "OFF")

    }

    override fun savingApiResponse() {

    }

    override fun openHowToUseActivity() {
//        val intent = Intent(runningActivity, MainActivity::class.java)
//        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//        startActivity(intent)
//        runningActivity!!.finish()
    }


    override fun openExtraStartActivity() {
//        val intent = Intent(runningActivity, StartActivity::class.java)
//        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//        startActivity(intent)
//        runningActivity!!.finish()
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
//                val intent = Intent(runningActivity, ExitActivity::class.java)
//                startActivity(intent)
            } else if (count >= 1) {
//                val intent = Intent(runningActivity, ExitActivity::class.java)
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

        runningActivity?.let { activity ->
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
                                    override fun adDisplayedCallback(displayed: Boolean) {

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