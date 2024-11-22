package com.harshil258.adplacer.app

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.DialogInterface
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
import com.harshil258.adplacer.utils.Constants.preLoadAppopen
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
    private lateinit var appUpdateManager: AppUpdateManager

    init {
        adPlacerApplication = this
        initSecureStorageManager(instance)
        registerLifecycle()
        registerHomeButtonReceiver()
        initializeMobileAds()
        instance.pingSite()
        appUpdateManager =
            AppUpdateManagerFactory.create(adPlacerApplication.instance.applicationContext)
    }

    private fun registerLifecycle() {
        instance.registerActivityLifecycleCallbacks(object :
            Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                activityStack.add(activity)
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
                activityStack.remove(activity)
                printActivityStack("Destroyed")
            }
        })
    }


    private fun printActivityStack(event: String) {
        val activityNames = activityStack.map { it::class.java.simpleName }
        Log.d("ActivityLifecycle", "$event: ${activityNames.joinToString(" -> ")}")
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
            return
        }

        if (!isAppOpenEmpty()) {
            runningActivity?.let { activity ->
                appOpenManager.showAppOpen(activity, object : AdCallback {
                    override fun adDisplayedCallback(displayed: Boolean) {
                        if (isSplashRunning) {
                            isSplashRunning = false
                            continueAppFlow()
                        }
                    }
                })
            }
        }
    }

    fun continueAppFlow() {
        messagingCallback?.startingTimerToChangeScreen()
        handler.removeCallbacksAndMessages(null)

        Log.d(TAG, "continueAppFlow:1212121212 ")

        val activity = runningActivity ?: return

        val appDetails = sharedPrefConfig.appDetails
        val whichScreenToGo = appDetails.whichScreenToGo

        when {
            appDetails.howtousestart == STATUS.ON.name && !sharedPrefConfig.isHowToUseShowDone -> {
                messagingCallback?.openHowToUseActivity()
            }

            whichScreenToGo.isNotEmpty() -> {
                when (whichScreenToGo) {
                    SCREENS.EXTRA_START.name -> messagingCallback?.openExtraStartActivity()
                    SCREENS.START.name -> messagingCallback?.openStartActivity()
                    else -> messagingCallback?.openHomeActivity()
                }
            }

            else -> {
                messagingCallback?.openHomeActivity()
            }
        }

        isSplashRunning = false
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

        val configSettings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(2)
            .build()
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
        if (wantToByPassResponse && sharedPrefConfig.isResponseGot) {
            startTimerForContinueFlow(0)
            isContinueFlowDone = true
        }

        initializeAndFetchFirebaseConfig({ firebaseConfig ->
            messagingCallback?.gotFirebaseResponse(firebaseConfig)

            if (whichResponse == TYPE_OF_RESPONSE.GOOGLE) {
                val response = firebaseConfig.getString(whichResponse.value)
                val apiResponse: ApiResponse = Gson().fromJson(response, ApiResponse::class.java)
                handleSuccessfulApiResponse(apiResponse)
            } else {
                LIBRARY_PACKAGE_NAME = firebaseConfig.getString("LIBRARY_PACKAGE_NAME")
                AuthorizationADS = firebaseConfig.getString("AuthorizationADS")
                BASE_URL_API = firebaseConfig.getString("BaseUrl")

                val jsonBody = JSONObject().apply {
                    put("appid", LIBRARY_PACKAGE_NAME)
                    put("secretKey", AuthorizationADS)
                }
                val requestBody = jsonBody.toString()
                    .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
                val call = AdApiClient().client.create(ApiInterface::class.java).getAll(requestBody)

                call?.enqueue(object : Callback<ApiResponse?> {
                    override fun onResponse(
                        call: Call<ApiResponse?>, response: Response<ApiResponse?>
                    ) {
                        if (response.isSuccessful && response.body() != null) {
                            handleSuccessfulApiResponse(response.body()!!)
                        } else {
                            onFailureResponse()
                        }
                    }

                    override fun onFailure(call: Call<ApiResponse?>, t: Throwable) {
                        onFailureResponse()
                    }
                })
            }
        }, {
            messagingCallback?.exitTheApplication()
        })
    }

    private fun onFailureResponse() {
        startTimerForContinueFlow(2000)
        preLoadAllNeededAds()
    }

    private fun handleSuccessfulApiResponse(response: ApiResponse) {
        try {
            val currentVersion = getCurrentAppVersion(runningActivity)
            val requiresForceUpdate =
                response.appDetails.forceUpdateVersions.contains(currentVersion)
            val requiresUpdate =
                !response.appDetails.noUpdateRequiredVersion.contains(currentVersion)
            Log.d(TAG, "handleSuccessfulApiResponse: response  ${Gson().toJson(response)}")
            Log.d(
                TAG,
                "handleSuccessfulApiResponse: requiresUpdate  ${requiresUpdate}   forceUpdateVersions   ${response.appDetails.forceUpdateVersions}"
            )
            Log.d(
                TAG,
                "handleSuccessfulApiResponse: requiresUpdate  ${requiresUpdate}   update   ${response.appDetails.noUpdateRequiredVersion}"
            )
            sharedPrefConfig.isResponseGot = true
            saveApiResponse(response)

            if (requiresForceUpdate || requiresUpdate) {
                handler.removeCallbacksAndMessages(null)

                val isCancelable = !requiresForceUpdate
                val appUpdateInfoTask = appUpdateManager.appUpdateInfo

                appUpdateInfoTask.addOnSuccessListener { appUpdateInfo: AppUpdateInfo ->
                    when {
                        // Check if an update is available and it's flexible or immediate
                        appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE -> {
                            promptForUpdate(
                                activity = runningActivity,
                                title = "🔄 Update Available!",
                                description = if (requiresForceUpdate) {
                                    "🚀 A new version is here with important updates and improvements. Please update now to continue using the app seamlessly."
                                } else {
                                    "✨ We’ve made some exciting improvements! Update now to enjoy the latest features and a smoother experience. You can skip for now, but we recommend updating."
                                },
                                negativeButtonText = if (requiresForceUpdate) "" else "Later",
                                positiveButtonText = "Update Now 🚀",
                                response = response,
                                isCancelable = isCancelable,
                                negativeCallback = {
                                    if (!requiresForceUpdate) {
                                        preLoadAllNeededAds()
                                    }
                                    startTimerForContinueFlow(0)
                                }
                            )
                        }

                        appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_NOT_AVAILABLE -> {
                            if (!requiresForceUpdate) {
                                preLoadAllNeededAds()
                            }
                            startTimerForContinueFlow(0)
                        }
                    }
                }


            } else {
                startTimerForContinueFlow(0)
                preLoadAllNeededAds()
            }
        } catch (e: Exception) {
            startTimerForContinueFlow(2000)
            preLoadAllNeededAds()
        }
    }

    private fun promptForUpdate(
        activity: Activity?,
        title: String,
        description: String,
        negativeButtonText: String,
        positiveButtonText: String,
        response: ApiResponse,
        isCancelable: Boolean,
        negativeCallback: () -> Unit
    ) {
        val dialogCallback = object : DialogCallBack {
            override fun onPositiveClicked(dialog: DialogInterface) {
                activity?.let {
                    val appPackageName = it.packageName
                    GlobalUtils().openLinkInBrowser(
                        it,
                        "https://play.google.com/store/apps/details?id=$appPackageName"
                    )
                }
            }

            override fun onNegativeClicked(dialog: DialogInterface) {
                if (isCancelable) {
                    negativeCallback()
                } else {
                    messagingCallback?.exitTheApplication()
                }
            }

            override fun onDialogCancelled() {
                if (isCancelable) {
                    negativeCallback()
                } else {
                    messagingCallback?.exitTheApplication()
                }
            }

            override fun onDialogDismissed() {
                if (isCancelable) {
                    negativeCallback()
                } else {
                    messagingCallback?.exitTheApplication()
                }
            }
        }

        DialogUtil.createMaterialSimpleDialog(
            activity = activity,
            title = title,
            description = description,
            negativeButtonText = negativeButtonText,
            positiveButtonText = positiveButtonText,
            dialogCallback = dialogCallback,
            isCancelable = isCancelable
        )
//        DialogUtil.createSimpleDialog(
//            activity = activity,
//            title = title,
//            description = description,
//            negativeButtonText = negativeButtonText,
//            positiveButtonText = positiveButtonText,
//            dialogCallback = dialogCallback,
//            isCancelable = isCancelable
//        )
    }


    private fun preLoadAllNeededAds() {


        runningActivity?.let { currentActivity ->
            if (preLoadInterstitial) {
                interstitialManager.preloadInterstitialAd(
                    currentActivity
                )
            }
            if (preLoadNative) {
                nativeAdManager.loadNativeAd(currentActivity)
            }
            if (preLoadReward) {
                rewardManager.preloadRewardAd(currentActivity)
            }

            if (preLoadAppopen) {
                appOpenManager.loadAppOpen(currentActivity, object : AdCallback {
                    override fun adDisplayedCallback(displayed: Boolean) {}
                })
            }


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
