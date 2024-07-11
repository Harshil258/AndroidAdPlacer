package com.harshil258.adplacer.interfaces

import android.app.Dialog

interface RewardAdCallBack {
    fun onContinueFlow()
    fun onRewardGranted()
    fun onAdNotAvailable(dialog: Dialog?)
}
