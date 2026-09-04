package com.marsz.miniquery.util

import android.os.Build
import android.os.Debug
import java.io.File
import com.marsz.miniquery.BuildConfig

/**
 * 运行环境安全状态（金标安全检测相关）。
 *
 * 检测维度全部为**本地只读**，结果仅展示在「设置 → 隐私与安全」，绝不上传服务器：
 * - 是否为调试构建 / 调试器已附加；
 * - 设备是否已 Root（检测常见 Root 痕迹文件与 su 二进制）；
 * - 是否通过代理联网（HTTP 代理），存在流量劫持风险；
 * - 是否运行在模拟器中。
 *
 * 注意：此类检测均可被有意识绕过，仅作为用户侧风险提示，不构成安全保证。
 */
data class SecurityStatus(
    val safe: Boolean,
    val issues: List<String>
) {
    val summary: String
        get() = if (safe) "未发现明显风险" else issues.joinToString("、")

    companion object {
        fun detect(): SecurityStatus {
            val issues = mutableListOf<String>()

            if (BuildConfig.DEBUG) issues += "调试模式"
            if (Debug.isDebuggerConnected()) issues += "调试器已连接"

            if (isRooted()) issues += "设备已 Root"
            if (isRunningOnProxy()) issues += "检测到网络代理"
            if (isEmulator()) issues += "运行在模拟器"

            return SecurityStatus(safe = issues.isEmpty(), issues = issues)
        }

        private fun isRooted(): Boolean {
            val indicators = listOf(
                "/system/bin/su",
                "/system/xbin/su",
                "/sbin/su",
                "/system/sd/xbin/su",
                "/system/bin/.ext/.su",
                "/system/etc/init.d/99SuperSUDaemon",
                "/system/app/Superuser.apk",
                "/data/local/su"
            )
            if (indicators.any { File(it).exists() }) return true
            // 不调用 Runtime.exec("su")：避免触发安全软件告警，仅做文件特征判断
            return false
        }

        private fun isRunningOnProxy(): Boolean {
            val proxyHost = System.getProperty("http.proxyHost").orEmpty()
            val proxyPort = System.getProperty("http.proxyPort").orEmpty()
            return proxyHost.isNotBlank() || proxyPort.isNotBlank()
        }

        private fun isEmulator(): Boolean {
            val fingerprint = Build.FINGERPRINT.orEmpty()
            val hardware = Build.HARDWARE.orEmpty()
            val model = Build.MODEL.orEmpty()
            val product = Build.PRODUCT.orEmpty()
            return fingerprint.contains("generic", ignoreCase = true) ||
                fingerprint.contains("emulator", ignoreCase = true) ||
                hardware.contains("goldfish", ignoreCase = true) ||
                hardware.contains("ranchu", ignoreCase = true) ||
                model.contains("sdk", ignoreCase = true) ||
                product.contains("sdk", ignoreCase = true)
        }
    }
}
