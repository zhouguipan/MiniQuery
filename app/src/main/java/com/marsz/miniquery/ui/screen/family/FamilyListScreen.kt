package com.marsz.miniquery.ui.screen.family

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.marsz.miniquery.data.model.FamilyDetail
import com.marsz.miniquery.ui.component.AppScaffold
import com.marsz.miniquery.ui.component.EmptyState
import com.marsz.miniquery.ui.component.SectionCard
import com.marsz.miniquery.ui.component.StateContent
import com.marsz.miniquery.ui.theme.Dimens
import com.marsz.miniquery.vm.MainViewModel
import com.marsz.miniquery.vm.TabState

/**
 * 家族列表页。
 *
 * 这里**只请求家族本身**，成员资料一律留到详情页再按滚动位置分页拉取，
 * 所以进入列表只发一个请求，滑动也不会被后台批量拉资料拖慢。
 *
 * 平板 / 折叠屏展开态下自动变成左右两栏：左侧列表、右侧详情，点击即时切换。
 */
@Composable
fun FamilyListScreen(
    vm: MainViewModel,
    twoPane: Boolean,
    onBack: () -> Unit,
    onOpenDetail: (Long) -> Unit,
    onQueryMember: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // 只在此处订阅一次 StateFlow；TabState<List<FamilyDetail>> 是原工程真实类型
    val families: TabState<List<FamilyDetail>> by vm.families.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { vm.loadFamilies() }

    var selectedId by remember { mutableStateOf<Long?>(null) }
    val selected = selectedId?.let { id -> families.data.orEmpty().firstOrNull { it.id == id } }

    AppScaffold(
        title = "家族",
        onBack = onBack,
        modifier = modifier
    ) { padding ->
        if (twoPane) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = Dimens.ScreenPadding, vertical = 8.dp)
            ) {
                Box(modifier = Modifier.weight(0.4f)) {
                    // 两栏模式下列表自带外边距，右侧详情贴边即可，避免中间出现双倍留白
                    FamilyListBody(
                        families = families,
                        highlightedId = selectedId,
                        onOpenDetail = { selectedId = it },
                        onRetry = { vm.loadFamilies(force = true) },
                        contentPadding = PaddingValues(0.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(0.6f)
                        .fillMaxHeight()
                        .padding(start = Dimens.CardGap)
                ) {
                    if (selected != null) {
                        FamilyDetailPane(
                            vm = vm,
                            family = selected,
                            onQueryMember = onQueryMember,
                            horizontalPadding = 0.dp
                        )
                    } else {
                        EmptyState(
                            text = "从左侧选择一个家族\n查看详细信息与成员",
                            icon = Icons.Outlined.Groups,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                FamilyListBody(
                    families = families,
                    highlightedId = null,
                    onOpenDetail = onOpenDetail,
                    onRetry = { vm.loadFamilies(force = true) }
                )
            }
        }
    }
}

@Composable
private fun FamilyListBody(
    families: TabState<List<FamilyDetail>>,
    highlightedId: Long?,
    onOpenDetail: (Long) -> Unit,
    onRetry: () -> Unit = {},
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(
        horizontal = Dimens.ScreenPadding,
        vertical = 6.dp
    )
) {
    // 卡顿优化：此处不再 collectAsStateWithLifecycle(vm.families)，改为由外层传入，
    // 避免同一 StateFlow 被父子组件各订阅一次 → 每次更新双重组。

    StateContent(
        state = families,
        modifier = modifier,
        emptyText = "该玩家还没有加入家族",
        emptyIcon = Icons.Outlined.Groups,
        onRetry = onRetry
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(Dimens.CardGap)
        ) {
            // 显式标注 lambda 参数类型 FamilyDetail，杜绝 items(Array)/items(List) 重载歧义；
            // 同时声明稳定 contentType，提升滚动复用时帧率。
            items(
                items = families.data.orEmpty(),
                key = { it.id },
                contentType = { "family_item" },
            ) { family: FamilyDetail ->
                FamilyListItem(
                    family = family,
                    highlighted = family.id == highlightedId,
                    onClick = { onOpenDetail(family.id) },
                    modifier = Modifier.animateItem()
                )
            }
        }
    }
}

@Composable
fun FamilyListItem(
    family: FamilyDetail,
    highlighted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    SectionCard(
        modifier = modifier,
        containerColor = if (highlighted) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surface,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.CardPadding)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FamilyAvatar(
                    flagUrl = family.header_flagm_url,
                    headerUrl = family.header_url,
                    headerType = family.header_type,
                    size = 56.dp
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = family.name?.takeIf { it.isNotBlank() } ?: "未命名家族",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "ID ${family.id} · Lv.${family.level ?: "0"} · ${family.member_count ?: "-"} 人",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(18.dp)
                )
            }

            family.desc?.takeIf { it.isNotBlank() }?.let { desc ->
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }
        }
    }
}
