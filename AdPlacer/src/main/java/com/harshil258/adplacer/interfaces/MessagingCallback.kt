package com.harshil258.adplacer.interfaces

import com.google.firebase.remoteconfig.FirebaseRemoteConfig

interface MessagingCallback {
    fun hideSplashLoader()
    fun showSplashLoader()
    val isExitActivity: Boolean
    fun openStartActivity()
    fun openHomeActivity()
    fun openHowToUseActivity()
    fun openExtraStartActivity()
    fun exitApplication()
    fun showNetworkDialog()
    fun onFirebaseResponseReceived(firebaseConfig: FirebaseRemoteConfig)
    fun startScreenTransitionTimer()
    fun onApiResponseSaved()
}
