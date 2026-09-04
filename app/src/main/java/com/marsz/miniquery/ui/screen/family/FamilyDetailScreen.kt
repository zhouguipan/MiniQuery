package com.marsz.miniquery.ui.screen.family

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.marsz.miniquery.data.model.FamilyDetail
import com.marsz.miniquery.ui.component.AppScaffold
import com.marsz.miniquery.ui.component.EmptyState
import com.marsz.miniquery.ui.component.MemberRowView
import com.marsz.miniquery.ui.component.SectionCard
import com.marsz.miniquery.ui.component.SectionTitle
import com.marsz.miniquery.ui.theme.Dimens
import com.marsz.miniquery.vm.FamilyMemberState
import com.marsz.miniquery.vm.MainViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** 距列表尾部还剩多少项时就开始预取下一页 */
private const val PREFETCH_AHEAD = 10

/**
 * 家族详情 + 成员列表。
 *
 * 成员不是点一下才加载：列表滚动到接近尾部时自动追加下一页，
 * 滑到底时新数据通常已经在路上了，体感上是"一直能滑"。
 */
@Composable
fun FamilyDetailPane(
    vm: MainViewModel,
    family: FamilyDetail,
    onQueryMember: (String) -> Unit,
    modifier: Modifier = Modifier,
    /** 外层容器已加过左右边距时传 0，避免双重留白 */
    horizontalPadding: Dp = Dimens.ScreenPadding
) {
    val memberStates by vm.familyMembers.collectAsStateWithLifecycle()
    val state = memberStates[family.id] ?: FamilyMemberState()
    val listState = rememberLazyListState()

    // 进入页面先取第一页；VM 内部有防重，重复调用是安全的
    LaunchedEffect(family.id) {
        vm.loadFamilyMemberPage(family)
    }

    // 用 rememberUpdatedState 读取最新状态，避免把整个 state 作为 LaunchedEffect 的 key
    // 导致每次分页都重启协程（重启会丢失滚动监听的连续性）
    val currentState by rememberUpdatedState(state)
    val currentFamily by rememberUpdatedState(family)
    LaunchedEffect(listState) {
        snapshotFlow {
            val info = listState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            lastVisible to info.totalItemsCount
        }
            .map { (lastVisible, total) ->
                lastVisible >= 0 && total > 0 && lastVisible >= total - PREFETCH_AHEAD
            }
            .distinctUntilChanged()
            .collect { needMore ->
                if (needMore) {
                    val st = currentState
                    if (!st.loading && !st.allLoaded) {
                        vm.loadFamilyMemberPage(currentFamily)
                    }
                }
            }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = horizontalPadding, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(Dimens.CardGap)
    ) {
        item(key = "detail_header") {
            SectionCard {
                Column(modifier = Modifier.padding(Dimens.CardPadding)) {
                    FamilyDetailHeader(family = family)
                }
            }
        }

        item(key = "member_header") {
            SectionCard {
                Column(modifier = Modifier.padding(Dimens.CardPadding)) {
                    SectionTitle(
                        text = "成员",
                        icon = Icons.Outlined.Groups,
                        trailing = {
                            Text(
                                text = if (state.total > 0) "${state.rows.size} / ${state.total}" else "-",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                }
            }
        }

        if (state.rows.isEmpty() && !state.loading) {
            item(key = "member_empty") {
                EmptyState(
                    text = "该家族暂无成员信息",
                    icon = Icons.Outlined.Groups,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        itemsIndexed(
            items = state.rows,
            key = { index, member -> "${member.uin}_$index" }
        ) { _, member ->
            SectionCard {
                MemberRowView(
                    member = member,
                    onQuery = onQueryMember,
                    highlighted = member.uin == family.leader_uin?.toString()
                )
            }
        }

        if (state.loading) {
            item(key = "loading") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
                }
            }
        }

        if (state.allLoaded && state.rows.isNotEmpty()) {
            item(key = "all_loaded") {
                Text(
                    text = "已显示全部 ${state.rows.size} 位成员",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/** 手机上独立成页的家族详情 */
@Composable
fun FamilyDetailScreen(
    vm: MainViewModel,
    familyId: Long,
    onBack: () -> Unit,
    onQueryMember: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val families by vm.families.collectAsStateWithLifecycle()
    val family = families.data.orEmpty().firstOrNull { it.id == familyId }

    AppScaffold(
        title = family?.name?.takeIf { it.isNotBlank() } ?: "家族详情",
        onBack = onBack,
        modifier = modifier
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Dimens.ScreenPadding, vertical = 8.dp)
        ) {
            if (family == null) {
                EmptyState(
                    text = "家族信息已失效，请返回重试",
                    icon = Icons.Outlined.Groups,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                FamilyDetailPane(
                    vm = vm,
                    family = family,
                    onQueryMember = onQueryMember,
                    horizontalPadding = 0.dp
                )
            }
        }
    }
}
