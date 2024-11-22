package com.harshil258.adplacer.utils

import android.app.Activity
import android.app.Dialog
import android.view.LayoutInflater
import android.widget.TextView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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
                if (isCancelable) {
                    dialog.dismiss()
                }
            }

            positiveButton.setOnClickListener {
                dialogCallback.onPositiveClicked(dialog)
                if (isCancelable) {
                    dialog.dismiss()
                }
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


    fun createMaterialSimpleDialog(
        activity: Activity?,
        title: String,
        description: String,
        negativeButtonText: String,
        positiveButtonText: String,
        dialogCallback: DialogCallBack,
        isCancelable: Boolean
    ) {
        activity?.let {
            // Build the Material Dialog
            val dialog = MaterialAlertDialogBuilder(it)
                .setTitle(title) // Set dialog title
                .setMessage(description) // Set dialog message
                .setCancelable(isCancelable) // Make the dialog cancelable or not
                .setNegativeButton(negativeButtonText) { dialogInterface, _ ->
                    dialogCallback.onNegativeClicked(dialogInterface)
                    if (isCancelable) {
                        dialogInterface.dismiss()
                    }
                }
                .setPositiveButton(positiveButtonText) { dialogInterface, _ ->
                    dialogCallback.onPositiveClicked(dialogInterface)
                    if (isCancelable) {
                        dialogInterface.dismiss()
                    }
                }
                .setOnCancelListener {
                    dialogCallback.onDialogCancelled()
                }
                .setOnDismissListener {
                    dialogCallback.onDialogDismissed()
                }
                .create()

            // Show the dialog
            dialog.show()
        }
    }
}