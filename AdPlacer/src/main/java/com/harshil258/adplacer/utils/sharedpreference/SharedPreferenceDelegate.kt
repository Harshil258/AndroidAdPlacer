package com.zeel_enterprise.shreekhodalkotlin.common

import android.content.SharedPreferences
import androidx.core.content.edit
import com.google.gson.Gson
import com.harshil258.adplacer.utils.fromJson
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class StringPreferenceDelegate(
    private val sharedPreferences: SharedPreferences,
    private val key: String,
    private val defaultValue: String = ""
) : ReadWriteProperty<Any, String> {
    override fun setValue(thisRef: Any, property: KProperty<*>, value: String) {
        sharedPreferences.edit {
            putString(key, value)
        }
    }

    override fun getValue(thisRef: Any, property: KProperty<*>): String {
        return sharedPreferences.getString(key, defaultValue) ?: defaultValue
    }
}

class StringSetPreferenceDelegate(
    private val sharedPreferences: SharedPreferences,
    private val key: String,
    private val defaultValue: Set<String> = setOf()
) : ReadWriteProperty<Any, Set<String>> {
    override fun setValue(thisRef: Any, property: KProperty<*>, value: Set<String>) {
        sharedPreferences.edit {
            putStringSet(key, value)
        }
    }

    override fun getValue(thisRef: Any, property: KProperty<*>): Set<String> {
        return sharedPreferences.getStringSet(key, defaultValue) ?: defaultValue
    }
}

class DataModelPreferenceDelegate<T>(
    private val sharedPreferences: SharedPreferences,
    private val key: String,
    private val clazz: Class<T>,
    private val defaultValue: T
) : ReadWriteProperty<Any, T> {

    override fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
        val json = Gson().toJson(value)
        sharedPreferences.edit {
            putString(key, json)
        }
    }

    override fun getValue(thisRef: Any, property: KProperty<*>): T {
        val json = sharedPreferences.getString(key, null)
        return if (json != null) {
            json.fromJson(clazz)
        } else {
            defaultValue
        }
    }
}

class IntPreferenceDelegate(
    private val sharedPreferences: SharedPreferences,
    private val key: String,
    private val defaultValue: Int = -1,
) : ReadWriteProperty<Any, Int> {
    override fun setValue(thisRef: Any, property: KProperty<*>, value: Int) {
        sharedPreferences.edit {
            putInt(key, value)
        }
    }

    override fun getValue(thisRef: Any, property: KProperty<*>): Int {
        return sharedPreferences.getInt(key, defaultValue)
    }
}

class LongPreferenceDelegate(
    private val sharedPreferences: SharedPreferences,
    private val key: String,
    private val defaultValue: Long = 0,
) : ReadWriteProperty<Any, Long> {
    override fun setValue(thisRef: Any, property: KProperty<*>, value: Long) {
        sharedPreferences.edit {
            putLong(key, value)
        }
    }

    override fun getValue(thisRef: Any, property: KProperty<*>): Long {
        return sharedPreferences.getLong(key, defaultValue)
    }
}

class FloatPreferenceDelegate(
    private val sharedPreferences: SharedPreferences,
    private val key: String,
    private val defaultValue: Float = 0f,
) : ReadWriteProperty<Any, Float> {
    override fun setValue(thisRef: Any, property: KProperty<*>, value: Float) {
        sharedPreferences.edit {
            putFloat(key, value)
        }
    }

    override fun getValue(thisRef: Any, property: KProperty<*>): Float {
        return sharedPreferences.getFloat(key, defaultValue)
    }
}

class BooleanPreferenceDelegate(
    private val sharedPreferences: SharedPreferences,
    private val key: String,
    private val defaultValue: Boolean = false,
) : ReadWriteProperty<Any, Boolean> {
    override fun setValue(thisRef: Any, property: KProperty<*>, value: Boolean) {
        sharedPreferences.edit {
            putBoolean(key, value)
        }
    }

    override fun getValue(thisRef: Any, property: KProperty<*>): Boolean {
        return sharedPreferences.getBoolean(key, defaultValue)
    }
}

class KeyExistPreferenceDelegate(
    private val sharedPreferences: SharedPreferences,
    private val key: String,
) : ReadOnlyProperty<Any, Boolean> {
    override fun getValue(thisRef: Any, property: KProperty<*>): Boolean {
        return sharedPreferences.contains(key)
    }
}

fun SharedPreferences.removeData(vararg keys: String) {
    edit {
        keys.forEach { key -> remove(key) }
    }
}
