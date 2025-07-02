package com.example.notesapp.util

import android.content.Context
import android.content.SharedPreferences

object PinManager {

    private const val PREF_NAME = "pin_pref"
    private const val PIN_KEY = "user_pin"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun savePin(context: Context, pin: String) {
        val editor = getPrefs(context).edit()
        editor.putString(PIN_KEY, pin)
        editor.apply()
    }

    fun getPin(context: Context): String? {
        return getPrefs(context).getString(PIN_KEY, null)
    }

    fun clearPin(context: Context) {
        getPrefs(context).edit().remove(PIN_KEY).apply()
    }

    fun isPinSet(context: Context): Boolean {
        return getPrefs(context).contains(PIN_KEY)
    }
}
