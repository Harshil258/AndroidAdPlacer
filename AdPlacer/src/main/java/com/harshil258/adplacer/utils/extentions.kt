package com.harshil258.adplacer.utils

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.gson.Gson
import com.harshil258.adplacer.models.NATIVE_SIZE
import com.harshil258.adplacer.utils.Constants.pingUrl
import com.harshil258.adplacer.utils.Constants.runningActivity
import com.zeel_enterprise.shreekhodalkotlin.common.SecureStorageManager.Companion.sharedPrefConfig

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

enum class STATUS {
    ON, OFF
}

fun Context.pingSite() {
    // Create an OkHttpClient instance
    val client = OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS).build()

    // Build the request
    val request = Request.Builder().url(pingUrl).build()

    // Execute the request
    client.newCall(request).enqueue(object : okhttp3.Callback {
        override fun onFailure(call: okhttp3.Call, e: IOException) {}

        override fun onResponse(call: okhttp3.Call, response: Response) {
            if (response.isSuccessful) {
                println("Ping successful! Response code: ${response.code}")
            } else {
                println("Ping failed with response code: ${response.code}")
            }
        }
    })
}

object extentions {

    fun isAdStatusOn(): Boolean {
        return runningActivity?.let { sharedPrefConfig.appDetails.adStatus } == STATUS.ON.name
    }

    fun isNativeEmpty(): Boolean {
        Logger.e("TAG1232", "TAG1232  isAdStatusOn: ${isAdStatusOn()}")
        Logger.e("TAG1232", "TAG1232  isNativeEmpty: ${
            runningActivity?.let { sharedPrefConfig.appDetails.admobNativeAd }
                .isNullOrEmpty() || !isAdStatusOn()
        }")
        return runningActivity?.let { sharedPrefConfig.appDetails.admobNativeAd }
            .isNullOrEmpty() || !isAdStatusOn()
    }

    fun isInterstitialEmpty(): Boolean {
        return runningActivity?.let { sharedPrefConfig.appDetails.admobInterstitialAd }
            .isNullOrEmpty() || !isAdStatusOn()
    }

    fun isRewardEmpty(): Boolean {
        return runningActivity?.let { sharedPrefConfig.appDetails.admobRewardAd }
            .isNullOrEmpty() || !isAdStatusOn()
    }

    fun isAppOpenEmpty(): Boolean {
        return runningActivity?.let { sharedPrefConfig.appDetails.admobAppOpenAd }
            .isNullOrEmpty() || !isAdStatusOn()
    }

    fun isBannerEmpty(): Boolean {
        return runningActivity?.let { sharedPrefConfig.appDetails.admobBannerAd }
            .isNullOrEmpty() || !isAdStatusOn()
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

fun <A> String.fromJson(type: Class<A>): A {
    return Gson().fromJson(this, type)
}

fun <A> A.toJson(): String? {
    return Gson().toJson(this)
}

