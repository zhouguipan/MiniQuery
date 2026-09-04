package com.marsz.miniquery.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import com.marsz.miniquery.util.EmojiAtlas
import com.marsz.miniquery.util.MoodPiece
import com.marsz.miniquery.util.MoodTextParser
import com.marsz.miniquery.util.parseHexColor

/**
 * 渲染带表情 / 颜色代码的个性签名。
 *
 * 表情以 InlineTextContent 内嵌，等效于网页里的 <img class="mood-emo">；
 * 图集是异步加载的，加载完成后通过 [EmojiAtlas.version] 变化自动重组一次，
 * 因此首屏不会出现"表情空白"被永久缓存的问题。
 */
@Composable
fun MoodText(
    text: String?,
    modifier: Modifier = Modifier,
    defaultColor: Color = MaterialTheme.colorScheme.onSurface
) {
    val atlasVersion = EmojiAtlas.version

    val pieces = remember(text) { MoodTextParser.parse(text) }
    if (pieces.isEmpty()) {
        Text(
            text = "-",
            style = MaterialTheme.typography.bodyMedium,
            color = defaultColor,
            modifier = modifier
        )
        return
    }

    val annotated = remember(pieces) {
        buildAnnotatedString {
            pieces.forEach { piece ->
                when (piece) {
                    is MoodPiece.Text -> {
                        val color = parseHexColor(piece.colorHex)
                        if (color == null) append(piece.value)
                        else withStyle(SpanStyle(color = Color(color))) { append(piece.value) }
                    }
                    MoodPiece.NewLine -> append('\n')
                    is MoodPiece.Emoji -> appendInlineContent("emo:${piece.sprite}")
                }
            }
        }
    }

    val inlineContent = remember(pieces, atlasVersion) {
        pieces.filterIsInstance<MoodPiece.Emoji>()
            .distinctBy { it.sprite }
            .associate { piece ->
                "emo:${piece.sprite}" to InlineTextContent(
                    placeholder = Placeholder(
                        width = 20.sp,
                        height = 20.sp,
                        placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
                    )
                ) {
                    EmojiAtlas.crop(piece.sprite)?.let { bmp ->
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = piece.code,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
    }

    Text(
        text = annotated,
        inlineContent = inlineContent,
        style = MaterialTheme.typography.bodyMedium,
        color = defaultColor,
        modifier = modifier
    )
}
