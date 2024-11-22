package com.harshil258.adplacer.utils

import android.util.Log
import com.harshil258.adplacer.utils.Constants.showLogs

object Logger {

    private var lastLogTime: Long = System.currentTimeMillis()
    val ADSLOG = "ADSLOG"
    val TAG = "TAG_NEO"

    private fun log(level: String, tag: String, message: String) {
        if (!showLogs)
            return
        val currentTime = System.currentTimeMillis()
        val timeElapsed = currentTime - lastLogTime
        lastLogTime = currentTime

        val logMessage = "${timeElapsed}ms $message "
        when (level) {
            "VERBOSE" -> Log.v(tag, logMessage)
            "DEBUG" -> Log.d(tag, logMessage)
            "INFO" -> Log.i(tag, logMessage)
            "WARN" -> Log.w(tag, logMessage)
            "ERROR" -> Log.e(tag, logMessage)
            else -> Log.d(tag, logMessage)
        }

    }

    fun v(tag: String, message: String) {
        log("VERBOSE", tag, message)
    }

    fun d(tag: String, message: String) {
        log("DEBUG", tag, message)
    }

    fun i(tag: String, message: String) {
        log("INFO", tag, message)
    }

    fun w(tag: String, message: String) {
        log("WARN", tag, message)
    }

    fun e(tag: String, message: String) {
        log("ERROR", tag, message)
    }
}
