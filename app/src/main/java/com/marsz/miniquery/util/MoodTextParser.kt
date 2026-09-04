package com.marsz.miniquery.util

/**
 * moodText 解析器。
 *
 * 原 HTML 逻辑：
 *  1. 颜色简写 #Y/#G/#W/#R/#K/#B 展开为 #cXXXXXX
 *  2. 统一换行符（\r\n、\r、字面 \n）
 *  3. 按正则 (#A\d{3})|#c([0-9a-fA-F]{6})|#n|#r|#[PpL] 逐行解析
 *  4. 表情码替换为图集切图，颜色码开启新的颜色区间，#n/#r 为软换行
 */
object MoodTextParser {

    private val COLOR_ALIAS: List<Pair<String, String>> = listOf(
        "#Y" to "#cffff00",
        "#G" to "#c00ff00",
        "#W" to "#cffffff",
        "#R" to "#cff0000",
        "#K" to "#c000000",
        "#B" to "#c0000ff"
    )

    private val TOKEN = Regex("(#A\\d{3})|#c([0-9a-fA-F]{6})|#n|#r|#[PpL]")

    fun parse(raw: String?): List<MoodPiece> {
        if (raw.isNullOrEmpty()) return emptyList()

        var s = raw
        for ((alias, full) in COLOR_ALIAS) {
            if (s != null) {
                s = s.replace(alias, full)
            }
        }
        if (s != null) {
            s = s
                .replace("\\r\\n", "\n")
                .replace("\\n", "\n")
                .replace("\r\n", "\n")
                .replace('\r', '\n')
        }

        val result = mutableListOf<MoodPiece>()
        val lines = s?.split("\n")
        if (lines != null) {
            lines.forEachIndexed { index, line ->
                if (index > 0) result += MoodPiece.NewLine
                parseLine(line, result)
            }
        }
        return result
    }

    private fun parseLine(line: String, out: MutableList<MoodPiece>) {
        var last = 0
        var colorHex: String? = null

        TOKEN.findAll(line).forEach { m ->
            val before = line.substring(last, m.range.first)
            if (before.isNotEmpty()) out += MoodPiece.Text(before, colorHex)

            val emojiCode = m.groupValues[1]
            val hex = m.groupValues[2]
            val matched = m.value

            when {
                emojiCode.isNotEmpty() -> {
                    val sprite = EmojiAtlas.CODE_ICON[emojiCode]
                    if (sprite != null) out += MoodPiece.Emoji(sprite, emojiCode)
                    else out += MoodPiece.Text(emojiCode, colorHex)
                }
                hex.isNotEmpty() -> colorHex = hex
                matched == "#n" || matched == "#r" -> out += MoodPiece.NewLine
                // #P / #p / #L 原样吞掉
            }
            last = m.range.last + 1
        }

        val after = line.substring(last)
        if (after.isNotEmpty()) out += MoodPiece.Text(after, colorHex)
    }
}

sealed interface MoodPiece {
    data class Text(val value: String, val colorHex: String?) : MoodPiece
    data class Emoji(val sprite: String, val code: String) : MoodPiece
    object NewLine : MoodPiece
}

/** 把 #RRGGBB 解析为 0xAARRGGBB，失败返回 null */
fun parseHexColor(hex: String?): Long? {
    if (hex.isNullOrEmpty()) return null
    return runCatching { android.graphics.Color.parseColor("#$hex").toLong() and 0xFFFFFFFFL }
        .getOrNull()
}
