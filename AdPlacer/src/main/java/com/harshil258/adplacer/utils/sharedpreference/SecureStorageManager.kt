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
    var isLoadingFirstTime: Boolean
    var isHowToUseShowDone: Boolean
    var apiResponse: ApiResponse
    var appDetails: AppDetails
    var adsDetails: AdsDetails
    var isResponseGot: Boolean

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
        lateinit var secureStorageManager: SecureStorageManager

        fun initSecureStorageManager(context: Context){
            secureStorageManager = SecureStorageManager(context)
        }
    }

    private val encryptedPrefs: SharedPreferences = context.getSharedPreferences("H_VEKARIYA", 0)

    override var isLoadingFirstTime: Boolean by BooleanPreferenceDelegate(
        encryptedPrefs, IS_LOADING_FIRST_TIME
    )

    override var isHowToUseShowDone: Boolean by BooleanPreferenceDelegate(
        encryptedPrefs, IS_HOW_TO_USE_SHOW_DONE
    )

    override var isResponseGot: Boolean by BooleanPreferenceDelegate(
        encryptedPrefs, IS_RESPONSE_GOT
    )

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