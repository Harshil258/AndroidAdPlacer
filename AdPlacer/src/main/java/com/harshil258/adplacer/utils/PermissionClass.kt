package com.harshil258.adplacer.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat


interface PermissionCallback {
    fun onPermissionRequestFailed() {}
    fun onPermRequested() {}
    fun onSuccess()
    fun onPermNotGranted() {}
    fun onPermDeny() {}
}

class PermissionClass {

    fun checkPermission(
        context: Activity,
        hasToRequest: Boolean,
        permArr: ArrayList<String>,
        registry: ActivityResultRegistry,
        callBack: PermissionCallback
    ) {
        val permissionResultLauncher =
            registry.register(
                "102",
                ActivityResultContracts.RequestMultiplePermissions()
            ) { permission: Map<String, Boolean> ->
                var allGranted = permission.values.all { it }

                if (permission.containsKey(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) &&
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    allGranted = true
                }

                if (allGranted) {
                    callBack.onSuccess()
                    savePermissions(context, permission.keys)
                } else {
                    callBack.onPermDeny()
                }
            }

        if (isAllPermGranted(context, permArr)) {
            callBack.onSuccess()
        } else {
            if (hasToRequest) {
                if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) && isAnyPermNeedRationalAsk(
                        context,
                        permArr
                    )
                ) {
                    callBack.onPermissionRequestFailed()
                } else {
                    try {
                        permissionResultLauncher.launch(permArr.toTypedArray())
                        callBack.onPermRequested()
                    } catch (e: Exception) {
                        callBack.onPermissionRequestFailed()
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    private fun isAnyPermNeedRationalAsk(context: Activity, permArr: ArrayList<String>): Boolean {
        val sp = getSharedPreferences(context)
        return permArr.any { permission ->
            if (ActivityCompat.shouldShowRequestPermissionRationale(context, permission)) {
                if (sp.getBoolean(permission, true)) {
                    sp.edit().putBoolean(permission, false).apply()
                    false
                } else {
                    true
                }
            } else {
                !sp.getBoolean(permission, true)
            }
        }
    }

    private fun isAllPermGranted(context: Activity, permArr: ArrayList<String>): Boolean {
        return permArr.all { permission ->
            ContextCompat.checkSelfPermission(
                context,
                permission
            ) == PackageManager.PERMISSION_GRANTED ||
                    (permission == Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED &&
                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
                            ) == PackageManager.PERMISSION_GRANTED)
        }
    }

    private fun savePermissions(context: Context, permissions: Set<String>) {
        val sp = getSharedPreferences(context)
        permissions.forEach { permission ->
            sp.edit().putBoolean(permission, true).apply()
        }
    }

    companion object {
        private fun getSharedPreferences(context: Context): SharedPreferences {
            return context.getSharedPreferences("PERMISSION", Context.MODE_PRIVATE)
        }
    }
}