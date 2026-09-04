package com.marsz.miniquery.ui.screen.skin

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
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.marsz.miniquery.cache.CacheCategory
import com.marsz.miniquery.data.model.SkinItem
import com.marsz.miniquery.data.net.Api
import com.marsz.miniquery.ui.component.AppScaffold
import com.marsz.miniquery.ui.component.CachedImage
import com.marsz.miniquery.ui.component.ImageViewerDialog
import com.marsz.miniquery.ui.component.StateContent
import com.marsz.miniquery.ui.theme.Dimens
import com.marsz.miniquery.vm.MainViewModel

/**
 * 皮肤页。
 *
 * 列表默认**全部展开**，不再有"展开 / 收起"开关：
 * 网格本身就是惰性布局，几百个皮肤也只有可见的那几项会被加载，
 * 既省内存又不会有折叠时的跳动。
 */
@Composable
fun SkinScreen(
    vm: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val skins by vm.skins.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.loadSkins() }

    val list = skins.data.orEmpty()
    var viewerIndex by remember { mutableStateOf<Int?>(null) }

    AppScaffold(title = "皮肤", onBack = onBack, modifier = modifier) { padding ->
        StateContent(
            state = skins,
            modifier = Modifier.padding(padding),
            emptyText = "该玩家暂无皮肤",
            emptyIcon = Icons.Outlined.Checkroom,
            onRetry = { vm.loadSkins(force = true) }
        ) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = Dimens.GridMinColumn),
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
                    key = { index, item -> "${item.id}_${item.name}_$index" }
                ) { index, skin ->
                    SkinCard(
                        skin = skin,
                        onClick = { viewerIndex = index }
                    )
                }
            }
        }
    }

    viewerIndex?.let { index ->
        ImageViewerDialog(
            urls = list.map { Api.avatar(it.head) },
            initialIndex = index,
            category = CacheCategory.SKIN,
            titleOf = { list.getOrNull(it)?.name ?: "" },
            onDismiss = { viewerIndex = null }
        )
    }
}

@Composable
private fun SkinCard(
    skin: SkinItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
    ) {
        Column {
            CachedImage(
                url = Api.avatar(skin.head),
                category = CacheCategory.SKIN,
                contentDescription = skin.name,
                contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .padding(8.dp)
            )
            Text(
                text = skin.name?.takeIf { it.isNotBlank() } ?: "未命名",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp)
                    .padding(bottom = 8.dp)
            )
        }
    }
}
