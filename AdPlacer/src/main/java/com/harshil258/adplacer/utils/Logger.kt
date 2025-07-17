package com.harshil258.adplacer.utils

import android.util.Log
import com.harshil258.adplacer.BuildConfig
import com.harshil258.adplacer.utils.Constants.showLogs

object Logger {

    private var lastLogTime: Long = System.currentTimeMillis()
    val ADSLOG = "ADSLOG"
    val TAG = "TAG_NEO"

    private fun getLogOrigin(): String {
        val stackTrace = Throwable().stackTrace
        // Index 3 or 4 usually points to the caller method (depends on inline)
        val element = stackTrace.firstOrNull {
            it.className != Logger::class.java.name
        } ?: return ""
        val fileName = element.fileName ?: "UnknownFile"
        val lineNumber = element.lineNumber
        val methodName = element.methodName
        return "($fileName:$lineNumber)#$methodName"
    }

    private fun log(level: String, tag: String, message: String) {
        if (!showLogs) return
//        if (BuildConfig.DEBUG){
            val currentTime = System.currentTimeMillis()
            val timeElapsed = currentTime - lastLogTime
            Logger.lastLogTime = currentTime

            val origin = getLogOrigin()
            val logMessage = "$origin ${timeElapsed}ms: $message"

            when (level) {
                "VERBOSE" -> Log.v(tag, logMessage)
                "DEBUG" -> Log.d(tag, logMessage)
                "INFO" -> Log.i(tag, logMessage)
                "WARN" -> Log.w(tag, logMessage)
                "ERROR" -> Log.e(tag, logMessage)
                else -> Log.d(tag, logMessage)
            }
//        }


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
