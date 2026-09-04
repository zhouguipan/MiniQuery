package com.marsz.miniquery.ui.screen.relation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.marsz.miniquery.data.model.MemberRow
import com.marsz.miniquery.ui.component.MemberRowView
import com.marsz.miniquery.vm.MainViewModel
import com.marsz.miniquery.vm.TabState
import kotlinx.coroutines.flow.StateFlow

/**
 * 关系列表通用页：好友 / 关注 / 黑名单 共用同一套 UI。
 *
 * - 列表行直接复用 [MemberRowView]，与家族成员页视觉完全一致；
 * - 昵称原样显示（ViewModel 转换 MemberRow 时已保留 nickname 原始值，不做 BBCode 剥离）；
 * - uin < 1000 的迷你号已由 ViewModel 过滤，这里只负责渲染。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RelationListScreen(
    title: String,
    state: StateFlow<TabState<List<MemberRow>>>,
    load: (force: Boolean) -> Unit,
    vm: MainViewModel,
    onQueryMember: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tabState by state.collectAsStateWithLifecycle()

    // 进入页面加载；ViewModel 内部有防重，重复调用安全
    LaunchedEffect(Unit) { load(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                // 加载中（首次，尚无数据）
                tabState.loading && tabState.data.isNullOrEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                // 出错
                tabState.error != null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = tabState.error ?: "加载失败",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }

                // 空数据
                tabState.data.isNullOrEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "${title}为空",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }

                // 成功：渲染列表
                else -> {
                    val rows = tabState.data!!
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            horizontal = 12.dp, vertical = 6.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        itemsIndexed(
                            items = rows,
                            key = { _, member -> member.uin },
                        ) { _, member ->
                            MemberRowView(
                                member = member,
                                onQuery = onQueryMember,
                            )
                        }
                    }
                }
            }
        }
    }
}
