package com.fitness.training.util

import android.content.Context

object BodyProfile {
    private const val PREFS_NAME = "body_profile"
    private const val KEY_GENDER = "gender"        // 0=未设置, 1=男, 2=女
    private const val KEY_HEIGHT = "height"        // 身高 cm
    private const val KEY_BIRTH_YEAR = "birth_year" // 出生年份
    
    fun getGender(context: Context): Int {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_GENDER, 0)
    }
    
    fun setGender(context: Context, gender: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putInt(KEY_GENDER, gender).apply()
    }
    
    fun getHeight(context: Context): Int {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_HEIGHT, 0)
    }
    
    fun setHeight(context: Context, height: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putInt(KEY_HEIGHT, height).apply()
    }
    
    fun getBirthYear(context: Context): Int {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_BIRTH_YEAR, 0)
    }
    
    fun setBirthYear(context: Context, year: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putInt(KEY_BIRTH_YEAR, year).apply()
    }
    
    fun getGenderText(context: Context): String {
        return when (getGender(context)) {
            1 -> "男"
            2 -> "女"
            else -> "未设置"
        }
    }
    
    fun getHeightText(context: Context): String {
        val height = getHeight(context)
        return if (height > 0) "${height} cm" else "未设置"
    }
    
    fun getAgeText(context: Context): String {
        val birthYear = getBirthYear(context)
        if (birthYear <= 0) return "未设置"
        val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
        return "${currentYear - birthYear} 岁"
    }
}
