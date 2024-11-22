package com.harshil258.adplacer.interfaces

import android.content.DialogInterface

interface DialogCallBack {
    fun onPositiveClicked(dialog: DialogInterface)
    fun onNegativeClicked(dialog: DialogInterface)
    fun onDialogCancelled()  // Callback for dialog cancel
    fun onDialogDismissed()  // Callback for dialog dismiss
}
