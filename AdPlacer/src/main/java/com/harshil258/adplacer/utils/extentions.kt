package com.harshil258.adplacer.utils

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.harshil258.adplacer.models.NATIVE_SIZE
import com.harshil258.adplacer.utils.Constants.runningActivity
import com.harshil258.adplacer.utils.SharedPrefConfig.Companion.sharedPrefConfig


enum class STATUS {
    ON, OFF
}



object extentions {

    fun isAdStatusOn(): Boolean {
        return runningActivity?.let { sharedPrefConfig.appDetails.adStatus } == STATUS.ON.name
    }


    fun isNativeEmpty(): Boolean {
        return runningActivity?.let { sharedPrefConfig.appDetails.admobNativeAd }.isNullOrEmpty()
    }

    fun isInterstitialEmpty(): Boolean {
        return runningActivity?.let { sharedPrefConfig.appDetails.admobInterstitialAd }
            .isNullOrEmpty()
    }

    fun isRewardEmpty(): Boolean {
        return runningActivity?.let { sharedPrefConfig.appDetails.admobRewardAd }.isNullOrEmpty()
    }

    fun isAppOpenEmpty(): Boolean {
        return runningActivity?.let { sharedPrefConfig.appDetails.admobAppOpenAd }.isNullOrEmpty()
    }

    fun isBannerEmpty(): Boolean {
        return runningActivity?.let { sharedPrefConfig.appDetails.admobBannerAd }.isNullOrEmpty()
    }

    fun nativeAdSizeBigOrSmall(): NATIVE_SIZE {
        val appDetails = runningActivity?.let { sharedPrefConfig.appDetails }

        return when {
            appDetails?.nativeBigOrSmall == "LARGE" -> NATIVE_SIZE.LARGE
            appDetails?.nativeBigOrSmall == "SMALL" -> NATIVE_SIZE.SMALL
            else -> NATIVE_SIZE.LARGE
        }
    }


    fun nativeAdSizeMediumOrSmall(): NATIVE_SIZE {
        val appDetails = runningActivity?.let { sharedPrefConfig.appDetails }

        return when {
            appDetails?.nativeMediumOrSmall == "MEDIUM" -> NATIVE_SIZE.MEDIUM
            appDetails?.nativeMediumOrSmall == "SMALL" -> NATIVE_SIZE.SMALL
            else -> NATIVE_SIZE.MEDIUM
        }
    }

    fun LogCustomEvent(
        context: Context, eventName: String, parameterName: String, parameterValue: String
    ) {
        val firebaseAnalytics = FirebaseAnalytics.getInstance(context)
        val params = Bundle().apply {
            putString(parameterName, parameterValue)
        }
        firebaseAnalytics.logEvent("$eventName$parameterName$parameterValue", params)
    }



}


