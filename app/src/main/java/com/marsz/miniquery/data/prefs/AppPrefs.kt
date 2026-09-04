package com.marsz.miniquery.data.prefs

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class ThemeMode(val label: String) {
    FOLLOW_SYSTEM("跟随系统"),
    LIGHT("浅色"),
    DARK("深色")
}

/**
 * 轻量设置存储。
 * 除了读写，还维护一份可观察的状态副本，Compose 里读取后设置变更会即时生效，
 * 无需手动重启 Activity。
 */
object AppPrefs {

    private const val FILE = "mini_query_prefs"
    private const val KEY_THEME = "theme_mode"
    private const val KEY_LAST_UIN = "last_uin"
    private const val KEY_HISTORY = "history"
    private const val KEY_ANIM = "reduce_anim"
    private const val KEY_PRIVACY_ACCEPTED = "privacy_accepted"

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    /* ==================== 可观察状态副本 ==================== */

    var themeMode by mutableStateOf(ThemeMode.FOLLOW_SYSTEM)
        private set

    var reduceAnim by mutableStateOf(false)
        private set

    /** 应用启动时调用一次，把磁盘上的值灌入可观察状态 */
    fun hydrate(context: Context) {
        val p = prefs(context)
        themeMode = runCatching { ThemeMode.valueOf(p.getString(KEY_THEME, ThemeMode.FOLLOW_SYSTEM.name)!!) }
            .getOrDefault(ThemeMode.FOLLOW_SYSTEM)
        reduceAnim = p.getBoolean(KEY_ANIM, false)
    }

    /* ==================== 主题 ==================== */

    fun setThemeMode(context: Context, mode: ThemeMode) {
        themeMode = mode
        prefs(context).edit().putString(KEY_THEME, mode.name).apply()
    }

    /* ==================== 减弱动画 ==================== */

    fun setReduceAnim(context: Context, enabled: Boolean) {
        reduceAnim = enabled
        prefs(context).edit().putBoolean(KEY_ANIM, enabled).apply()
    }

    /* ==================== 最近查询的迷你号 ==================== */

    fun lastUin(context: Context): String? = prefs(context).getString(KEY_LAST_UIN, null)

    fun setLastUin(context: Context, uin: String) {
        prefs(context).edit().putString(KEY_LAST_UIN, uin).apply()
    }

    /* ==================== 查询历史 ==================== */

    private const val MAX_HISTORY = 10

    /** 最近查询记录，最新的排在最前 */
    fun getHistory(context: Context): List<String> {
        val raw = prefs(context).getString(KEY_HISTORY, null) ?: return emptyList()
        return raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    /** 写入一条查询记录：去重后置于队首，超出上限丢弃最旧的 */
    fun addHistory(context: Context, uin: String) {
        val list = getHistory(context).toMutableList()
        list.remove(uin)
        list.add(0, uin)
        val trimmed = list.take(MAX_HISTORY)
        prefs(context).edit().putString(KEY_HISTORY, trimmed.joinToString(",")).apply()
    }

    fun removeHistory(context: Context, uin: String) {
        val list = getHistory(context).toMutableList()
        list.remove(uin)
        prefs(context).edit().putString(KEY_HISTORY, list.joinToString(",")).apply()
    }

    fun clearHistory(context: Context) {
        prefs(context).edit().remove(KEY_HISTORY).apply()
    }

    /* ==================== 隐私政策同意 ==================== */

    /**
     * 金标 / 个保法要求：涉及个人信息处理前须取得用户明示同意。
     * 这里只记录"用户已查看并同意隐私政策"这一事实，不含任何个人数据。
     */
    fun isPrivacyAccepted(context: Context): Boolean =
        prefs(context).getBoolean(KEY_PRIVACY_ACCEPTED, false)

    fun setPrivacyAccepted(context: Context) {
        prefs(context).edit().putBoolean(KEY_PRIVACY_ACCEPTED, true).apply()
    }
}
