package com.marsz.miniquery.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.marsz.miniquery.cache.CacheCategory

/**
 * 带头像框的头像。
 *
 * 头像框由服务端下发，可能是 png 也可能是 gif：
 * 这里不做任何格式假设，交给图片加载器按实际内容解码，动图可直接播放。
 * 头像框加载失败时不绘制任何东西，只露出底下的头像，避免出现灰块。
 */
@Composable
fun HeadframeAvatar(
    avatarUrl: String?,
    headframeUrl: String?,
    size: Dp,
    modifier: Modifier = Modifier,
    showFrame: Boolean = true
) {
    val avatarSize = size * 0.76f
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        CachedImage(
            url = avatarUrl,
            category = CacheCategory.AVATAR,
            contentDescription = "头像",
            contentScale = ContentScale.Crop,
            shape = RoundedCornerShape(size * 0.2f),
            modifier = Modifier.size(avatarSize)
        )
        if (showFrame && !headframeUrl.isNullOrBlank()) {
            CachedImage(
                url = headframeUrl,
                category = CacheCategory.HEADFRAME,
                contentDescription = "头像框",
                contentScale = ContentScale.Fit,
                showPlaceholder = false,
                modifier = Modifier.size(size)
            )
        }
    }
}

/** 小尺寸头像，用于成员列表等密集场景 */
@Composable
fun SmallAvatar(
    avatarUrl: String?,
    headframeUrl: String?,
    modifier: Modifier = Modifier,
    size: Dp = 46.dp
) {
    HeadframeAvatar(
        avatarUrl = avatarUrl,
        headframeUrl = headframeUrl,
        size = size,
        modifier = modifier,
        showFrame = true
    )
}
