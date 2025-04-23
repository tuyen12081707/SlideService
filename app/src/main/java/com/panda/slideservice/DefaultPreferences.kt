package com.panda.slideservice

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import androidx.core.content.edit


class DefaultPreferences(private val mContext: Context) {
    private val SP: SharedPreferences? = null
    var gif: Int = 0

    fun read(key: String?, defValue: String?): String? {
        return PreferenceManager.getDefaultSharedPreferences(this.mContext).getString(key, defValue)
    }

    fun save(key: String?, value: String?) {
        PreferenceManager.getDefaultSharedPreferences(this.mContext).edit(commit = true) {
            putString(key, value)
        }
    }
}