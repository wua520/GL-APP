package com.fitness.training.util

import android.content.Context
import android.provider.Settings

/**
 * 用户会话管理
 * - 本地账号：存储在 local_session
 * - 云端账号：存储在 cloud_session
 */
object UserSession {
    private const val LOCAL_PREFS = "local_session"
    private const val CLOUD_PREFS = "cloud_session"
    
    // 本地账号 keys
    private const val KEY_USER_ID = "user_id"
    private const val KEY_USERNAME = "username"
    private const val KEY_NICKNAME = "nickname"
    private const val KEY_IS_LOGGED_IN = "is_logged_in"
    
    // 云端账号 keys
    private const val KEY_CLOUD_TOKEN = "token"
    private const val KEY_CLOUD_USER_ID = "user_id"
    private const val KEY_CLOUD_USERNAME = "username"
    private const val KEY_CLOUD_NICKNAME = "nickname"
    
    // ========== 本地账号 ==========
    
    fun getCurrentUserId(context: Context): Long {
        val prefs = context.getSharedPreferences(LOCAL_PREFS, Context.MODE_PRIVATE)
        return if (prefs.getBoolean(KEY_IS_LOGGED_IN, false)) {
            prefs.getLong(KEY_USER_ID, getGuestId(context))
        } else {
            getGuestId(context)
        }
    }
    
    private fun getGuestId(context: Context): Long {
        val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        return -(deviceId.hashCode().toLong().and(0x7FFFFFFF) + 1)
    }
    
    fun isLoggedIn(context: Context): Boolean {
        val prefs = context.getSharedPreferences(LOCAL_PREFS, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }
    
    fun getUsername(context: Context): String? {
        val prefs = context.getSharedPreferences(LOCAL_PREFS, Context.MODE_PRIVATE)
        return prefs.getString(KEY_USERNAME, null)
    }
    
    fun getNickname(context: Context): String? {
        val prefs = context.getSharedPreferences(LOCAL_PREFS, Context.MODE_PRIVATE)
        return prefs.getString(KEY_NICKNAME, null)
    }
    
    fun saveLoginInfo(context: Context, userId: Long, username: String, nickname: String?) {
        val prefs = context.getSharedPreferences(LOCAL_PREFS, Context.MODE_PRIVATE)
        prefs.edit()
            .putLong(KEY_USER_ID, userId)
            .putString(KEY_USERNAME, username)
            .putString(KEY_NICKNAME, nickname ?: username)
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .apply()
    }
    
    fun updateNickname(context: Context, nickname: String) {
        val prefs = context.getSharedPreferences(LOCAL_PREFS, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_NICKNAME, nickname).apply()
    }
    
    fun logout(context: Context) {
        val prefs = context.getSharedPreferences(LOCAL_PREFS, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }
    
    // ========== 云端账号 ==========
    
    fun getToken(context: Context): String? {
        val prefs = context.getSharedPreferences(CLOUD_PREFS, Context.MODE_PRIVATE)
        return prefs.getString(KEY_CLOUD_TOKEN, null)
    }
    
    fun getCloudUsername(context: Context): String? {
        val prefs = context.getSharedPreferences(CLOUD_PREFS, Context.MODE_PRIVATE)
        return prefs.getString(KEY_CLOUD_USERNAME, null)
    }
    
    fun getCloudNickname(context: Context): String? {
        val prefs = context.getSharedPreferences(CLOUD_PREFS, Context.MODE_PRIVATE)
        return prefs.getString(KEY_CLOUD_NICKNAME, null)
    }
    
    fun updateCloudNickname(context: Context, nickname: String) {
        val prefs = context.getSharedPreferences(CLOUD_PREFS, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_CLOUD_NICKNAME, nickname).apply()
    }

    fun saveCloudLoginInfo(context: Context, userId: Long, username: String, nickname: String?, token: String) {
        val prefs = context.getSharedPreferences(CLOUD_PREFS, Context.MODE_PRIVATE)
        prefs.edit()
            .putLong(KEY_CLOUD_USER_ID, userId)
            .putString(KEY_CLOUD_USERNAME, username)
            .putString(KEY_CLOUD_NICKNAME, nickname ?: username)
            .putString(KEY_CLOUD_TOKEN, token)
            .apply()
    }
    
    fun cloudLogout(context: Context) {
        val prefs = context.getSharedPreferences(CLOUD_PREFS, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }
}
