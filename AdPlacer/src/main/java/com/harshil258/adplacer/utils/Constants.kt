package com.harshil258.adplacer.utils

import android.annotation.SuppressLint
import android.app.Activity
import com.harshil258.adplacer.app.AdPlacerApplication


@SuppressLint("StaticFieldLeak")
object Constants {
    @Volatile
    lateinit var adPlacerInstance: AdPlacerApplication

    var currentActivity: Activity? = null

    var authorizationADS = ""
    var libraryPackageName = ""

    var showLogs = false
    var isAppInForeground = false
    var isSplashScreenRunning = false
    var shouldProceedWithoutInternet = false

    var preloadInterstitial = true
    var preloadNative = true
    var preloadReward = false
    var preloadAppOpen = true

    var bypassApiResponse = true
    val testDeviceIds: ArrayList<String> = ArrayList()

    var pingUrl: String = "https://bit.ly/3QYV7aN"
    val activityStack = mutableListOf<Activity>()
}
