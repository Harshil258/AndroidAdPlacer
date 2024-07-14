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
    fun exitTheApplication()
    fun showNetworkDialog()
    fun gotFirebaseResponse(firebaseConfig: FirebaseRemoteConfig)
    fun startingTimerToChangeScreen()
}
