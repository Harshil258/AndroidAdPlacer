package com.harshil258.adplacer.utils

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics

object commonFunctions {

    fun logCustomEvent(
        context: Context,
        eventName: String,
        eventParams: Map<String, String>
    ) {
        Log.d("TAG23535", "logCustomEvent: eventName  ${eventName}")
        // Get Firebase Analytics instance
        val firebaseAnalytics = FirebaseAnalytics.getInstance(context)

        // Prepare a Bundle with event parameters
        val bundle = Bundle().apply {
            for ((key, value) in eventParams) {
                putString(key, value)
            }
        }

        // Log the custom event
        firebaseAnalytics.logEvent(eventName, bundle)
    }



}