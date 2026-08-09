package com.fitness.training.util

import android.content.Context

object BodyProfile {
    private const val PREFS_NAME = "body_profile"
    private const val KEY_GENDER = "gender"        // 0=未设置, 1=男, 2=女
    private const val KEY_HEIGHT = "height"        // 身高 cm
    private const val KEY_BIRTH_YEAR = "birth_year" // 出生年份
    private const val KEY_UPDATED_AT = "updated_at" // 更新时间
    
    fun getGender(context: Context): Int {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_GENDER, 0)
    }
    
    fun setGender(context: Context, gender: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_GENDER, gender)
            .putLong(KEY_UPDATED_AT, System.currentTimeMillis())
            .apply()
    }
    
    fun getHeight(context: Context): Int {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_HEIGHT, 0)
    }
    
    fun setHeight(context: Context, height: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_HEIGHT, height)
            .putLong(KEY_UPDATED_AT, System.currentTimeMillis())
            .apply()
    }
    
    fun getBirthYear(context: Context): Int {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_BIRTH_YEAR, 0)
    }
    
    fun setBirthYear(context: Context, year: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_BIRTH_YEAR, year)
            .putLong(KEY_UPDATED_AT, System.currentTimeMillis())
            .apply()
    }
    
    fun getUpdatedAt(context: Context): Long {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getLong(KEY_UPDATED_AT, 0)
    }
    
    fun setUpdatedAt(context: Context, timestamp: Long) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putLong(KEY_UPDATED_AT, timestamp).apply()
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
    
    // 获取完整的身体档案数据（用于同步）
    fun getProfileData(context: Context): com.fitness.training.network.BodyProfileData? {
        val gender = getGender(context)
        val height = getHeight(context)
        val birthYear = getBirthYear(context)
        val updatedAt = getUpdatedAt(context)
        
        // 如果所有字段都是默认值，返回null（不上传空数据）
        if (gender == 0 && height == 0 && birthYear == 0) {
            return null
        }
        
        return com.fitness.training.network.BodyProfileData(
            gender = gender,
            height = height,
            birthYear = birthYear,
            updatedAt = if (updatedAt == 0L) System.currentTimeMillis() else updatedAt
        )
    }
    
    // 保存完整的身体档案数据（用于同步）
    fun saveProfileData(context: Context, data: com.fitness.training.network.BodyProfileData) {
        val localUpdatedAt = getUpdatedAt(context)
        
        // 只有云端数据更新时才保存
        if (data.updatedAt > localUpdatedAt) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putInt(KEY_GENDER, data.gender)
                .putInt(KEY_HEIGHT, data.height)
                .putInt(KEY_BIRTH_YEAR, data.birthYear)
                .putLong(KEY_UPDATED_AT, data.updatedAt)
                .apply()
        }
    }
    
    // 清除身体档案数据
    fun clearProfile(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }
}
