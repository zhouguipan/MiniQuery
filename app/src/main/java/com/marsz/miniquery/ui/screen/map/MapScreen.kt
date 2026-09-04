package com.marsz.miniquery.ui.screen.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.marsz.miniquery.cache.CacheCategory
import com.marsz.miniquery.data.model.MapItem
import com.marsz.miniquery.data.net.Api
import com.marsz.miniquery.ui.component.AppScaffold
import com.marsz.miniquery.ui.component.CachedImage
import com.marsz.miniquery.ui.component.ImageViewerDialog
import com.marsz.miniquery.ui.component.StateContent
import com.marsz.miniquery.ui.theme.Dimens
import com.marsz.miniquery.vm.MainViewModel

/**
 * 地图页。
 *
 * 改成封面卡片：16:9 封面 + 名称 + 简介两行，
 * 平板上自动扩展成三四列，不再是一条条挤在一起的窄行。
 */
@Composable
fun MapScreen(
    vm: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val maps by vm.maps.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.loadMaps() }

    val list = maps.data.orEmpty()
    var viewerIndex by remember { mutableStateOf<Int?>(null) }

    AppScaffold(title = "地图", onBack = onBack, modifier = modifier) { padding ->
        StateContent(
            state = maps,
            modifier = Modifier.padding(padding),
            emptyText = "该玩家暂无地图作品",
            emptyIcon = Icons.Outlined.Map,
            onRetry = { vm.loadMaps(force = true) }
        ) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = Dimens.GridMinColumnLarge),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    horizontal = Dimens.ScreenPadding,
                    vertical = 8.dp
                ),
                verticalArrangement = Arrangement.spacedBy(Dimens.CardGap),
                horizontalArrangement = Arrangement.spacedBy(Dimens.CardGap)
            ) {
                itemsIndexed(
                    items = list,
                    key = { index, item -> "${item.name}_$index" }
                ) { index, map ->
                    MapCard(
                        map = map,
                        onClick = { viewerIndex = index }
                    )
                }
            }
        }
    }

    viewerIndex?.let { index ->
        ImageViewerDialog(
            urls = list.map { it.cover_url ?: Api.unknownMapCover },
            initialIndex = index,
            category = CacheCategory.MAP,
            titleOf = { list.getOrNull(it)?.name ?: "" },
            onDismiss = { viewerIndex = null }
        )
    }
}

@Composable
private fun MapCard(
    map: MapItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier
    ) {
        Column {
            CachedImage(
                url = map.cover_url ?: Api.unknownMapCover,
                category = CacheCategory.MAP,
                contentDescription = map.name,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 11.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = map.name?.takeIf { it.isNotBlank() } ?: "未命名地图",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = map.memo?.takeIf { it.isNotBlank() } ?: "暂无简介",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
