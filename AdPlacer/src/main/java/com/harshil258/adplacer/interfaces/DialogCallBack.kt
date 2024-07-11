package com.harshil258.adplacer.interfaces

import android.app.Dialog

interface DialogCallBack {
    fun onPositiveClicked(dialog: Dialog)
    fun onNegativeClicked(dialog: Dialog)
    fun onDialogCancelled()  // Callback for dialog cancel
    fun onDialogDismissed()  // Callback for dialog dismiss
}
