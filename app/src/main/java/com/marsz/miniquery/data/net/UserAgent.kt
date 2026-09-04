package com.marsz.miniquery.data.net

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.util.Locale

/**
 * 请求身份标识。
 *
 * 每个请求都带上一段包含应用版本与设备信息的 User-Agent，
 * 服务端由此可以区分来源（App / 网页）、定位问题机型、按版本灰度。
 * 只在启动时构建一次并缓存，避免每次请求都做包信息与反射查询。
 *
 * ## 字段边界（金标隐私要求：最小必要、不得用于追踪）
 * - 包含：应用版本、Android 版本与 SDK、机型与内部代号、CPU 架构、系统语言
 * - 明确**不**包含：IMEI、AndroidId、手机号、账号、精确位置、MAC 地址、序列号
 * 这些信息仅用于服务端兼容性判断与问题排查，不会上传到除接口之外的任何地方。
 */
object UserAgent {

    @Volatile
    private var cached: String? = null

    /**
     * 形如：
     * `MiniQuery/2.0.0 (Android 14; SDK 34; Xiaomi 23046RP50C/cmi; arm64-v8a; zh-CN)`
     */
    fun get(context: Context): String {
        cached?.let { return it }
        val value = build(context.applicationContext)
        cached = value
        return value
    }

    /** `zh-CN, en;q=0.9` —— 只暴露语言偏好，不含地区精确信息 */
    fun acceptLanguage(): String {
        val locale = Locale.getDefault()
        val primary = locale.toLanguageTag().ifBlank { "zh-CN" }
        return "$primary, en;q=0.9"
    }

    private fun build(context: Context): String {
        val appName = "MiniQuery"
        val version = runCatching {
            val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            info.versionName ?: "unknown"
        }.getOrDefault("unknown")

        val device = runCatching {
            // Build.MODEL 是用户可读的机型名，Build.DEVICE 是内部代号，两者都带上更利于排查
            val model = Build.MODEL?.trim().orEmpty()
            val deviceCode = Build.DEVICE?.trim().orEmpty()
            if (model.isNotEmpty() && deviceCode.isNotEmpty() && !model.equals(deviceCode, true)) {
                "$model/$deviceCode"
            } else {
                model.ifEmpty { deviceCode }
            }
        }.getOrDefault("unknown")

        val abi = Build.SUPPORTED_ABIS?.firstOrNull() ?: "unknown"
        val locale = Locale.getDefault().let { "${it.language}-${it.country}" }

        return "$appName/$version (" +
                "Android ${Build.VERSION.RELEASE}; " +
                "SDK ${Build.VERSION.SDK_INT}; " +
                "$device; " +
                "$abi; " +
                "$locale" +
                ")"
    }
}
