package com.marsz.miniquery.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.marsz.miniquery.cache.CacheCategory
import com.marsz.miniquery.cache.ImageCache

/**
 * 按分类走缓存的图片组件。
 *
 * - 每个分类使用独立的 ImageLoader（独立内存缓存 + 独立磁盘目录），
 *   设置页可以按分类查看占用并单独清理；
 * - 解码器同时支持 PNG 与 GIF，头像框是动图时可直接播放；
 * - 加载中显示弱色占位块，失败显示占位图标，不会出现空白区域跳动。
 */
@Composable
fun CachedImage(
    url: String?,
    category: CacheCategory,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    shape: Shape? = null,
    /** 为 false 时，加载失败/无图直接不绘制（用于头像框这类叠加层，避免灰块挡住底图） */
    showPlaceholder: Boolean = true,
    placeholderIcon: (@Composable () -> Unit)? = null
) {
    val context = LocalContext.current
    val loader = remember(category) { ImageCache.loader(context, category) }

    var hasError by remember(url) { mutableStateOf(false) }
    var isLoading by remember(url) { mutableStateOf(true) }

    val m = if (shape != null) modifier.clip(shape) else modifier

    Box(contentAlignment = Alignment.Center, modifier = m) {
        when {
            url.isNullOrBlank() -> if (showPlaceholder) ImagePlaceholder(placeholderIcon)
            hasError -> if (showPlaceholder) ImagePlaceholder(placeholderIcon)
            else -> {
                AsyncImage(
                    model = remember(url) {
                        ImageRequest.Builder(context)
                            .data(url)
                            .crossfade(false)
                            .build()
                    },
                    contentDescription = contentDescription,
                    imageLoader = loader,
                    contentScale = contentScale,
                    modifier = Modifier.fillMaxSize(),
                    onSuccess = { isLoading = false; hasError = false },
                    onError = { hasError = true; isLoading = false },
                    onLoading = { isLoading = true }
                )
                if (isLoading && showPlaceholder) {
                    ImagePlaceholder(placeholderIcon)
                }
            }
        }
    }
}

@Composable
private fun BoxScope.ImagePlaceholder(
    placeholderIcon: (@Composable () -> Unit)?
) {
    Box(
        modifier = Modifier
            .matchParentSize()
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            // 装饰性占位：对无障碍服务隐藏，避免 TalkBack 念出"未加标签的图片"
            .clearAndSetSemantics { },
        contentAlignment = Alignment.Center
    ) {
        placeholderIcon?.invoke()
    }
}
