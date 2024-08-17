package com.harshil258.adplacer.utils


import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.harshil258.adplacer.models.AdsDetails
import com.harshil258.adplacer.models.ApiResponse
import com.harshil258.adplacer.models.AppDetails
import com.zeel_enterprise.shreekhodalkotlin.common.SecureStorageManager

//class SharedPrefConfig(private val context: Context) {
//    private val prefs: SharedPreferences? =
//        context.getSharedPreferences("H_VEKARIYA", Context.MODE_PRIVATE)
//    private val gson = Gson()
//
//
//    companion object {
//    }
//
//    var isLoadingFirstTime: Boolean
//        get() = prefs.load("isLoadingFirstTime", false)
//        set(v) = prefs.save("isLoadingFirstTime", true)
//
//
//    private inline fun <reified T> SharedPreferences?.save(key: String, value: T) {
//        this?.edit()?.putString(key, gson.toJson(value))?.apply()
//    }
//
//    private inline fun <reified T> SharedPreferences?.load(key: String, defaultValue: T): T {
//        val json = this?.getString(key, null)
//        return if (json != null) {
//            gson.fromJson(json, T::class.java)
//        } else {
//            defaultValue
//        }
//    }
//
//    var isHowToUseShowDone: Boolean
//        get() = prefs.load("isHowToUseShowDone", false)
//        set(value) = prefs.save("isHowToUseShowDone", value)
//
//    var apiResponse: ApiResponse
//        get() = prefs.load("API_RESPONSE", ApiResponse())
//        set(value) = prefs.save("API_RESPONSE", value)
//
//    var appDetails: AppDetails
//        get() = apiResponse.appDetails
//        set(value) {
//            val updatedApiResponse = apiResponse.copy(appDetails = value)
//            Logger.e("rdhdhsdthjtg", ": ${updatedApiResponse.appDetails.adStatus}")
//            apiResponse = updatedApiResponse
//        }
//
//    var adsDetails: AdsDetails
//        get() = apiResponse.adsDetails
//        set(value) {
//            val updatedApiResponse = apiResponse.copy(adsDetails = value)
//            apiResponse = updatedApiResponse
//        }
//
//    fun getStringPref(key: String, defaultValue: String?): String {
//        return prefs?.getString(key, defaultValue).toString()
//    }
//
//    fun setStringPref(key: String, value: String?) {
//        prefs?.edit()?.putString(key, value)?.apply()
//    }
//}
//
