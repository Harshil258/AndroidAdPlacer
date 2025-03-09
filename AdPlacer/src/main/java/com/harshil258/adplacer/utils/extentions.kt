package com.harshil258.adplacer.utils

import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.core.content.ContextCompat
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.gson.Gson
import com.harshil258.adplacer.models.NATIVE_SIZE
import com.harshil258.adplacer.utils.sharedpreference.SecureStorageManager.Companion.sharedPrefConfig
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

enum class AdStatus {
    ON, OFF
}

/**
 * Pings the configured URL using OkHttp.
 */
fun Context.pingSite() {
    val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    val request = Request.Builder().url(Constants.pingUrl).build()

    client.newCall(request).enqueue(object : okhttp3.Callback {
        override fun onFailure(call: okhttp3.Call, e: IOException) {
            // Optionally log failure here.
            Logger.e("PingSite", "Ping failed: ${e.message}")
        }

        override fun onResponse(call: okhttp3.Call, response: Response) {
            if (response.isSuccessful) {
                println("Ping successful! Response code: ${response.code}  Response: $response")
            } else {
                println("Ping failed with response code: ${response.code}")
            }
        }
    })
}

/**
 * Checks if all specified permissions are granted.
 */
fun Context.areAllPermissionsGranted(permissions: Array<String>): Boolean {
    for (permission in permissions) {
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            return false
        }
    }
    return true
}

object Extensions {

    fun isAdStatusEnabled(): Boolean {
        // Returns true only if the ad status is ON.
        return Constants.currentActivity?.let { sharedPrefConfig.appDetails.adStatus } == AdStatus.ON.name
    }

    fun isNativeAdEmpty(): Boolean {
        val isEmpty = Constants.currentActivity?.let { sharedPrefConfig.appDetails.admobNativeAd }.isNullOrEmpty()
        Logger.e("TAG1232", "isAdStatusEnabled: ${isAdStatusEnabled()}")
        Logger.e("TAG1232", "isNativeAdEmpty: $isEmpty or not enabled: ${!isAdStatusEnabled()}")
        return isEmpty || !isAdStatusEnabled()
    }

    fun isInterstitialAdEmpty(): Boolean {
        return Constants.currentActivity?.let { sharedPrefConfig.appDetails.admobInterstitialAd }
            .isNullOrEmpty() || !isAdStatusEnabled()
    }

    fun isRewardAdEmpty(): Boolean {
        return Constants.currentActivity?.let { sharedPrefConfig.appDetails.admobRewardAd }
            .isNullOrEmpty() || !isAdStatusEnabled()
    }

    fun isAppOpenAdEmpty(): Boolean {
        return Constants.currentActivity?.let { sharedPrefConfig.appDetails.admobAppOpenAd }
            .isNullOrEmpty() || !isAdStatusEnabled()
    }

    fun isBannerAdEmpty(): Boolean {
        return Constants.currentActivity?.let { sharedPrefConfig.appDetails.admobBannerAd }
            .isNullOrEmpty() || !isAdStatusEnabled()
    }

    fun getNativeAdSizeBigOrSmall(): NATIVE_SIZE {
        val appDetails = Constants.currentActivity?.let { sharedPrefConfig.appDetails }
        return when (appDetails?.nativeBigOrSmall) {
            "LARGE" -> NATIVE_SIZE.LARGE
            "SMALL" -> NATIVE_SIZE.SMALL
            else -> NATIVE_SIZE.LARGE
        }
    }

    fun getNativeAdSizeMediumOrSmall(): NATIVE_SIZE {
        val appDetails = Constants.currentActivity?.let { sharedPrefConfig.appDetails }
        return when (appDetails?.nativeMediumOrSmall) {
            "MEDIUM" -> NATIVE_SIZE.MEDIUM
            "SMALL" -> NATIVE_SIZE.SMALL
            else -> NATIVE_SIZE.MEDIUM
        }
    }

    /**
     * Logs a custom event with Firebase Analytics.
     */
    fun logCustomEvent(context: Context, eventName: String, parameterName: String, parameterValue: String) {
        val firebaseAnalytics = FirebaseAnalytics.getInstance(context)
        val params = Bundle().apply {
            putString(parameterName, parameterValue)
        }
        firebaseAnalytics.logEvent("$eventName$parameterName$parameterValue", params)
    }
}

/**
 * Extension function to deserialize JSON strings into objects.
 */
fun <T> String.fromJson(type: Class<T>): T {
    return Gson().fromJson(this, type)
}

/**
 * Extension function to serialize an object into a JSON string.
 */
fun <T> T.toJson(): String {
    return Gson().toJson(this)
}

