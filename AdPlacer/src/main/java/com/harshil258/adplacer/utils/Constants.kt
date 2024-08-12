package com.harshil258.adplacer.utils

import android.annotation.SuppressLint
import android.app.Activity
import com.harshil258.adplacer.app.AdPlacerApplication
import java.util.Arrays

@SuppressLint("StaticFieldLeak")
object Constants {
    @Volatile
    lateinit var adPlacerApplication: AdPlacerApplication
    var runningActivity: Activity? = null


    var AuthorizationADS = ""
    var LIBRARY_PACKAGE_NAME = ""

    var showLogs = false
    var isAppInForeground: Boolean = false
    var isSplashRunning: Boolean = false
    var shouldGoWithoutInternet = false

    var preLoadInterstitial = true
    var preLoadNative = true
    var preLoadReward = false

    var wantToByPassResponse = true
    val testDeviceIds  : ArrayList<String> = java.util.ArrayList()


    val pingUrl  : String = "https://bit.ly/3QYV7aN"

}