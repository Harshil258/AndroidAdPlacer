package com.zeel_enterprise.shreekhodalkotlin.common

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.harshil258.adplacer.models.AdsDetails
import com.harshil258.adplacer.models.ApiResponse
import com.harshil258.adplacer.models.AppDetails
import com.harshil258.adplacer.utils.Logger

interface ISecureStorageManager {
    var apiResponse: ApiResponse
    var appDetails: AppDetails
    var adsDetails: AdsDetails

    fun clearAll()
}

class SecureStorageManager(
    context: Context,
) : ISecureStorageManager {
    companion object {
        private const val IS_LOADING_FIRST_TIME = "isLoadingFirstTime"
        private const val IS_HOW_TO_USE_SHOW_DONE = "isHowToUseShowDone"
        private const val API_RESPONSE = "API_RESPONSE"
        private const val IS_RESPONSE_GOT = "isResponseGot"

        @SuppressLint("StaticFieldLeak")
        lateinit var sharedPrefConfig: SecureStorageManager

        fun initSecureStorageManager(context: Context){
            sharedPrefConfig = SecureStorageManager(context)
        }
    }

    private val encryptedPrefs: SharedPreferences = context.getSharedPreferences("H_VEKARIYA", 0)

    var isLoadingFirstTime: Boolean by BooleanPreferenceDelegate(
        encryptedPrefs, IS_LOADING_FIRST_TIME
    )

    var isHowToUseShowDone: Boolean by BooleanPreferenceDelegate(
        encryptedPrefs, IS_HOW_TO_USE_SHOW_DONE
    )

    fun getStringPref(key: String, defaultValue: String?): String {
        return encryptedPrefs.getString(key, defaultValue).toString()
    }
    fun setStringPref(key: String, value: String?) {
        encryptedPrefs.edit()?.putString(key, value)?.apply()
    }

    var isResponseGot: Boolean  = encryptedPrefs.getBoolean(IS_HOW_TO_USE_SHOW_DONE, false)

    override var apiResponse: ApiResponse by DataModelPreferenceDelegate(
        encryptedPrefs, API_RESPONSE, ApiResponse::class.java, ApiResponse()
    )

    override var adsDetails: AdsDetails
        get() = apiResponse.adsDetails
        set(value) {
            val updatedApiResponse = apiResponse.copy(adsDetails = value)
            apiResponse = updatedApiResponse
        }

    override var appDetails: AppDetails
        get() = apiResponse.appDetails
        set(value) {
            val updatedApiResponse = apiResponse.copy(appDetails = value)
            Logger.e("rdhdhsdthjtg", ": ${updatedApiResponse.appDetails.adStatus}")
            apiResponse = updatedApiResponse
        }

    override fun clearAll() {
        encryptedPrefs.edit {
            clear()
        }
    }
}