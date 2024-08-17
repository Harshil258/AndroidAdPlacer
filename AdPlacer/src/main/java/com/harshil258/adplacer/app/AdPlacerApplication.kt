package com.harshil258.adplacer.app

import android.app.Activity
import android.app.Application
import android.app.Dialog
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
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.google.gson.Gson
import com.harshil258.adplacer.adClass.AppOpenManager
import com.harshil258.adplacer.adClass.InterstitialManager
import com.harshil258.adplacer.adClass.NativeAdManager
import com.harshil258.adplacer.adClass.RewardAdManager
import com.harshil258.adplacer.api.AdApiClient
import com.harshil258.adplacer.api.AdApiClient.Companion.BASE_URL_API
import com.harshil258.adplacer.interfaces.AdCallback
import com.harshil258.adplacer.interfaces.ApiInterface
import com.harshil258.adplacer.interfaces.DialogCallBack
import com.harshil258.adplacer.interfaces.MessagingCallback
import com.harshil258.adplacer.models.ApiResponse
import com.harshil258.adplacer.models.SCREENS
import com.harshil258.adplacer.models.TYPE_OF_RESPONSE
import com.harshil258.adplacer.utils.Constants.AuthorizationADS
import com.harshil258.adplacer.utils.Constants.LIBRARY_PACKAGE_NAME
import com.harshil258.adplacer.utils.Constants.activityStack
import com.harshil258.adplacer.utils.Constants.adPlacerApplication
import com.harshil258.adplacer.utils.Constants.isSplashRunning
import com.harshil258.adplacer.utils.Constants.preLoadInterstitial
import com.harshil258.adplacer.utils.Constants.preLoadNative
import com.harshil258.adplacer.utils.Constants.preLoadReward
import com.harshil258.adplacer.utils.Constants.runningActivity
import com.harshil258.adplacer.utils.Constants.testDeviceIds
import com.harshil258.adplacer.utils.Constants.wantToByPassResponse
import com.harshil258.adplacer.utils.DialogUtil
import com.harshil258.adplacer.utils.GlobalUtils
import com.harshil258.adplacer.utils.HomeButtonReceiver
import com.harshil258.adplacer.utils.Logger
import com.harshil258.adplacer.utils.Logger.TAG
import com.harshil258.adplacer.utils.STATUS

