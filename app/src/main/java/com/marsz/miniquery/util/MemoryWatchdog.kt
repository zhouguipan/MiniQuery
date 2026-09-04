package com.marsz.miniquery.util

import android.app.ActivityManager
import android.content.ComponentCallbacks2
import android.content.Context
import android.os.Debug
import android.util.Log
import com.marsz.miniquery.BuildConfig
import com.marsz.miniquery.cache.ImageCache

/**
 * 后台内存治理（对应金标联盟《公平运行内存机制》要求）。
 *
 * 职责：
 * 1. 进程进入后台 → 立即释放图片内存缓存（磁盘保留，回前台秒加载）；
 * 2. 系统发出内存紧张回调 → 逐级释放，降低被 LMK 杀掉的概率；
 * 3. 在后台时若可用内存跌破安全阈值，主动再压一轮，避免后台占用过高被商店检测扣分。
 *
 * 内存水位只在 Debug 构建打印，Release 不输出，不影响性能与隐私。
 */
object MemoryWatchdog {

    private const val TAG = "MemoryWatchdog"

    /** 后台安全水位：低于此值认为需要再释放一轮 */
    private const val BACKGROUND_SAFE_MB = 80

    fun install(context: Context) {
        val app = context.applicationContext

        // 注册系统级内存回调，覆盖 Application 之外的场景（如 Service 后台运行）
        app.registerComponentCallbacks(object : ComponentCallbacks2 {
            override fun onTrimMemory(level: Int) {
                trim(level)
                if (level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND) {
                    // 后台：再扫一遍，释放任何新增的缓存
                    ensureBackgroundBudget(app)
                }
            }

            override fun onLowMemory() {
                trim(ComponentCallbacks2.TRIM_MEMORY_COMPLETE)
            }

            override fun onConfigurationChanged(newConfig: android.content.res.Configuration) = Unit
        })
    }

    fun trim(level: Int) {
        ImageCache.trimMemory(level)
    }

    /** 低端设备判定：<= 128MB 堆的设备把缓存上限再压半 */
    fun isLowMemoryDevice(context: Context): Boolean {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        return (am?.memoryClass ?: 192) <= 128
    }

    private fun ensureBackgroundBudget(context: Context) {
        val availableMb = runCatching {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memInfo = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
            (memInfo.availMem / (1024 * 1024)).toInt()
        }.getOrDefault(Int.MAX_VALUE)

        if (availableMb < BACKGROUND_SAFE_MB) {
            Log.w(TAG, "后台可用内存偏低（${availableMb}MB），强制释放图片内存缓存")
            ImageCache.trimMemory(ComponentCallbacks2.TRIM_MEMORY_COMPLETE)
        }

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "PSS=${Debug.getPss() / 1024}MB avail=${availableMb}MB")
        }
    }
}
