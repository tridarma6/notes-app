package com.example.notesapp.util

import android.content.Context
import android.content.SharedPreferences

object PinManager {

    private const val PREF_NAME = "pin_pref"
    private const val PIN_KEY = "user_pin"
    private const val KEY_SECURITY_QUESTION_INDEX = "security_question_index"
    private const val KEY_SECURITY_ANSWER = "security_answer"
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

    fun saveSecurityQuestion(context: Context, questionIndex: Int, answer: String) {
        getPrefs(context).edit()
            .putInt(KEY_SECURITY_QUESTION_INDEX, questionIndex)
            .putString(KEY_SECURITY_ANSWER, answer)
            .apply()
    }

    fun getSecurityQuestionIndex(context: Context): Int {
        return getPrefs(context).getInt(KEY_SECURITY_QUESTION_INDEX, -1)
    }

    fun verifySecurityAnswer(context: Context, answer: String): Boolean {
        val savedAnswer = getPrefs(context).getString(KEY_SECURITY_ANSWER, null)
        return savedAnswer != null && savedAnswer.equals(answer, ignoreCase = true)
    }

    fun isSecurityQuestionSet(context: Context): Boolean {
        return getPrefs(context).contains(KEY_SECURITY_QUESTION_INDEX) && getPrefs(context).contains(KEY_SECURITY_ANSWER)
    }

}
