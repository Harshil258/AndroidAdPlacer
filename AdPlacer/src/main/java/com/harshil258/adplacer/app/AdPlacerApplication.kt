package com.harshil258.adplacer.app

import android.app.Activity
import android.app.Application
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.ProcessLifecycleOwner
import com.harshil258.adplacer.interfaces.AdCallback
import com.harshil258.adplacer.adClass.InterstitialManager
import com.harshil258.adplacer.adClass.NativeAdManager
import com.harshil258.adplacer.adClass.RewardAdManager
import com.harshil258.adplacer.api.AdApiClient
import com.harshil258.adplacer.interfaces.ApiInterface
import com.harshil258.adplacer.interfaces.MessagingCallback
import com.harshil258.adplacer.models.ApiResponse
import com.harshil258.adplacer.models.SCREENS
import com.harshil258.adplacer.models.TYPE_OF_RESPONSE
import com.harshil258.adplacer.utils.Constants.AuthorizationADS
import com.harshil258.adplacer.utils.Constants.LIBRARY_PACKAGE_NAME
import com.harshil258.adplacer.utils.Constants.runningActivity
import com.harshil258.adplacer.utils.GlobalUtils
import com.harshil258.adplacer.utils.HomeButtonReceiver
import com.harshil258.adplacer.utils.STATUS
import com.harshil258.adplacer.utils.SharedPrefConfig.Companion.sharedPrefConfig
import com.harshil258.adplacer.utils.SharedPrefConfig.Companion.sharedPrefConfigInstance
import com.harshil258.adplacer.utils.extentions.isAdStatusOn
import com.harshil258.adplacer.utils.extentions.isAppOpenEmpty
import com.harshil258.adplacer.utils.fromJson
import com.google.android.gms.ads.MobileAds
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.google.gson.Gson
import com.harshil258.adplacer.interfaces.DialogCallBack
import com.harshil258.adplacer.utils.Logger
import com.harshil258.adplacer.R
import com.onesignal.OneSignal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AdPlacerApplication(instance: Application) {


    companion object {
        @JvmField
        var isAppInForeground: Boolean = false
        @JvmField
        var isSplashRunning: Boolean = false
        lateinit var adPlacerApplication: AdPlacerApplication
        var shouldGoWithoutInternet = false
    }


    var appOpenManager: com.harshil258.adplacer.adClass.AppOpenManager


    @JvmField
    val interstitialManager: InterstitialManager
    val rewardClass: RewardAdManager

    @JvmField
    val nativeAdManager: NativeAdManager

    @JvmField
    var messagingCallback: MessagingCallback? = null
    var homeButtonReceiver: HomeButtonReceiver


    init {
        Logger.i("TAGCOMMON", "AdPlacerApplication  FIRST LOG")

        adPlacerApplication = this
        sharedPrefConfigInstance(instance)
    }

    fun processLifecycleRegister(observer: LifecycleObserver?) {
        ProcessLifecycleOwner.get().lifecycle.addObserver(observer!!)
    }


    fun showAppOpen() {
        if (InterstitialManager.isAdShowing) {
            return
        }
        if (RewardAdManager.isAdShowing) {
            return
        }
        if (com.harshil258.adplacer.adClass.AppOpenManager.shouldStopAppOpen) {
            com.harshil258.adplacer.adClass.AppOpenManager.shouldStopAppOpen = false
            return
        }
        if (!isAppOpenEmpty()) {
            runningActivity?.apply {
                appOpenManager.showAppOpen(this, object : AdCallback {
                    override fun adDisplayedCallback(displayed: Boolean) {
                        if (isSplashRunning) {
                            isSplashRunning = false
                            Handler(Looper.getMainLooper()).postDelayed({ continueAppFlow() }, 750)
                        }
                    }
                })
            }
        }
    }


    fun continueAppFlow() {
        Logger.e("TAGCOMMON", "continueAppFlow:")
        handler.removeCallbacksAndMessages(null)

        val activity = runningActivity ?: return

        val appDetails = sharedPrefConfig.appDetails
        val whichScreenToGo = appDetails.whichScreenToGo

        Logger.e("TAGCOMMON", "whichScreenToGo: $whichScreenToGo")
        Logger.e("TAGCOMMON", "howtousestart: ${appDetails.howtousestart}")
        Logger.e("TAGCOMMON", "isHowToUseShowDone: ${sharedPrefConfig.isHowToUseShowDone}")

        if (appDetails.howtousestart == STATUS.ON.name && !sharedPrefConfig.isHowToUseShowDone) {
            Logger.e("TAGCOMMON", "Opening HowToUseActivity.")
            messagingCallback?.openHowToUseActivity()
        } else if (whichScreenToGo.isNotEmpty()) {
            Logger.e("TAGCOMMON", "whichScreenToGo is not empty. Value: $whichScreenToGo")

            when (whichScreenToGo) {
                SCREENS.EXTRA_START.name -> {
                    Logger.e("TAGCOMMON", "Opening ExtraaStartActivity.")
                    messagingCallback?.openExtraaStartActivity()
                }

                SCREENS.START.name -> {
                    Logger.e("TAGCOMMON", "Opening StartActivity.")
                    messagingCallback?.openStartActivity()
                }

                else -> {
                    Logger.e("TAGCOMMON", "Opening HomeActivity.")
                    messagingCallback?.openHomeActivity()
                }
            }
        } else {
            Logger.e(
                "TAGCOMMON",
                "whichScreenToGo is empty or not digits only. Opening HomeActivity."
            )
            messagingCallback?.openHomeActivity()
        }

        activity.finish()
        Logger.e("TAGCOMMON", "Running activity finished.")
        isSplashRunning = false
        Logger.e("TAGCOMMON", "Splash screen is not running.")
    }

    fun initializeAndFetchFirebaseConfig(
        onSuccess: (FirebaseRemoteConfig) -> Unit,
        onFailure: () -> Unit,
        retries: Int = 3,
        retryDelay: Long = 2000 // delay in milliseconds
    ) {
        val firebaseRemoteConfig: FirebaseRemoteConfig = FirebaseRemoteConfig.getInstance()

        // Set default values for Remote Config parameters
        val configDefaults: Map<String, Any> = mapOf(
            "temp_response" to "default value" // Replace with your default value
        )
        firebaseRemoteConfig.setDefaultsAsync(configDefaults)

        val configSettings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(2) // Set to 2 seconds
            .build()
        firebaseRemoteConfig.setConfigSettingsAsync(configSettings)

        // Fetch and activate the remote config values
        firebaseRemoteConfig.fetchAndActivate()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Call the success callback
                    onSuccess(firebaseRemoteConfig)
                } else {
                    // Handle the error
                    Logger.e("LauncherActivity", "Failed to fetch config")

                    if (retries > 0) {
                        // Retry after a delay
                        Handler(Looper.getMainLooper()).postDelayed({
                            initializeAndFetchFirebaseConfig(
                                onSuccess,
                                onFailure,
                                retries - 1,
                                retryDelay
                            )
                        }, retryDelay)
                    } else {
                        // Call the failure callback if no retries are left
                        onFailure()
                    }
                }
            }
    }


    fun fetchApiResponse(whichResponse: TYPE_OF_RESPONSE) {

        initializeAndFetchFirebaseConfig(onSuccess = { firebaseConfig ->
            messagingCallback?.gotFirebaseResponse(firebaseConfig)


            if (whichResponse == TYPE_OF_RESPONSE.GOOGLE) {
                var response = firebaseConfig.getString(whichResponse.value)
                var apiResponse: ApiResponse = Gson().fromJson(response)
                Log.e("TAGCOMMON", "onResponse: GOOGLE  ")
                onSuccessfulResponse(apiResponse)
            } else {
                LIBRARY_PACKAGE_NAME = firebaseConfig.getString("LIBRARY_PACKAGE_NAME")
                AuthorizationADS = firebaseConfig.getString("AuthorizationADS")
                AdApiClient.BASE_URL_API = firebaseConfig.getString("BaseUrl")


                val jsonBody = JSONObject().apply {
                    put("appid", LIBRARY_PACKAGE_NAME)
                    put("secretKey", AuthorizationADS)
                }

                val requestBody = jsonBody.toString()
                    .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

                val call = AdApiClient().client.create(ApiInterface::class.java).getAll(requestBody)

                call?.enqueue(object : Callback<ApiResponse?> {
                    override fun onResponse(
                        call: Call<ApiResponse?>,
                        response: Response<ApiResponse?>
                    ) {
                        if (response.isSuccessful && response.body() != null) {
                            Log.e("TAGCOMMON", "onResponse: API  ")
                            onSuccessfulResponse(response.body()!!)
                        }
                    }

                    override fun onFailure(call: Call<ApiResponse?>, t: Throwable) {
                        onFailureResponse(t)
                    }
                })
            }


        }, onFailure = {
            messagingCallback?.exitTheApplication()
        },
            retries = 3,
            retryDelay = 2000
        )


    }

    private fun onFailureResponse(t: Throwable) {
        Logger.i("TAGCOMMON", "onFailure: ${t.message}")
        startTimerForAdStatus(2000)
        checkAndShowAdIfAvailable()
    }

    private fun onSuccessfulResponse(response: ApiResponse) {
        Logger.i(
            "TAGCOMMON",
            "got response: ${response.appDetails.appName}  adStatus :   ${response.appDetails.adStatus}"
        )
        try {
            runningActivity?.let { OneSignal.initWithContext(it, response.appDetails.oneSignalAppId) }


            val currentVersion = getCurrentAppVersion(runningActivity)
            val shouldForceUpdate = response.appDetails.forceUpdateVersions.contains(currentVersion)
            val shouldUpdate = response.appDetails.updateRequiredVersions.contains(currentVersion)

            if (shouldForceUpdate || shouldUpdate) {
                try {
                    handler.removeCallbacksAndMessages(null)
                    Logger.d("runnable2", "Removed callbacks and messages from handler")
                } catch (e: Exception) {
                    e.printStackTrace()
                    Logger.e(
                        "runnable2",
                        "Exception while removing callbacks and messages from handler: ${e.message}"
                    )
                }
                checkAndShowInternetDialog(
                    activity = runningActivity,
                    title = "Update Required",
                    description = "Please update the app to the latest version.",
                    negativeButtonText = "Later",
                    positiveButtonText = "Update",
                    callback = object : DialogCallBack {
                        override fun onPositiveClicked(dialog: Dialog) {
                            val appPackageName = runningActivity?.packageName
                            Logger.e(
                                "checkAndShowInternetDialog",
                                "onPositiveClicked: ${"https://play.google.com/store/apps/details?id=$appPackageName"}"
                            )
                            GlobalUtils().openLinkInBrowser(
                                runningActivity!!,
                                "https://play.google.com/store/apps/details?id=$appPackageName"
                            )
                        }

                        override fun onNegativeClicked(dialog: Dialog) {
                            Logger.e("checkAndShowInternetDialog", "onNegativeClicked")

                            if (shouldUpdate) {
                                dialog.dismiss()
                                sharedPrefConfig.apiResponse = response
                                startTimerForAdStatus(0)
                                checkAndShowAdIfAvailable()
                            }

                        }

                        override fun onDialogCancelled() {
                            Logger.e("checkAndShowInternetDialog", "onDialogCancelled")

                            sharedPrefConfig.apiResponse = response
                            startTimerForAdStatus(0)
                            checkAndShowAdIfAvailable()
                        }

                        override fun onDialogDismissed() {
                            Logger.e("checkAndShowInternetDialog", "onDialogDismissed")

                            sharedPrefConfig.apiResponse = response
                            startTimerForAdStatus(0)
                            checkAndShowAdIfAvailable()
                        }
                    },
                    updateRequiredVersions = response.appDetails.updateRequiredVersions,
                    forceUpdateVersions = response.appDetails.forceUpdateVersions
                )
            } else {
                sharedPrefConfig.apiResponse = response
                startTimerForAdStatus(2000)
                checkAndShowAdIfAvailable()
            }

        } catch (e: Exception) {
            startTimerForAdStatus(2000)
            checkAndShowAdIfAvailable()
        }
    }


    fun checkAndShowInternetDialog(
        activity: Activity?,
        title: String?,
        description: String?,
        negativeButtonText: String?,
        positiveButtonText: String?,
        callback: DialogCallBack,
        updateRequiredVersions: List<String>,
        forceUpdateVersions: List<String>
    ) {
        val currentVersion = getCurrentAppVersion(activity)

        val shouldForceUpdate = forceUpdateVersions.contains(currentVersion)
        val shouldUpdate = updateRequiredVersions.contains(currentVersion)

        if (shouldForceUpdate || shouldUpdate) {
            val isCancelable = !shouldForceUpdate

            showCommonDialog(
                activity = activity,
                title = title,
                description = description,
                negativeButtonText = negativeButtonText,
                positiveButtonText = positiveButtonText,
                isCancelable = isCancelable,
                callback = callback
            )
        } else {
            Logger.e("checkAndShowInternetDialog", "Current version does not require an update.")
        }
    }


    private fun checkAndShowAdIfAvailable() {
        Logger.d(
            "checkAndShowAdIfAvailable",
            "Initialized global preferences ${runningActivity?.let { sharedPrefConfig.appDetails.adStatus }}"
        )


        initClickCounts()
        Logger.d("checkAndShowAdIfAvailable", "Initialized click counts")

        if (isAdStatusOn()) {
            Logger.d("checkAndShowAdIfAvailable", "Ad status is ON")

            if (!GlobalUtils().isNetworkAvailable(runningActivity!!.applicationContext)) {
                Logger.d(
                    "checkAndShowAdIfAvailable",
                    "Network is not available, showing network dialog"
                )
                showInteretDialog()
            } else {
                Logger.d(
                    "checkAndShowAdIfAvailable",
                    "Network is available, showing app open ad and preloading interstitial and native ads"
                )
                showAppOpen()
                interstitialManager.preloadInterAd(runningActivity!!)
                nativeAdManager.loadNativeAd(runningActivity!!)
            }
        } else {
            Logger.d("checkAndShowAdIfAvailable", "Ad status is OFF")

            if (!com.harshil258.adplacer.adClass.AppOpenManager.isAdShowing) {
                Logger.d(
                    "checkAndShowAdIfAvailable",
                    "Ad is not showing, removing callbacks and continuing app flow"
                )

                try {
                    handler2.removeCallbacksAndMessages(null)
                    handler2.removeCallbacks(runnable2)
                    Logger.d(
                        "checkAndShowAdIfAvailable",
                        "Removed callbacks and messages from handler2"
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                    Logger.e(
                        "checkAndShowAdIfAvailable",
                        "Exception while removing callbacks and messages from handler2: ${e.message}"
                    )
                }

                try {
                    handler.removeCallbacksAndMessages(null)
                    handler.removeCallbacks(runnable)
                    Logger.d(
                        "checkAndShowAdIfAvailable",
                        "Removed callbacks and messages from handler"
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                    Logger.e(
                        "checkAndShowAdIfAvailable",
                        "Exception while removing callbacks and messages from handler: ${e.message}"
                    )
                }

                continueAppFlow()
            } else {
                Logger.d("checkAndShowAdIfAvailable", "Ad is currently showing, no action taken")
            }
        }
    }


    var handler: Handler = Handler(Looper.getMainLooper())
    var runnable: Runnable = Runnable {
        try {
            handler.removeCallbacksAndMessages(null)
        } catch (e: Exception) {
            e.printStackTrace()
        }


        if (!com.harshil258.adplacer.adClass.AppOpenManager.isAdShowing && isSplashRunning) {
            if (!GlobalUtils().isNetworkAvailable(runningActivity!!.applicationContext)) {
                if (sharedPrefConfig.appDetails.adStatus.trim { it <= ' ' } == "1"
                ) {
                    showInteretDialog()
                } else {
                    continueAppFlow()
                }
            } else {
                continueAppFlow()
            }
        }
    }


    var commonDialog: Dialog? = null

    fun showCommonDialog(
        activity: Activity?,
        title: String?,
        description: String?,
        negativeButtonText: String?,
        positiveButtonText: String?,
        isCancelable: Boolean,
        callback: DialogCallBack
    ) {
        if (commonDialog?.isShowing == true) return

        activity?.let { act ->
            commonDialog = Dialog(act).apply {
                setContentView(R.layout.layout_internet_dialog)
                window?.setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                window?.setBackgroundDrawableResource(android.R.color.transparent)
                setCancelable(isCancelable)

                findViewById<TextView>(R.id.txtTitle).text = title
                findViewById<TextView>(R.id.txtDescription).text = description

                findViewById<TextView>(R.id.txtPositive).apply {
                    text = positiveButtonText
                    visibility =
                        if (!positiveButtonText.isNullOrEmpty()) View.VISIBLE else View.GONE
                    setOnClickListener {
                        commonDialog?.let { dialog ->
                            callback.onPositiveClicked(dialog)
                        }
                    }
                }

                findViewById<TextView>(R.id.txtNegative).apply {
                    text = negativeButtonText
                    visibility =
                        if (!negativeButtonText.isNullOrEmpty()) View.VISIBLE else View.GONE
                    setOnClickListener {
                        commonDialog?.let { dialog ->
                            callback.onNegativeClicked(dialog)
                        }
                    }
                }

                setOnCancelListener {
                    callback.onDialogCancelled()
                }

                setOnDismissListener {
                    callback.onDialogDismissed()
                }

                show()

            }
        } ?: run {
            Logger.e("showInternetDialog", "Activity is null, cannot show dialog.")
        }
    }

    // Helper function to get current app version
    fun getCurrentAppVersion(activity: Activity?): String? {
        return try {
            val packageInfo = activity?.packageManager?.getPackageInfo(activity.packageName, 0)
            packageInfo?.versionName
        } catch (e: Exception) {
            Logger.e("getCurrentAppVersion", "Failed to get current app version" + e)
            null
        }
    }

    fun showOneSignalNotificationPrompt(){
        CoroutineScope(Dispatchers.IO).launch {
            OneSignal.Notifications.requestPermission(false)
        }
    }

    fun startTimerForContinueFlow(duration: Int) {
        try {
            handler.removeCallbacksAndMessages(null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        handler.postDelayed(runnable, duration.toLong())
    }

    var handler2: Handler = Handler(Looper.getMainLooper())
    var runnable2: Runnable = Runnable {
        try {
            handler2.removeCallbacksAndMessages(null)
            Logger.d("runnable2", "Removed callbacks and messages from handler2")
        } catch (e: Exception) {
            e.printStackTrace()
            Logger.e(
                "runnable2",
                "Exception while removing callbacks and messages from handler2: ${e.message}"
            )
        }

        if (!com.harshil258.adplacer.adClass.AppOpenManager.isAdShowing) {
            Logger.d("runnable2", "Ad is not showing")

            if (!GlobalUtils().isNetworkAvailable(runningActivity!!.applicationContext)) {
                Logger.d("runnable2", "Network is not available")

                if (isAdStatusOn()) {
                    Logger.d("runnable2", "Ad status is ON, showing network dialog")
                    showInteretDialog()
                } else {
                    Logger.d("runnable2", "Ad status is OFF, continuing app flow")
                    continueAppFlow()
                }
            } else {
                Logger.d("runnable2", "Network is available")

                if (isAdStatusOn()) {
                    Logger.d(
                        "runnable2",
                        "Ad status is ON, continuing app flow after removing callbacks and messages from handler"
                    )

                    try {
                        handler.removeCallbacksAndMessages(null)
                        Logger.d("runnable2", "Removed callbacks and messages from handler")
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Logger.e(
                            "runnable2",
                            "Exception while removing callbacks and messages from handler: ${e.message}"
                        )
                    }

                    continueAppFlow()
                } else {
                    Logger.d(
                        "runnable2",
                        "Ad status is OFF, continuing app flow without any action"
                    )
                    continueAppFlow()
                }
            }
        } else {
            Logger.d("runnable2", "Ad is currently showing, no action taken")
        }
    }


    private fun startTimerForAdStatus(duration: Int) {
        try {
            handler2.removeCallbacksAndMessages(null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        handler2.postDelayed(runnable2, duration.toLong())
    }

    fun showInteretDialog() {
        if (com.harshil258.adplacer.adClass.AppOpenManager.appOpenAd != null) {
            return
        }


        messagingCallback?.showNetworkDialog()


    }

    fun initClickCounts() {
        InterstitialManager.clickCounts = 3  // Default value

        sharedPrefConfig.appDetails.interstitialAdFrequency.takeIf {
            it.isNotEmpty() && TextUtils.isDigitsOnly(
                it
            )
        }
            ?.let {
                try {
                    InterstitialManager.clickCounts = it.toInt()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
    }


    fun registerMessagingCallback(instanceCallback: MessagingCallback?) {
        messagingCallback = instanceCallback
    }


    init {
        adPlacerApplication = this
        homeButtonReceiver = HomeButtonReceiver()
        val filter = IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            instance.registerReceiver(homeButtonReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            instance.registerReceiver(homeButtonReceiver, filter)
        }

        MobileAds.initialize(instance) { }

        appOpenManager = com.harshil258.adplacer.adClass.AppOpenManager()
        interstitialManager = InterstitialManager()
        rewardClass = RewardAdManager()
        nativeAdManager = NativeAdManager()
    }

}
