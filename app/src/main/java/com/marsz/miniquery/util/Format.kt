package com.marsz.miniquery.util

import java.util.Locale
import kotlin.math.floor

/** 空值统一显示为 "-"，与 HTML 的 setText 行为一致 */
fun String?.dash(): String = if (this == null || this.isBlank()) "-" else this.trim()

/** 布尔显示 */
fun Boolean.yesNo(): String = if (this) "是" else "否"

/**
 * 数字格式化：
 * >= 1亿 → x.x亿；>= 1万 → x.x万；否则原值。
 */
fun formatNum(raw: String?): String {
    val s = raw?.trim()
    if (s.isNullOrEmpty()) return "-"
    val v = s.toDoubleOrNull() ?: return s
    return when {
        v >= 1e8 -> String.format(Locale.US, "%.1f亿", v / 1e8)
        v >= 1e4 -> String.format(Locale.US, "%.1f万", v / 1e4)
        v == floor(v) -> v.toLong().toString()
        else -> s
    }
}

/** 秒级时间戳 → yyyy-MM-dd */
fun formatJoinTime(ts: Long?): String {
    if (ts == null || ts <= 0) return "-"
    val d = java.util.Date(ts * 1000)
    return String.format(Locale.CHINA, "%tF", d)
}

/** 迷你号等长数字，用于卡片内展示 */
fun Long?.uinText(): String = if (this == null || this <= 0) "-" else this.toString()

/** 字节数 → 适合展示的容量文本（KB / MB / GB） */
fun formatSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1 -> String.format(Locale.US, "%.2f GB", gb)
        mb >= 1 -> String.format(Locale.US, "%.1f MB", mb)
        kb >= 1 -> String.format(Locale.US, "%.0f KB", kb)
        else -> "$bytes B"
    }
}
