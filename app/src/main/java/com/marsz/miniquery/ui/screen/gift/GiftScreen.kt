package com.marsz.miniquery.ui.screen.gift

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.marsz.miniquery.cache.CacheCategory
import com.marsz.miniquery.data.model.GiftItem
import com.marsz.miniquery.data.net.Api
import com.marsz.miniquery.ui.component.AppScaffold
import com.marsz.miniquery.ui.component.CachedImage
import com.marsz.miniquery.ui.component.StateContent
import com.marsz.miniquery.ui.theme.Dimens
import com.marsz.miniquery.util.formatNum
import com.marsz.miniquery.vm.MainViewModel

/**
 * 礼物页。
 *
 * 卡片式排布：图标居中、下方名称与数量，数量用色块强调，
 * 相比原来的列表行更整齐，数量多少一眼可见。
 */
@Composable
fun GiftScreen(
    vm: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gifts by vm.gifts.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.loadGifts() }

    AppScaffold(title = "礼物", onBack = onBack, modifier = modifier) { padding ->
        StateContent(
            state = gifts,
            modifier = Modifier.padding(padding),
            emptyText = "该玩家暂无礼物",
            emptyIcon = Icons.Outlined.CardGiftcard,
            onRetry = { vm.loadGifts(force = true) }
        ) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 104.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    horizontal = Dimens.ScreenPadding,
                    vertical = 8.dp
                ),
                verticalArrangement = Arrangement.spacedBy(Dimens.CardGap),
                horizontalArrangement = Arrangement.spacedBy(Dimens.CardGap)
            ) {
                itemsIndexed(
                    items = gifts.data.orEmpty(),
                    key = { index, item -> "${item.id}_${item.name}_$index" }
                ) { _, gift ->
                    GiftCard(gift = gift)
                }
            }
        }
    }
}

@Composable
private fun GiftCard(
    gift: GiftItem,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                CachedImage(
                    url = Api.giftIcon(gift.id),
                    category = CacheCategory.GIFT,
                    contentDescription = gift.name,
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                    modifier = Modifier
                        .padding(8.dp)
                        .size(40.dp)
                )
            }
            Text(
                text = gift.name?.takeIf { it.isNotBlank() } ?: "未命名",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = "× ${formatNum(gift.num)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 2.dp)
                )
            }
        }
    }
}
