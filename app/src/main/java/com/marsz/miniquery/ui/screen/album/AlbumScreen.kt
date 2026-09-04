package com.marsz.miniquery.ui.screen.album

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.marsz.miniquery.cache.CacheCategory
import com.marsz.miniquery.ui.component.AppScaffold
import com.marsz.miniquery.ui.component.CachedImage
import com.marsz.miniquery.ui.component.ImageViewerDialog
import com.marsz.miniquery.ui.component.StateContent
import com.marsz.miniquery.ui.theme.Dimens
import com.marsz.miniquery.vm.MainViewModel

/**
 * 相册页。
 * 正方形缩略图网格，点击进入全屏查看，可左右滑动翻页、返回键关闭。
 */
@Composable
fun AlbumScreen(
    vm: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val album by vm.album.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.loadAlbum() }

    val urls = album.data.orEmpty()
    var viewerIndex by remember { mutableStateOf<Int?>(null) }

    AppScaffold(title = "相册", onBack = onBack, modifier = modifier) { padding ->
        StateContent(
            state = album,
            modifier = Modifier.padding(padding),
            emptyText = "该玩家暂无相册图片",
            emptyIcon = Icons.Outlined.PhotoLibrary,
            onRetry = { vm.loadAlbum(force = true) }
        ) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 108.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    horizontal = Dimens.ScreenPadding,
                    vertical = 8.dp
                ),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                itemsIndexed(
                    items = urls,
                    key = { index, url -> "$url#$index" }
                ) { index, url ->
                    Surface(
                        onClick = { viewerIndex = index },
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.aspectRatio(1f)
                    ) {
                        CachedImage(
                            url = url,
                            category = CacheCategory.ALBUM,
                            contentDescription = "相册图片",
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }

    viewerIndex?.let { index ->
        ImageViewerDialog(
            urls = urls,
            initialIndex = index,
            category = CacheCategory.ALBUM,
            onDismiss = { viewerIndex = null }
        )
    }
}
