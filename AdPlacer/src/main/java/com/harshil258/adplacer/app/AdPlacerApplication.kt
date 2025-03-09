package com.harshil258.adplacer.app

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.Log
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.ProcessLifecycleOwner
import com.facebook.ads.AudienceNetworkAds
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.google.gson.Gson
import com.harshil258.adplacer.UpdateActivity
import com.harshil258.adplacer.adClass.AppOpenAdManager
import com.harshil258.adplacer.adClass.InterstitialManager
import com.harshil258.adplacer.adClass.NativeAdManager
import com.harshil258.adplacer.adClass.RewardAdManager
import com.harshil258.adplacer.api.AdApiClient
import com.harshil258.adplacer.api.AdApiClient.Companion.BASE_URL_API
import com.harshil258.adplacer.interfaces.AdCallback
import com.harshil258.adplacer.interfaces.ApiInterface
import com.harshil258.adplacer.interfaces.MessagingCallback
import com.harshil258.adplacer.models.ApiResponse
import com.harshil258.adplacer.models.SCREENS
import com.harshil258.adplacer.models.TYPE_OF_RESPONSE
import com.harshil258.adplacer.utils.AdStatus
import com.harshil258.adplacer.utils.Constants
import com.harshil258.adplacer.utils.Constants.adPlacerInstance
import com.harshil258.adplacer.utils.Extensions.isAppOpenAdEmpty
import com.harshil258.adplacer.utils.HomeButtonReceiver
import com.harshil258.adplacer.utils.Logger
import com.harshil258.adplacer.utils.pingSite
import com.harshil258.adplacer.utils.sharedpreference.SecureStorageManager.Companion.initSecureStorageManager
import com.harshil258.adplacer.utils.sharedpreference.SecureStorageManager.Companion.sharedPrefConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class AdPlacerApplication(private val application: Application) {

    val appOpenAdManager: AppOpenAdManager = AppOpenAdManager()
    val interstitialAdManager: InterstitialManager = InterstitialManager()
    val rewardAdManager: RewardAdManager = RewardAdManager()
    val nativeAdManager: NativeAdManager = NativeAdManager()
    var messagingListener: MessagingCallback? = null

    private val homeButtonReceiver = HomeButtonReceiver()
    private val mainHandler = Handler(Looper.getMainLooper())
    private lateinit var appUpdateManager: AppUpdateManager

    init {
        adPlacerInstance = this
        initSecureStorageManager(application)
        registerActivityLifecycleCallbacks()
        registerHomeButtonReceiver()
        initializeMobileAds()
        application.pingSite()
        appUpdateManager = AppUpdateManagerFactory.create(application.applicationContext)
    }

    /**
     * Registers a callback to monitor activity lifecycle events.
     */
    private fun registerActivityLifecycleCallbacks() {
        application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                Constants.activityStack.add(activity)
                logActivityStack("Created")
            }

            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {
                Constants.activityStack.remove(activity)
                logActivityStack("Destroyed")
            }
        })
    }

    /**
     * Logs the current activity stack.
     */
    private fun logActivityStack(event: String) {
        val activityNames = Constants.activityStack.map { it::class.java.simpleName }
        Log.d("ActivityLifecycle", "$event: ${activityNames.joinToString(" -> ")}")
    }

    /**
     * Registers the receiver to listen for home button presses.
     */
    private fun registerHomeButtonReceiver() {
        val intentFilter = IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            application.registerReceiver(homeButtonReceiver, intentFilter, Context.RECEIVER_EXPORTED)
        } else {
            application.registerReceiver(homeButtonReceiver, intentFilter)
        }
    }

    /**
     * Opens the Ad Inspector. Useful for debugging ad issues.
     */
    fun openAdInspector() {
        // Use Constants.currentActivity instead of a global runningActivity.
        Constants.currentActivity?.let { activity ->
            MobileAds.openAdInspector(activity) { error ->
                Logger.e(ADS_TAG, "Error opening Ad Inspector: ${error?.message}")
            }
        }
    }

    /**
     * Initializes the Mobile Ads SDK on a background thread.
     */
    private fun initializeMobileAds() {
        CoroutineScope(Dispatchers.IO).launch {
            // Initialize Facebook Audience Network ads.
            AudienceNetworkAds.initialize(application)
            MobileAds.initialize(application) { initializationStatus ->
                Log.e(ADS_TAG, "initializeMobileAds: DEVICE_ID_EMULATOR ${AdRequest.DEVICE_ID_EMULATOR}")
                Log.e(ADS_TAG, "initializeMobileAds: testDeviceIds ${Constants.testDeviceIds.toSet()}")
                Constants.testDeviceIds.add(AdRequest.DEVICE_ID_EMULATOR)
                Log.e(ADS_TAG, "initializeMobileAds: after adding testDeviceIds ${Constants.testDeviceIds.toSet()}")

                val requestConfig = RequestConfiguration.Builder()
                    .setTestDeviceIds(Constants.testDeviceIds)
                    .build()
                MobileAds.setRequestConfiguration(requestConfig)

                // Log adapter statuses.
                for ((adapterName, adapterStatus) in initializationStatus.adapterStatusMap) {
                    Logger.d(
                        ADS_TAG,
                        "Adapter name: $adapterName, Description: ${adapterStatus?.description}, Latency: ${adapterStatus?.latency}"
                    )
                }
            }
        }
    }

    /**
     * Registers a LifecycleObserver for process-level lifecycle events.
     */
    fun registerLifecycleObserver(observer: LifecycleObserver?) {
        observer?.let {
            ProcessLifecycleOwner.get().lifecycle.addObserver(it)
        }
    }

    /**
     * Displays the App Open ad if allowed by the current conditions.
     */
    fun showAppOpenAd() {
        // Do not show App Open ad if any full-screen ad is already showing or if app open ads are disabled.
        if (InterstitialManager.isInterstitialAdShowing || RewardAdManager.isAdShowing || AppOpenAdManager.shouldStopAppOpenAd) {
            return
        }

        if (!isAppOpenAdEmpty()) {
            Constants.currentActivity?.let { activity ->
                appOpenAdManager.showAppOpenAd(activity, object : AdCallback {
                    override fun onAdDisplayed(isDisplayed: Boolean) {
                        if (Constants.isSplashScreenRunning) {
                            Constants.isSplashScreenRunning = false
                            continueAppFlow()
                        }
                    }
                })
            }
        }
    }

    /**
     * Continues the application flow after ads are shown or after a timeout.
     */
    fun continueAppFlow() {
        messagingListener?.startScreenTransitionTimer()
        mainHandler.removeCallbacksAndMessages(null)
        Log.d(ADS_TAG, "continueAppFlow invoked")

        val currentActivity = Constants.currentActivity ?: return
        val appDetails = sharedPrefConfig.appDetails
        val nextScreen = appDetails.whichScreenToGo

        when {
            appDetails.howtousestart == AdStatus.ON.name && !sharedPrefConfig.isHowToUseShowDone -> {
                messagingListener?.openHowToUseActivity()
            }
            nextScreen.isNotEmpty() -> {
                when (nextScreen) {
                    SCREENS.EXTRA_START.name -> messagingListener?.openExtraStartActivity()
                    SCREENS.START.name -> messagingListener?.openStartActivity()
                    else -> messagingListener?.openHomeActivity()
                }
            }
            else -> messagingListener?.openHomeActivity()
        }
        Constants.isSplashScreenRunning = false
    }

    /**
     * Initializes and fetches Firebase Remote Config with retry logic.
     */
    private fun initializeAndFetchFirebaseConfig(
        onSuccess: (FirebaseRemoteConfig) -> Unit,
        onFailure: () -> Unit,
        retries: Int = 3,
        retryDelayMillis: Long = 2000
    ) {
        val firebaseRemoteConfig = FirebaseRemoteConfig.getInstance()
        val defaultConfigs = mapOf("temp_response" to "default value")
        firebaseRemoteConfig.setDefaultsAsync(defaultConfigs)

        val configSettings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(2)
            .build()
        firebaseRemoteConfig.setConfigSettingsAsync(configSettings)

        firebaseRemoteConfig.fetchAndActivate().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                onSuccess(firebaseRemoteConfig)
            } else if (retries > 0) {
                mainHandler.postDelayed({
                    initializeAndFetchFirebaseConfig(onSuccess, onFailure, retries - 1, retryDelayMillis)
                }, retryDelayMillis)
            } else {
                onFailure()
            }
        }
    }

    var isContinueFlowCompleted = false

    /**
     * Fetches API response either from Firebase Remote Config or via a network call.
     */
    fun fetchApiResponse(responseType: TYPE_OF_RESPONSE) {
        if (Constants.bypassApiResponse && sharedPrefConfig.isResponseGot) {
            startContinueFlowTimer(0)
            isContinueFlowCompleted = true
            return
        }

        initializeAndFetchFirebaseConfig({ firebaseConfig ->
            messagingListener?.onFirebaseResponseReceived(firebaseConfig)
            if (responseType == TYPE_OF_RESPONSE.GOOGLE) {
                val responseJson = firebaseConfig.getString(responseType.value)
                val apiResponse: ApiResponse = Gson().fromJson(responseJson, ApiResponse::class.java)
                processSuccessfulApiResponse(apiResponse)
            } else {
                Constants.libraryPackageName = firebaseConfig.getString("LIBRARY_PACKAGE_NAME")
                Constants.authorizationADS = firebaseConfig.getString("AuthorizationADS")
                BASE_URL_API = firebaseConfig.getString("BaseUrl")

                val jsonBody = JSONObject().apply {
                    put("appid", Constants.libraryPackageName)
                    put("secretKey", Constants.authorizationADS)
                }
                val requestBody = jsonBody.toString()
                    .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
                val call = AdApiClient().client.create(ApiInterface::class.java).getAll(requestBody)
                call?.enqueue(object : Callback<ApiResponse?> {
                    override fun onResponse(call: Call<ApiResponse?>, response: Response<ApiResponse?>) {
                        if (response.isSuccessful && response.body() != null) {
                            processSuccessfulApiResponse(response.body()!!)
                        } else {
                            handleFailureResponse()
                        }
                    }

                    override fun onFailure(call: Call<ApiResponse?>, t: Throwable) {
                        handleFailureResponse()
                    }
                })
            }
        }, {
            messagingListener?.exitApplication()
        })
    }

    /**
     * Handles API response failure.
     */
    private fun handleFailureResponse() {
        startContinueFlowTimer(2000)
        preloadAllAds()
    }

    /**
     * Processes a successful API response.
     */
    private fun processSuccessfulApiResponse(apiResponse: ApiResponse) {
        try {
            val currentVersion = getAppVersion(Constants.currentActivity)
            val requiresForceUpdate = apiResponse.appDetails.forceUpdateVersions.contains(currentVersion)
            val requiresUpdate = !apiResponse.appDetails.noUpdateRequiredVersion.contains(currentVersion)
            Log.d(ADS_TAG, "API Response: ${Gson().toJson(apiResponse)}")
            sharedPrefConfig.isResponseGot = true
            saveApiResponse(apiResponse)

            if (requiresForceUpdate || requiresUpdate) {
                mainHandler.removeCallbacksAndMessages(null)
                val updateInfoTask = appUpdateManager.appUpdateInfo
                updateInfoTask.addOnSuccessListener { appUpdateInfo: AppUpdateInfo ->
                    if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                        Constants.currentActivity?.startActivity(
                            Intent(Constants.currentActivity, UpdateActivity::class.java)
                        )
                    } else {
                        if (!requiresForceUpdate) preloadAllAds()
                        startContinueFlowTimer(0)
                    }
                }.addOnFailureListener {
                    if (!requiresForceUpdate) preloadAllAds()
                    startContinueFlowTimer(0)
                }
            } else {
                startContinueFlowTimer(0)
                preloadAllAds()
            }
        } catch (e: Exception) {
            startContinueFlowTimer(2000)
            preloadAllAds()
        }
    }

    /**
     * Preloads all required ad formats.
     */
    fun preloadAllAds() {
        Constants.currentActivity?.let { currentActivity ->
            if (Constants.preloadInterstitial) {
                interstitialAdManager.preloadInterstitialAd(currentActivity)
            }
            if (Constants.preloadNative) {
                nativeAdManager.loadNativeAd(currentActivity)
            }
            if (Constants.preloadReward) {
                rewardAdManager.preloadRewardAd(currentActivity)
            }
            if (Constants.preloadAppOpen) {
                appOpenAdManager.loadAppOpenAd(currentActivity, object : AdCallback {
                    override fun onAdDisplayed(isDisplayed: Boolean) {
                        // No action needed.
                    }
                })
            }
        }
    }

    /**
     * Saves the API response in shared preferences and notifies the messaging listener.
     */
    private fun saveApiResponse(apiResponse: ApiResponse) {
        sharedPrefConfig.apiResponse = apiResponse
        messagingListener?.onApiResponseSaved()
    }

    /**
     * Retrieves the current app version.
     */
    private fun getAppVersion(activity: Activity?): String {
        return try {
            val packageInfo = activity?.packageManager?.getPackageInfo(activity.packageName, 0)
            packageInfo?.versionName ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    fun showInternetDialog() {
        messagingListener?.showNetworkDialog()
    }

    fun registerMessagingListener(listener: MessagingCallback?) {
        messagingListener = listener
    }

    fun startContinueFlowTimer(delayMillis: Long) {
        if (!isContinueFlowCompleted) {
            mainHandler.postDelayed({ continueAppFlow() }, delayMillis)
        }
    }

    /**
     * Initializes click counts for interstitial ads.
     */
    fun initializeClickCounts() {
        InterstitialManager.currentClickCount = 3  // Default value
        sharedPrefConfig.appDetails.interstitialAdFrequency.takeIf {
            it.isNotEmpty() && TextUtils.isDigitsOnly(it)
        }?.let {
            try {
                InterstitialManager.currentClickCount = it.toInt()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    companion object {

        fun getInstance(): AdPlacerApplication {
            return adPlacerInstance
                ?: throw IllegalStateException("AdPlacerApplication is not initialized")
        }

        // For logging and debugging.
        private const val ADS_TAG = "AdPlacerApplication"
    }
}



