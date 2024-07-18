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
import com.harshil258.adplacer.api.AdApiClient.Companion.BASE_URL_API
import com.harshil258.adplacer.utils.Constants.adPlacerApplication
import com.harshil258.adplacer.utils.Constants.isSplashRunning
import com.harshil258.adplacer.utils.Constants.preLoadInterstitial
import com.harshil258.adplacer.utils.Constants.preLoadNative
import com.harshil258.adplacer.utils.Constants.preLoadReward
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




    var appOpenManager: com.harshil258.adplacer.adClass.AppOpenManager

    val interstitialManager: InterstitialManager
    val rewardClass: RewardAdManager
    val nativeAdManager: NativeAdManager
    var messagingCallback: MessagingCallback? = null
    var homeButtonReceiver: HomeButtonReceiver


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


    init {
        Logger.i("TAGCOMMON", "AdPlacerApplication  FIRST LOG")

        adPlacerApplication = this
        sharedPrefConfigInstance(instance)
    }

    fun processLifecycleRegister(observer: LifecycleObserver?) {
        ProcessLifecycleOwner.get().lifecycle.addObserver(observer!!)
    }



    fun showAppOpenAd() {
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
        Logger.e("TAGCOMMON", "continueAppFlow: Called")

        handler.removeCallbacksAndMessages(null)

        val activity = runningActivity ?: run {
            Logger.e("TAGCOMMON", "continueAppFlow: No running activity found. Exiting function.")
            return
        }

        val appDetails = sharedPrefConfig.appDetails
        val whichScreenToGo = appDetails.whichScreenToGo

        Logger.e("TAGCOMMON", "continueAppFlow: whichScreenToGo = $whichScreenToGo")
        Logger.e("TAGCOMMON", "continueAppFlow: howtousestart = ${appDetails.howtousestart}")
        Logger.e(
            "TAGCOMMON",
            "continueAppFlow: isHowToUseShowDone = ${sharedPrefConfig.isHowToUseShowDone}"
        )

        when {
            appDetails.howtousestart == STATUS.ON.name && !sharedPrefConfig.isHowToUseShowDone -> {
                Logger.e("TAGCOMMON", "continueAppFlow: Opening HowToUseActivity.")
                messagingCallback?.openHowToUseActivity()
            }

            whichScreenToGo.isNotEmpty() -> {
                Logger.e(
                    "TAGCOMMON",
                    "continueAppFlow: whichScreenToGo is not empty. Value: $whichScreenToGo"
                )

                when (whichScreenToGo) {
                    SCREENS.EXTRA_START.name -> {
                        Logger.e("TAGCOMMON", "continueAppFlow: Opening ExtraStartActivity.")
                        messagingCallback?.openExtraStartActivity()
                    }

                    SCREENS.START.name -> {
                        Logger.e("TAGCOMMON", "continueAppFlow: Opening StartActivity.")
                        messagingCallback?.openStartActivity()
                    }

                    else -> {
                        Logger.e("TAGCOMMON", "continueAppFlow: Opening HomeActivity.")
                        messagingCallback?.openHomeActivity()
                    }
                }
            }

            else -> {
                Logger.e(
                    "TAGCOMMON",
                    "continueAppFlow: whichScreenToGo is empty or invalid. Opening HomeActivity."
                )
                messagingCallback?.openHomeActivity()
            }
        }

        activity.finish()
        Logger.e("TAGCOMMON", "continueAppFlow: Running activity finished.")
        isSplashRunning = false
        Logger.e("TAGCOMMON", "continueAppFlow: Splash screen is not running.")
    }


    fun initializeAndFetchFirebaseConfig(
        onSuccess: (FirebaseRemoteConfig) -> Unit,
        onFailure: () -> Unit,
        retries: Int = 3,
        retryDelay: Long = 2000 // delay in milliseconds
    ) {
        Logger.e(
            "FirebaseConfig",
            "initializeAndFetchFirebaseConfig: Called with retries=$retries and retryDelay=$retryDelay ms"
        )

        val firebaseRemoteConfig: FirebaseRemoteConfig = FirebaseRemoteConfig.getInstance()

        // Set default values for Remote Config parameters
        val configDefaults: Map<String, Any> = mapOf(
            "temp_response" to "default value" // Replace with your default value
        )
        firebaseRemoteConfig.setDefaultsAsync(configDefaults)
        Logger.e(
            "FirebaseConfig",
            "initializeAndFetchFirebaseConfig: Default values set: $configDefaults"
        )

        val configSettings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(2) // Set to 2 seconds
            .build()
        firebaseRemoteConfig.setConfigSettingsAsync(configSettings)
        Logger.e(
            "FirebaseConfig",
            "initializeAndFetchFirebaseConfig: Config settings applied: $configSettings"
        )

        // Fetch and activate the remote config values
        firebaseRemoteConfig.fetchAndActivate()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Logger.e(
                        "FirebaseConfig",
                        "initializeAndFetchFirebaseConfig: Fetch and activate successful"
                    )
                    // Call the success callback
                    onSuccess(firebaseRemoteConfig)
                } else {
                    Logger.e(
                        "FirebaseConfig",
                        "initializeAndFetchFirebaseConfig: Failed to fetch config. Retries left: $retries"
                    )

                    if (retries > 0) {
                        Logger.e(
                            "FirebaseConfig",
                            "initializeAndFetchFirebaseConfig: Retrying in $retryDelay ms"
                        )
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
                        Logger.e(
                            "FirebaseConfig",
                            "initializeAndFetchFirebaseConfig: No retries left. Invoking onFailure callback"
                        )
                        // Call the failure callback if no retries are left
                        onFailure()
                    }
                }
            }
    }


    fun fetchApiResponse(whichResponse: TYPE_OF_RESPONSE) {
        Logger.e("TAGCOMMON", "fetchApiResponse: Called with whichResponse=$whichResponse")

        initializeAndFetchFirebaseConfig(onSuccess = { firebaseConfig ->
            Logger.i(
                "TAGCOMMON",
                "initializeAndFetchFirebaseConfig: onSuccess for $whichResponse"
            )
            messagingCallback?.gotFirebaseResponse(firebaseConfig)

            if (whichResponse == TYPE_OF_RESPONSE.GOOGLE) {
                val response = firebaseConfig.getString(whichResponse.value)
                val apiResponse: ApiResponse = Gson().fromJson(response)
                Logger.e("TAGCOMMON", "onResponse: GOOGLE response fetched")
                handleSuccessfulApiResponse(apiResponse)
            } else {
                LIBRARY_PACKAGE_NAME = firebaseConfig.getString("LIBRARY_PACKAGE_NAME")
                AuthorizationADS = firebaseConfig.getString("AuthorizationADS")
                BASE_URL_API = firebaseConfig.getString("BaseUrl")
                Logger.i("TAGCOMMON", "BASE_URL_API: $BASE_URL_API")
                Logger.i("TAGCOMMON", "AuthorizationADS: $AuthorizationADS")
                Logger.i("TAGCOMMON", "LIBRARY_PACKAGE_NAME: $LIBRARY_PACKAGE_NAME")

                val jsonBody = JSONObject().apply {
                    put("appid", LIBRARY_PACKAGE_NAME)
                    put("secretKey", AuthorizationADS)
                }
                val requestBody = jsonBody.toString()
                    .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
                Logger.e("TAGCOMMON", "Request body created: $jsonBody")

                val call = AdApiClient().client.create(ApiInterface::class.java).getAll(requestBody)
                call?.enqueue(object : Callback<ApiResponse?> {
                    override fun onResponse(
                        call: Call<ApiResponse?>,
                        response: Response<ApiResponse?>
                    ) {
                        Logger.i("TAGCOMMON", "onResponse: ${response.body()}")

                        if (response.isSuccessful && response.body() != null) {
                            Logger.e("TAGCOMMON", "onResponse: API response successful")
                            handleSuccessfulApiResponse(response.body()!!)
                        } else {
                            Logger.e("TAGCOMMON", "onResponse: API response unsuccessful")
                            onFailureResponse()
                        }
                    }

                    override fun onFailure(call: Call<ApiResponse?>, t: Throwable) {
                        Logger.i("TAGCOMMON", "onFailure: ${t.message}")
                        onFailureResponse()
                    }
                })
            }
        }, onFailure = {
            Logger.e("TAGCOMMON", "initializeAndFetchFirebaseConfig: onFailure")
            messagingCallback?.exitTheApplication()
        },
            retries = 3,
            retryDelay = 2000
        )
    }

    private fun onFailureResponse() {
        startAdStatusTimer(2000)
        checkAndShowAdIfAvailable()
    }

    private fun handleSuccessfulApiResponse(response: ApiResponse) {
        Logger.i(
            "TAGCOMMON",
            "Received response: App Name = ${response.appDetails.appName}, Ad Status = ${response.appDetails.adStatus}"
        )
        try {
            val currentVersion = getCurrentAppVersion(runningActivity)
            val requiresForceUpdate =
                response.appDetails.forceUpdateVersions.contains(currentVersion)
            val requiresUpdate = response.appDetails.updateRequiredVersions.contains(currentVersion)

            if (requiresForceUpdate || requiresUpdate) {
                try {
                    handler.removeCallbacksAndMessages(null)
                    Logger.d("UpdateHandler", "Removed all callbacks and messages from handler")
                } catch (e: Exception) {
                    Logger.e(
                        "UpdateHandler",
                        "Exception removing callbacks and messages: ${e.message}"
                    )
                }
                promptForUpdate(
                    activity = runningActivity,
                    title = "Update Required",
                    description = "Please update the app to the latest version.",
                    negativeButtonText = "Later",
                    positiveButtonText = "Update",
                    response = response,
                    requiresUpdate = requiresUpdate
                )
            } else {
                saveApiResponse(response)
                startAdStatusTimer(delayMillis = 2000)
                checkAndShowAdIfAvailable()
            }
        } catch (e: Exception) {
            Logger.e("TAGCOMMON", "Exception handling API response: ${e.message}")
            startAdStatusTimer(delayMillis = 2000)
            checkAndShowAdIfAvailable()
        }
    }

    private fun promptForUpdate(
        activity: Activity?,
        title: String,
        description: String,
        negativeButtonText: String,
        positiveButtonText: String,
        response: ApiResponse,
        requiresUpdate: Boolean
    ) {
        val dialogCallback = object : DialogCallBack {
            override fun onPositiveClicked(dialog: Dialog) {
                val appPackageName = activity?.packageName
                Logger.e(
                    "UpdateDialog",
                    "User accepted update: https://play.google.com/store/apps/details?id=$appPackageName"
                )
                GlobalUtils().openLinkInBrowser(
                    activity!!,
                    "https://play.google.com/store/apps/details?id=$appPackageName"
                )
            }

            override fun onNegativeClicked(dialog: Dialog) {
                Logger.e("UpdateDialog", "User deferred update")
                handleDialogDismissOrCancel(response, requiresUpdate)
            }

            override fun onDialogCancelled() {
                Logger.e("UpdateDialog", "Update dialog cancelled")
                handleDialogDismissOrCancel(response, requiresUpdate)
            }

            override fun onDialogDismissed() {
                Logger.e("UpdateDialog", "Update dialog dismissed")
                handleDialogDismissOrCancel(response, requiresUpdate)
            }
        }

        val currentVersion = getCurrentAppVersion(activity)
        val forceUpdateRequired = response.appDetails.forceUpdateVersions.contains(currentVersion)
        val isCancelable = !forceUpdateRequired

        showCommonDialog(
            activity = activity,
            title = title,
            description = description,
            negativeButtonText = negativeButtonText,
            positiveButtonText = positiveButtonText,
            isCancelable = isCancelable,
            callback = dialogCallback
        )
    }

    private fun handleDialogDismissOrCancel(response: ApiResponse, requiresUpdate: Boolean) {
        if (requiresUpdate) {
            saveApiResponse(response)
            startAdStatusTimer(delayMillis = 0)
            checkAndShowAdIfAvailable()
        }
    }

    private fun saveApiResponse(response: ApiResponse) {
        sharedPrefConfig.apiResponse = response
    }


    private fun checkAndShowAdIfAvailable() {
        Logger.d(
            "AdCheck",
            "Initializing global preferences with ad status: ${runningActivity?.let { sharedPrefConfig.appDetails.adStatus }}"
        )

        initClickCounts()
        Logger.d("AdCheck", "Click counts initialized")

        if (isAdStatusOn()) {
            Logger.d("AdCheck", "Ad status is ON")

            runningActivity?.let {
                if (!GlobalUtils().isNetworkAvailable(it)) {
                    Logger.d("AdCheck", "Network is not available, showing network dialog")
                    showInternetDialog()
                } else {
                    Logger.d(
                        "AdCheck",
                        "Network is available, showing app open ad and preloading interstitial and native ads"
                    )
                    showAppOpenAd()
                    if(preLoadInterstitial){
                        interstitialManager.preloadInterstitialAd(runningActivity!!)
                    }
                    if(preLoadNative){
                        nativeAdManager.loadNativeAd(runningActivity!!)
                    }
                    if(preLoadReward){
                        rewardClass.preloadRewardAd(runningActivity!!)
                    }

                }
            }

        } else {
            Logger.d("AdCheck", "Ad status is OFF")

            if (!com.harshil258.adplacer.adClass.AppOpenManager.isAdShowing) {
                Logger.d(
                    "AdCheck",
                    "No ad is currently showing, removing callbacks and continuing app flow"
                )

                removeCallbacks(handler2, runnable2, "Handler2")
                removeCallbacks(handler, runnable, "Handler")

                continueAppFlow()
            } else {
                Logger.d("AdCheck", "Ad is currently showing, no action taken")
            }
        }
    }

    private fun removeCallbacks(handler: Handler, runnable: Runnable, handlerName: String) {
        try {
            handler.removeCallbacksAndMessages(null)
            handler.removeCallbacks(runnable)
            Logger.d("AdCheck", "Removed callbacks and messages from $handlerName")
        } catch (e: Exception) {
            Logger.e(
                "AdCheck",
                "Exception while removing callbacks and messages from $handlerName: ${e.message}"
            )
        }
    }


    var handler: Handler = Handler(Looper.getMainLooper())

    var runnable: Runnable = Runnable {
        try {
            handler.removeCallbacksAndMessages(null)
        } catch (e: Exception) {
            Logger.e("HandlerRunnable", "Error while removing callbacks: ${e.message}")
        }

        if (!com.harshil258.adplacer.adClass.AppOpenManager.isAdShowing && isSplashRunning) {
            val isNetworkAvailable = GlobalUtils().isNetworkAvailable(runningActivity!!.applicationContext)
            val adStatus = sharedPrefConfig.appDetails.adStatus.trim()

            if (!isNetworkAvailable) {
                Logger.d("HandlerRunnable", "Network is not available")

                if (adStatus == STATUS.ON.name) {
                    Logger.d("HandlerRunnable", "Ad status is ON, showing internet dialog")
                    showInternetDialog()
                } else {
                    Logger.d("HandlerRunnable", "Ad status is OFF, continuing app flow")
                    continueAppFlow()
                }
            } else {
                Logger.d("HandlerRunnable", "Network is available, continuing app flow")
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
            Logger.e("getCurrentAppVersion", "Failed to get current app version  " + e)
            null
        }
    }

    fun showOneSignalNotificationPrompt() {
        runningActivity?.let {
            OneSignal.initWithContext(
                it,
                sharedPrefConfig.appDetails.oneSignalAppId
            )
        }
        CoroutineScope(Dispatchers.Main).launch {
            OneSignal.Notifications.requestPermission(false)
        }
    }

    fun subscribeToTheTopic(tag : String) {
        CoroutineScope(Dispatchers.Main).launch {
            OneSignal.User.addTag("topic", tag)
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
            Logger.d("Runnable2", "Removed callbacks and messages from handler2")
        } catch (e: Exception) {
            Logger.e("Runnable2", "Error while removing callbacks: ${e.message}")
        }

        if (!com.harshil258.adplacer.adClass.AppOpenManager.isAdShowing) {
            Logger.d("Runnable2", "Ad is not showing")

            val isNetworkAvailable = GlobalUtils().isNetworkAvailable(runningActivity!!.applicationContext)

            if (!isNetworkAvailable) {
                Logger.d("Runnable2", "Network is not available")

                if (isAdStatusOn()) {
                    Logger.d("Runnable2", "Ad status is ON, showing network dialog")
                    showInternetDialog()
                } else {
                    Logger.d("Runnable2", "Ad status is OFF, continuing app flow")
                    continueAppFlow()
                }
            } else {
                Logger.d("Runnable2", "Network is available")

                if (isAdStatusOn()) {
                    Logger.d("Runnable2", "Ad status is ON, continuing app flow after removing callbacks")
                    continueAppFlow()
                } else {
                    Logger.d("Runnable2", "Ad status is OFF, continuing app flow without action")
                    continueAppFlow()
                }
            }
        } else {
            Logger.d("Runnable2", "Ad is currently showing, no action taken")
        }
    }



    private fun startAdStatusTimer(delayMillis: Int) {
        try {
            handler2.removeCallbacksAndMessages(null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        messagingCallback?.startingTimerToChangeScreen()
        handler2.postDelayed(runnable2, delayMillis.toLong())
    }

    fun showInternetDialog() {
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


}
