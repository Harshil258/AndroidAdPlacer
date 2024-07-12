package com.harshil258.adplacer.utils


import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.harshil258.adplacer.models.AdsDetails
import com.harshil258.adplacer.models.ApiResponse
import com.harshil258.adplacer.models.AppDetails
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken


inline fun <reified T> Gson.fromJson(json: String?): T {
    return this.fromJson(json, object : TypeToken<T>() {}.type)
}


class SharedPrefConfig(private val context: Context) {
    private val prefs: SharedPreferences? =
        context.getSharedPreferences("H_VEKARIYA", Context.MODE_PRIVATE)
    private val gson = Gson()


    companion object {
        lateinit var sharedPrefConfig: SharedPrefConfig


        fun sharedPrefConfigInstance(context: Context){
            sharedPrefConfig = SharedPrefConfig(context)
        }

    }

    val APPOPENCOUNT = "APPOPENCOUNT"
    val PREMIUM_PLAN = "PREMIUM_PLAN"


    var isLoadingFirstTime: Boolean
        get() = prefs.load("isLoadingFirstTime", false)
        set(v) = prefs.save("isLoadingFirstTime", true)


    private inline fun <reified T> SharedPreferences?.save(key: String, value: T) {
        this?.edit()?.putString(key, gson.toJson(value))?.apply()
    }

    private inline fun <reified T> SharedPreferences?.load(key: String, defaultValue: T): T {
        val json = this?.getString(key, null)
        return if (json != null) {
            gson.fromJson(json, T::class.java)
        } else {
            defaultValue
        }
    }


    var isResponseGot: Boolean
        get() = prefs.load("isResponseGot", false)
        set(value) = prefs.save("isResponseGot", value)


    var isHowToUseShowDone: Boolean
        get() = prefs.load("isHowToUseShowDone", false)
        set(value) = prefs.save("isHowToUseShowDone", value)


    var diaryfetchedid: Int?
        get() = prefs.load("diaryidfetchedkey", 0)
        set(value) = prefs.save("diaryidfetchedkey", value)

    var apiResponse: ApiResponse
        get() = prefs.load("API_RESPONSE", ApiResponse())
        set(value) = prefs.save("API_RESPONSE", value)

    var appDetails: AppDetails
        get() = apiResponse.appDetails
        set(value) {
            val updatedApiResponse = apiResponse.copy(appDetails = value)
            Log.e("rdhdhsdthjtg", ": ${updatedApiResponse.appDetails.adStatus}")
            apiResponse = updatedApiResponse
        }

    var adsDetails: AdsDetails
        get() = apiResponse.adsDetails
        set(value) {
            val updatedApiResponse = apiResponse.copy(adsDetails = value)
            apiResponse = updatedApiResponse
        }


    fun getStringPref(key: String, defaultValue: String?): String {
        return prefs?.getString(key, defaultValue).toString()
    }

    fun setStringPref(key: String, value: String?) {
        prefs?.edit()?.putString(key, value)?.apply()
    }

}

