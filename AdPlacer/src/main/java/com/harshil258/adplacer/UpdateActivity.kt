package com.harshil258.adplacer

import android.app.Activity
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.UpdateAvailability
import com.harshil258.adplacer.app.AdPlacerApplication
import com.harshil258.adplacer.interfaces.DialogCallBack
import com.harshil258.adplacer.utils.Constants
import com.harshil258.adplacer.utils.DialogUtil
import com.harshil258.adplacer.utils.GlobalUtils
import com.harshil258.adplacer.utils.Logger.TAG
import com.harshil258.adplacer.utils.sharedpreference.SecureStorageManager.Companion.sharedPrefConfig

class UpdateActivity : AppCompatActivity() {
    private lateinit var appUpdateManager: AppUpdateManager


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_update)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        appUpdateManager = AppUpdateManagerFactory.create(this)
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo

        val currentVersion = getCurrentAppVersion(Constants.currentActivity)

        val requiresForceUpdate =
            sharedPrefConfig.appDetails.forceUpdateVersions.contains(currentVersion)
        val isCancelable = !requiresForceUpdate

        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo: AppUpdateInfo ->
            when {
                // Check if an update is available and it's flexible or immediate
                appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE -> {

                    Logger.d(
                        TAG, "12121212   handleSuccessfulApiResponse: UPDATE_AVAILABLE"
                    )
                    promptForUpdate(activity = Constants.currentActivity,
                        title = "🔄 Update Available!",
                        description = if (requiresForceUpdate) {
                            "🚀 A new version is here with important updates and improvements. Please update now to continue using the app seamlessly."
                        } else {
                            "✨ We’ve made some exciting improvements! Update now to enjoy the latest features and a smoother experience. You can skip for now, but we recommend updating."
                        },
                        negativeButtonText = if (requiresForceUpdate) "" else "Later",
                        positiveButtonText = "Update Now 🚀",
                        isCancelable = isCancelable,
                        negativeCallback = {
                            finish()
                            if (!requiresForceUpdate) {
                                AdPlacerApplication.getInstance().preloadAllAds()
                            }
                            AdPlacerApplication.getInstance().startContinueFlowTimer(0)
                        })
                }

                else -> {
                    Logger.d(
                        TAG, "12121212   handleSuccessfulApiResponse: else"
                    )
                    if (!requiresForceUpdate) {
                        AdPlacerApplication.getInstance().preloadAllAds()
                    }
                    AdPlacerApplication.getInstance().startContinueFlowTimer(0)
                }
            }
        }.addOnFailureListener {
            if (!requiresForceUpdate) {
                AdPlacerApplication.getInstance().preloadAllAds()
            }
            AdPlacerApplication.getInstance().startContinueFlowTimer(0)
        }

    }

    private fun promptForUpdate(
        activity: Activity?,
        title: String,
        description: String,
        negativeButtonText: String,
        positiveButtonText: String,
        isCancelable: Boolean,
        negativeCallback: () -> Unit
    ) {
        val dialogCallback = object : DialogCallBack {
            override fun onPositiveClicked(dialog: DialogInterface) {
                activity?.let {
                    val appPackageName = it.packageName
                    Logger.d(TAG, "onPositiveClicked: appPackageName ${appPackageName}")
                    GlobalUtils().openLinkInBrowser(
                        it, "https://play.google.com/store/apps/details?id=$appPackageName"
                    )
                }
            }

            override fun onNegativeClicked(dialog: DialogInterface) {
                if (isCancelable) {
                    negativeCallback()
                } else {
                    Constants.currentActivity?.finishAffinity()
                }
            }

            override fun onDialogCancelled() {
                if (isCancelable) {
                    negativeCallback()
                } else {
                    Constants.currentActivity?.finishAffinity()
                }
            }

            override fun onDialogDismissed() {
                if (isCancelable) {
                    negativeCallback()
                } else {
                    Constants.currentActivity?.finishAffinity()
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

    private fun getCurrentAppVersion(activity: Activity?): String {
        return try {
            val packageInfo = activity?.packageManager?.getPackageInfo(activity.packageName, 0)
            packageInfo?.versionName ?: ""
        } catch (e: Exception) {
            ""
        }
    }

}