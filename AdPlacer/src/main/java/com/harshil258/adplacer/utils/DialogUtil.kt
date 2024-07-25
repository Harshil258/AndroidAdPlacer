package com.harshil258.adplacer.utils

import android.app.Activity
import android.app.Dialog
import android.view.LayoutInflater
import android.widget.Button
import android.widget.TextView
import com.harshil258.adplacer.R
import com.harshil258.adplacer.interfaces.DialogCallBack

object DialogUtil {

    fun createSimpleDialog(
        activity: Activity?,
        title: String,
        description: String,
        negativeButtonText: String,
        positiveButtonText: String,
        dialogCallback: DialogCallBack,
        isCancelable: Boolean
    ) {
        activity?.let {
            val dialog = Dialog(it)
            val view = LayoutInflater.from(it).inflate(R.layout.layout_internet_dialog, null)
            dialog.setContentView(view)
            dialog.setCancelable(isCancelable)
            dialog.setCanceledOnTouchOutside(isCancelable)

            val titleTextView: TextView = view.findViewById(R.id.dialogTitle)
            val descriptionTextView: TextView = view.findViewById(R.id.dialogDescription)
            val negativeButton: TextView = view.findViewById(R.id.dialogNegativeButton)
            val positiveButton: TextView = view.findViewById(R.id.dialogPositiveButton)

            titleTextView.text = title
            descriptionTextView.text = description
            negativeButton.text = negativeButtonText
            positiveButton.text = positiveButtonText

            negativeButton.setOnClickListener {
                dialogCallback.onNegativeClicked(dialog)
                dialog.dismiss()
            }

            positiveButton.setOnClickListener {
                dialogCallback.onPositiveClicked(dialog)
                dialog.dismiss()
            }

            dialog.setOnCancelListener {
                dialogCallback.onDialogCancelled()
            }

            dialog.setOnDismissListener {
                dialogCallback.onDialogDismissed()
            }

            dialog.show()
        }
    }
}