import com.harshil258.adplacer.utils.extentions.isAppOpenEmpty
import com.harshil258.adplacer.utils.pingSite
import com.zeel_enterprise.shreekhodalkotlin.common.SecureStorageManager.Companion.initSecureStorageManager
import com.zeel_enterprise.shreekhodalkotlin.common.SecureStorageManager.Companion.sharedPrefConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AdPlacerApplication(private val instance: Application) {

    val appOpenManager: AppOpenManager = AppOpenManager()
    val interstitialManager: InterstitialManager = InterstitialManager()
    val rewardManager: RewardAdManager = RewardAdManager()
    val nativeAdManager: NativeAdManager = NativeAdManager()
    var messagingCallback: MessagingCallback? = null
    private val homeButtonReceiver: HomeButtonReceiver = HomeButtonReceiver()
    private val handler = Handler(Looper.getMainLooper())

    init {
        adPlacerApplication = this
        initSecureStorageManager(instance)
        registerLifecycle()
        registerHomeButtonReceiver()
        initializeMobileAds()
        instance.pingSite()
    }

    private fun registerLifecycle(){
        instance.registerActivityLifecycleCallbacks(object :
            Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                activityStack.add(activity.localClassName)
                printActivityStack("Created")
            }

            override fun onActivityStarted(activity: Activity) {
            }

            override fun onActivityResumed(activity: Activity) {
            }

            override fun onActivityPaused(activity: Activity) {
            }

            override fun onActivityStopped(activity: Activity) {
            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
            }

            override fun onActivityDestroyed(activity: Activity) {
                activityStack.remove(activity.localClassName)
                printActivityStack("Destroyed")
            }
        })
    }

    private fun printActivityStack(event: String) {
        Log.d("ActivityLifecycle", "${activityStack.joinToString(" -> ")}")
    }

    private fun registerHomeButtonReceiver() {
        val filter = IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            instance.registerReceiver(homeButtonReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            instance.registerReceiver(homeButtonReceiver, filter)
        }
    }

    fun openAdInspector() {
        MobileAds.openAdInspector(runningActivity!!) { error ->
            // Error will be non-null if ad inspector closed due to an error.
            Logger.e(TAG, "opening openAdInspector  error    ${error?.message}")
        }
    }

    private fun initializeMobileAds() {
        val backgroundScope = CoroutineScope(Dispatchers.IO)
        backgroundScope.launch {
            // Initialize the Google Mobile Ads SDK on a background thread.
            AudienceNetworkAds.initialize(instance)
            MobileAds.initialize(instance) { initializationStatus ->


                Log.e(
                    TAG, "initializeMobileAds: DEVICE_ID_EMULATOR ${AdRequest.DEVICE_ID_EMULATOR}"
                )
                Log.e(TAG, "initializeMobileAds: testDeviceIds  ${testDeviceIds.toSet()}")
                testDeviceIds.add(AdRequest.DEVICE_ID_EMULATOR)
                Log.e(TAG, "initializeMobileAds: after testDeviceIds  ${testDeviceIds.toSet()}")
                val configuration =
                    RequestConfiguration.Builder().setTestDeviceIds(testDeviceIds).build()
                MobileAds.setRequestConfiguration(configuration)


                val statusMap = initializationStatus.adapterStatusMap
                for (adapterClass in statusMap.keys) {
                    val status = statusMap[adapterClass]
                    Logger.d(
                        "AdPlacerApplication", String.format(
                            "Adapter name: %s, Description: %s, Latency: %d",
                            adapterClass, status!!.description, status.latency
                        )
                    )
                }

                // Start loading ads here...
            }
        }
    }

    fun processLifecycleRegister(observer: LifecycleObserver?) {
        observer?.let {
            ProcessLifecycleOwner.get().lifecycle.addObserver(it)
        }
    }

    fun showAppOpenAd() {
        if (InterstitialManager.isAdShowing || RewardAdManager.isAdShowing || AppOpenManager.shouldStopAppOpen) {
            AppOpenManager.shouldStopAppOpen = false
            return
        }

        if (!isAppOpenEmpty()) {
            runningActivity?.let { activity ->
                appOpenManager.showAppOpen(activity, object : AdCallback {
                    override fun adDisplayedCallback(displayed: Boolean) {
                        if (isSplashRunning) {
                            isSplashRunning = false
                            handler.postDelayed({ continueAppFlow() }, 750)
                        }
                    }
                })
            }
        }
    }

    fun continueAppFlow() {
        messagingCallback?.startingTimerToChangeScreen()
        Logger.d(TAG, "continueAppFlow    -->  startingTimerToChangeScreen  -->    called")

        Logger.d(TAG, "continueAppFlow    -->   called")

        handler.removeCallbacksAndMessages(null)
        Logger.d(TAG, "continueAppFlow    -->   removed all callbacks and messages from handler")

        val activity = runningActivity
        if (activity == null) {
            Logger.d(TAG, "continueAppFlow    -->   runningActivity is null, returning")
            return
        }

        val appDetails = sharedPrefConfig.appDetails
        Logger.d(TAG, "continueAppFlow    -->   fetched appDetails: $appDetails")

        val whichScreenToGo = appDetails.whichScreenToGo
        Logger.d(TAG, "continueAppFlow    -->   whichScreenToGo: $whichScreenToGo")





        Logger.d(TAG, "continueAppFlow    -->   finished running activity")

        when {
            appDetails.howtousestart == STATUS.ON.name && !sharedPrefConfig.isHowToUseShowDone -> {
                Logger.e(TAG, "continueAppFlow    -->   opening HowToUseActivity")
                messagingCallback?.openHowToUseActivity()


            }

            whichScreenToGo.isNotEmpty() -> {
                when (whichScreenToGo) {
                    SCREENS.EXTRA_START.name -> {
                        Logger.e(TAG, "continueAppFlow    -->   opening ExtraStartActivity")
                        messagingCallback?.openExtraStartActivity()
                    }

                    SCREENS.START.name -> {
                        Logger.e(TAG, "continueAppFlow    -->   opening StartActivity")
                        messagingCallback?.openStartActivity()
                    }

                    else -> {
                        Logger.e(
                            TAG, "continueAppFlow    -->   opening HomeActivity (default case)"
                        )
                        messagingCallback?.openHomeActivity()
                    }
                }
            }

            else -> {
                Logger.e(TAG, "continueAppFlow    -->   opening HomeActivity (else case)")
                messagingCallback?.openHomeActivity()
            }
        }

        isSplashRunning = false
        Logger.e(TAG, "continueAppFlow    -->   set isSplashRunning to false")
    }

    private fun initializeAndFetchFirebaseConfig(
        onSuccess: (FirebaseRemoteConfig) -> Unit,
        onFailure: () -> Unit,
        retries: Int = 3,
        retryDelay: Long = 2000
    ) {
        val firebaseRemoteConfig = FirebaseRemoteConfig.getInstance()
        val configDefaults = mapOf("temp_response" to "default value")
        firebaseRemoteConfig.setDefaultsAsync(configDefaults)

        val configSettings =
            FirebaseRemoteConfigSettings.Builder().setMinimumFetchIntervalInSeconds(2).build()
        firebaseRemoteConfig.setConfigSettingsAsync(configSettings)

        firebaseRemoteConfig.fetchAndActivate().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                onSuccess(firebaseRemoteConfig)
            } else if (retries > 0) {
                handler.postDelayed({
                    initializeAndFetchFirebaseConfig(onSuccess, onFailure, retries - 1, retryDelay)
                }, retryDelay)
            } else {
                onFailure()
            }
        }
    }

    var isContinueFlowDone = false

    fun fetchApiResponse(whichResponse: TYPE_OF_RESPONSE) {
        Logger.d(TAG, "fetchApiResponse    -->   called with whichResponse = ${whichResponse.name}")

        if (wantToByPassResponse && sharedPrefConfig.isResponseGot) {
            Logger.d(
                TAG,
                "fetchApiResponse    -->   wantToByPassResponse   calling   -->   startTimerForContinueFlow"
            )
            startTimerForContinueFlow(0)
            isContinueFlowDone = true
        }

        Logger.d(TAG, "fetchApiResponse    -->   initializing and fetching Firebase config")

        initializeAndFetchFirebaseConfig({ firebaseConfig ->
            Logger.w(TAG, "fetchApiResponse    -->   got Firebase response")
            messagingCallback?.gotFirebaseResponse(firebaseConfig)

            if (whichResponse == TYPE_OF_RESPONSE.GOOGLE) {
                val response = firebaseConfig.getString(whichResponse.value)
                Logger.w(TAG, "fetchApiResponse    -->   Firebase response for GOOGLE = $response")
                val apiResponse: ApiResponse = Gson().fromJson(response, ApiResponse::class.java)
                handleSuccessfulApiResponse(apiResponse)
            } else {
                LIBRARY_PACKAGE_NAME = firebaseConfig.getString("LIBRARY_PACKAGE_NAME")
                AuthorizationADS = firebaseConfig.getString("AuthorizationADS")
                BASE_URL_API = firebaseConfig.getString("BaseUrl")

                Logger.d(
                    TAG,
                    "fetchApiResponse    -->   LIBRARY_PACKAGE_NAME = $LIBRARY_PACKAGE_NAME"
                )
                Logger.d(TAG, "fetchApiResponse    -->   AuthorizationADS = $AuthorizationADS")
                Logger.d(TAG, "fetchApiResponse    -->   BASE_URL_API = $BASE_URL_API")

                val jsonBody = JSONObject().apply {
                    put("appid", LIBRARY_PACKAGE_NAME)
                    put("secretKey", AuthorizationADS)
                }
                val requestBody = jsonBody.toString()
                    .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
                val call = AdApiClient().client.create(ApiInterface::class.java).getAll(requestBody)

                Logger.d(
                    TAG,
                    "fetchApiResponse    -->   making API call with requestBody = $jsonBody"
                )

                call?.enqueue(object : Callback<ApiResponse?> {
                    override fun onResponse(
                        call: Call<ApiResponse?>, response: Response<ApiResponse?>
                    ) {
                        if (response.isSuccessful && response.body() != null) {
                            Logger.w(
                                TAG,
                                "fetchApiResponse    -->   API response successful: ${response.body()}"
                            )
                            handleSuccessfulApiResponse(response.body()!!)
                        } else {
                            Logger.d(
                                TAG, "fetchApiResponse    -->   API response failure: ${
                                    response.errorBody()?.string()
                                }"
                            )
                            onFailureResponse()
                        }
                    }

                    override fun onFailure(call: Call<ApiResponse?>, t: Throwable) {
                        Logger.d(TAG, "fetchApiResponse    -->   API call failed: ${t.message}")
                        onFailureResponse()
                    }
                })
            }
        }, {
            Logger.d(
                TAG, "fetchApiResponse    -->   Firebase config fetch failed, exiting application"
            )
            messagingCallback?.exitTheApplication()
        })
    }


    private fun onFailureResponse() {
        startTimerForContinueFlow(2000)
        checkAndShowAdIfAvailable()
    }

    private fun handleSuccessfulApiResponse(response: ApiResponse) {
        Logger.d(TAG, "handleSuccessfulApiResponse    -->   called with response: $response")
        try {
            val currentVersion = getCurrentAppVersion(runningActivity)
            Logger.d(TAG, "handleSuccessfulApiResponse    -->   currentVersion: $currentVersion")

            val requiresForceUpdate =
                response.appDetails.forceUpdateVersions.contains(currentVersion)
            Logger.d(
                TAG,
                "handleSuccessfulApiResponse    -->   requiresForceUpdate: $requiresForceUpdate"
            )

            val requiresUpdate = response.appDetails.updateRequiredVersions.contains(currentVersion)
            Logger.d(TAG, "handleSuccessfulApiResponse    -->   requiresUpdate: $requiresUpdate")

            sharedPrefConfig.isResponseGot = true
            Logger.d(TAG, "handleSuccessfulApiResponse    -->   set isResponseGot to true")

            if (requiresForceUpdate || requiresUpdate) {
                Logger.d(
                    TAG,
                    "handleSuccessfulApiResponse    -->   update required, removing callbacks and prompting for update"
                )
                handler.removeCallbacksAndMessages(null)
                promptForUpdate(
                    runningActivity,
                    "Update Required",
                    "Please update the app to the latest version.",
                    "Later",
                    "Update",
                    response,
                    requiresUpdate
                )
            } else {
                Logger.d(
                    TAG,
                    "handleSuccessfulApiResponse    -->   no update required, saving response and continuing flow"
                )
                saveApiResponse(response)
                startTimerForContinueFlow(0)
                checkAndShowAdIfAvailable()
            }
        } catch (e: Exception) {
            Logger.e(TAG, "handleSuccessfulApiResponse    -->   exception occurred: ${e.message}")
            startTimerForContinueFlow(2000)
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
                GlobalUtils().openLinkInBrowser(
                    activity!!,
                    "https://play.google.com/store/apps/details?id=$appPackageName"
                )
            }

            override fun onNegativeClicked(dialog: Dialog) {
                handleDialogDismissOrCancel(response, requiresUpdate)
            }

            override fun onDialogCancelled() {
                handleDialogDismissOrCancel(response, requiresUpdate)
            }

            override fun onDialogDismissed() {
                handleDialogDismissOrCancel(response, requiresUpdate)
            }
        }

        val currentVersion = getCurrentAppVersion(activity)
        val forceUpdateRequired = response.appDetails.forceUpdateVersions.contains(currentVersion)
        DialogUtil.createSimpleDialog(
            activity = activity,
            title = title,
            description = description,
            negativeButtonText = negativeButtonText,
            positiveButtonText = positiveButtonText,
            dialogCallback = dialogCallback,
            isCancelable = forceUpdateRequired
        )
    }

    private fun handleDialogDismissOrCancel(response: ApiResponse, requiresUpdate: Boolean) {
        saveApiResponse(response)
        if (!requiresUpdate) {
            checkAndShowAdIfAvailable()
        }
    }

    private fun checkAndShowAdIfAvailable() {
        if (!InterstitialManager.isAdShowing && !RewardAdManager.isAdShowing) {
            startTimerForContinueFlow(0)
        } else if (isSplashRunning) {
            handler.removeCallbacksAndMessages(null)
            isSplashRunning = false
        }

        if (preLoadInterstitial) {
            interstitialManager.preloadInterstitialAd(runningActivity!!)
        }
        if (preLoadNative) {
            nativeAdManager.loadNativeAd(runningActivity!!)
        }
        if (preLoadReward) {
            rewardManager.preloadRewardAd(runningActivity!!)
        }

    }

    private fun saveApiResponse(response: ApiResponse) {
        sharedPrefConfig.apiResponse = (response)
        messagingCallback?.savingApiResponse()
    }

    private fun getCurrentAppVersion(activity: Activity?): String {
        return try {
            val packageInfo = activity?.packageManager?.getPackageInfo(activity.packageName, 0)
            packageInfo?.versionName ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    fun showInternetDialog() {
        if (AppOpenManager.appOpenAd != null) {
            return
        }
        messagingCallback?.showNetworkDialog()
    }

    fun registerMessagingCallback(instanceCallback: MessagingCallback?) {
        messagingCallback = instanceCallback
    }

    fun startTimerForContinueFlow(delay: Long) {
        if (!isContinueFlowDone) {
            handler.postDelayed({ continueAppFlow() }, delay)
        }
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

    companion object {


        fun initialize(app: Application): AdPlacerApplication {
            return adPlacerApplication ?: synchronized(this) {
                adPlacerApplication ?: AdPlacerApplication(app).also { adPlacerApplication = it }
            }
        }

        fun getInstance(): AdPlacerApplication {
            return adPlacerApplication
                ?: throw IllegalStateException("AdPlacerApplication is not initialized")
        }
    }
}
