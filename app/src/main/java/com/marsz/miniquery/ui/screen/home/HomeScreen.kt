package com.marsz.miniquery.ui.screen.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.PersonSearch
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.marsz.miniquery.ui.component.EmptyState
import com.marsz.miniquery.ui.component.InfoGrid
import com.marsz.miniquery.ui.component.LoadingState
import com.marsz.miniquery.ui.component.MemberRowView
import com.marsz.miniquery.ui.component.SectionCard
import com.marsz.miniquery.ui.component.SectionTitle
import com.marsz.miniquery.ui.nav.Routes
import com.marsz.miniquery.ui.theme.Dimens
import com.marsz.miniquery.util.dash
import com.marsz.miniquery.util.formatNum
import com.marsz.miniquery.vm.MainViewModel

/**
 * 首页（基础页）。
 *
 * 顶部只保留一行搜索栏，所有功能以入口卡片的形式放在资料下方，
 * 点进去是独立页面、可返回，不再用占据一整行的 Tab 切换。
 */
@Composable
fun HomeScreen(
    vm: MainViewModel,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val input by vm.input.collectAsStateWithLifecycle()
    val history by vm.history.collectAsStateWithLifecycle()
    val searching by vm.searching.collectAsStateWithLifecycle()
    val profile by vm.profile.collectAsStateWithLifecycle()
    val tracking by vm.tracking.collectAsStateWithLifecycle()
    val dev by vm.dev.collectAsStateWithLifecycle()
    val teamMembers by vm.teamMembers.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.ScreenPadding, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SearchBar(
                value = input,
                onValueChange = vm::onInputChanged,
                onSearch = { vm.search() },
                history = history,
                onPickHistory = { vm.search(it) },
                onRemoveHistory = vm::removeHistory,
                searching = searching,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { onNavigate(Routes.SETTINGS) }) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = "设置"
                )
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                searching -> LoadingState(modifier = Modifier.align(Alignment.Center))
                profile == null -> EmptyState(
                    text = "输入迷你号，查询玩家资料",
                    icon = Icons.Outlined.PersonSearch,
                    modifier = Modifier.align(Alignment.Center)
                )
                else -> {
                    val p = profile!!
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .navigationBarsPadding(),
                        contentPadding = PaddingValues(
                            horizontal = Dimens.ScreenPadding,
                            vertical = 4.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(Dimens.CardGap)
                    ) {
                        item(key = "header") {
                            UserHeaderCard(profile = p)
                        }

                        item(key = "entries") {
                            EntryGrid(onNavigate = onNavigate)
                        }

                        item(key = "base_info") {
                            SectionCard {
                                Column(modifier = Modifier.padding(Dimens.CardPadding)) {
                                    SectionTitle(text = "基础资料", icon = Icons.Outlined.Smartphone)
                                    InfoGrid(
                                        items = listOf(
                                            "等级" to p.level.dash(),
                                            "粉丝" to formatNum(p.fans),
                                            "关注" to formatNum(p.following),
                                            "人气" to formatNum(p.popularity),
                                            "信用分" to p.creditScore.dash(),
                                            "魅力值" to formatNum(p.charmValue),
                                            "获赞" to formatNum(p.thumbs_up),
                                            "IP 属地" to p.IP.dash(),
                                            "注册时间" to p.regist_account_time.dash(),
                                            "最后登录" to p.last_login_time.dash()
                                        )
                                    )
                                }
                            }
                        }

                        tracking?.let { t ->
                            item(key = "tracking") {
                                SectionCard {
                                    Column(modifier = Modifier.padding(Dimens.CardPadding)) {
                                        SectionTitle(text = "当前状态", icon = Icons.Outlined.Smartphone)
                                        InfoGrid(
                                            items = listOf(
                                                "状态" to t.status?.text.dash(),
                                                "客户端版本" to t.client_info?.version.dash(),
                                                "接口标识" to t.client_info?.apiid.dash()
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        dev?.let { d ->
                            item(key = "dev") {
                                SectionCard {
                                    Column(modifier = Modifier.padding(Dimens.CardPadding)) {
                                        SectionTitle(
                                            text = "创作者",
                                            icon = Icons.Outlined.Brush,
                                            trailing = {
                                                p.developerLevel?.takeIf { it.isNotBlank() }?.let {
                                                    androidx.compose.material3.Text(
                                                        text = "Lv.$it",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }
                                        )
                                        InfoGrid(
                                            items = listOf(
                                                "作品数" to d.opus.dash(),
                                                "总下载" to formatNum(d.all_download_count),
                                                "当前游玩" to formatNum(d.nowPlays),
                                                "作品获赞" to formatNum(d.like),
                                                "总计" to formatNum(d.total)
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        if (teamMembers.isNotEmpty()) {
                            item(key = "team") {
                                SectionCard {
                                    Column(modifier = Modifier.padding(Dimens.CardPadding)) {
                                        SectionTitle(text = "当前队伍", icon = Icons.Outlined.Group)
                                        teamMembers.forEachIndexed { index, member ->
                                            if (index > 0) HorizontalDivider(
                                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                            )
                                            MemberRowView(
                                                member = member,
                                                onQuery = { vm.search(it) },
                                                highlighted = member.uin == p.uin.toString(),
                                                modifier = Modifier.padding(vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        item(key = "bottom_space") {
                            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(bottom = 12.dp))
                        }
                    }
                }
            }
        }
    }
}
