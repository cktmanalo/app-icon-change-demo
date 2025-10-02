package com.gonodono.appiconchangedemo

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Created by Charles Manalo on 10/02/2025
 */


class PrefsHelper {

    fun getPrefs(context: Context): SharedPreferences? {
        return context.getSharedPreferences(BuildConfig.APPLICATION_ID, 0)
    }

    fun getString(context: Context, key: String?): String {
        return getPrefs(context)!!.getString(key, "")!!
    }

    fun getInt(context: Context, key: String?): Int {
        return getPrefs(context)!!.getInt(key, 0)
    }

    fun getFloat(context: Context, key: String?): Float {
        return getPrefs(context)!!.getFloat(key, 0f)
    }

    fun getLong(context: Context, key: String?): Long {
        return getPrefs(context)!!.getLong(key, 0L)
    }

    fun getBoolean(context: Context, key: String?): Boolean {
        return getPrefs(context)!!.getBoolean(key, false)
    }

    fun setBoolean(context: Context, key: String?, value: Boolean) {
        getPrefs(context)!!.edit { putBoolean(key, value) }
    }

    fun setString(context: Context, key: String?, value: String?) {
        getPrefs(context)!!.edit { putString(key, value) }
    }

    fun setInt(context: Context, key: String?, value: Int) {
        getPrefs(context)!!.edit { putInt(key, value) }
    }

    fun setFloat(context: Context, key: String?, value: Float) {
        getPrefs(context)!!.edit { putFloat(key, value) }
    }

    fun setLong(context: Context, key: String?, value: Long) {
        getPrefs(context)!!.edit { putLong(key, value) }
    }

    fun removePref(context: Context, key: String?) {
        getPrefs(context)!!.edit { remove(key).clear() }
    }

    fun setListObject(context: Context, key: String?, list: MutableList<out Any?>?) {
        val gson: com.google.gson.Gson = com.google.gson.Gson()
        val listString: String? = gson.toJson(list)
        getPrefs(context)!!.edit { putString(key, listString) }
    }

    fun <T> getListObject(context: Context, key: String?, clz: Class<Array<T?>?>): MutableList<T?> {
        val gson: com.google.gson.Gson = com.google.gson.Gson()
        val listString: String = getPrefs(context)!!.getString(key, "")!!
        val list: Array<T?> = gson.fromJson<Array<T?>?>(listString, clz)
        return listOf<T?>(*list) as MutableList<T?>
    }
}