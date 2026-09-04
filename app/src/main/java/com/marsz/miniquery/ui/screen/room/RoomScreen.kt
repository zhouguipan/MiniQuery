package com.marsz.miniquery.ui.screen.room

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MeetingRoom
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.marsz.miniquery.cache.CacheCategory
import com.marsz.miniquery.ui.component.AppScaffold
import com.marsz.miniquery.ui.component.CachedImage
import com.marsz.miniquery.ui.component.EmptyState
import com.marsz.miniquery.ui.component.InfoGrid
import com.marsz.miniquery.ui.component.LoadingState
import com.marsz.miniquery.ui.component.MemberRowView
import com.marsz.miniquery.ui.component.SectionCard
import com.marsz.miniquery.ui.component.SectionTitle
import com.marsz.miniquery.ui.theme.Dimens
import com.marsz.miniquery.util.dash
import com.marsz.miniquery.util.yesNo
import com.marsz.miniquery.vm.MainViewModel

/**
 * 房间页。
 * 展示当前所在房间的信息与成员；不在房间时给出明确提示，不留空白页。
 */
@Composable
fun RoomScreen(
    vm: MainViewModel,
    onBack: () -> Unit,
    onQueryMember: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val room by vm.room.collectAsStateWithLifecycle()
    val roomLoaded by vm.roomLoaded.collectAsStateWithLifecycle()
    val members by vm.roomMembers.collectAsStateWithLifecycle()
    val currentUin by vm.currentUin.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { vm.loadRoom() }

    AppScaffold(title = "房间", onBack = onBack, modifier = modifier) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                !roomLoaded -> LoadingState(modifier = Modifier.fillMaxWidth())
                !isInRoom(room?.room_status) -> EmptyState(
                    text = "该玩家当前不在房间中",
                    icon = Icons.Outlined.MeetingRoom,
                    modifier = Modifier.fillMaxWidth()
                )
                else -> {
                    val info = room?.room_info
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            horizontal = Dimens.ScreenPadding,
                            vertical = 8.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(Dimens.CardGap)
                    ) {
                        info?.thumbnail?.takeIf { it.isNotBlank() }?.let { thumb ->
                            item(key = "thumb") {
                                SectionCard {
                                    CachedImage(
                                        url = thumb,
                                        category = CacheCategory.MAP,
                                        contentDescription = "房间缩略图",
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(16f / 9f)
                                    )
                                }
                            }
                        }

                        item(key = "info") {
                            SectionCard {
                                Column(modifier = Modifier.padding(Dimens.CardPadding)) {
                                    SectionTitle(
                                        text = info?.room_name.dash(),
                                        icon = Icons.Outlined.SportsEsports
                                    )
                                    InfoGrid(
                                        items = listOf(
                                            "状态" to room?.room_status.dash(),
                                            "模式" to info?.mode.dash(),
                                            "地图" to info?.map_name.dash(),
                                            "人数" to info?.player_count.dash(),
                                            "有密码" to (info?.has_password?.yesNo() ?: "-"),
                                            "可见性" to info?.visibility.dash(),
                                            "版本" to info?.version.dash(),
                                            "开始时间" to info?.start_time.dash()
                                        )
                                    )
                                }
                            }
                        }

                        if (members.isNotEmpty()) {
                            item(key = "members_header") {
                                SectionCard {
                                    Column(modifier = Modifier.padding(Dimens.CardPadding)) {
                                        SectionTitle(
                                            text = "房间成员",
                                            icon = Icons.Outlined.MeetingRoom,
                                            trailing = {
                                                Text(
                                                    text = "${members.size} 人",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        )
                                        HorizontalDivider(
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            }

                            itemsIndexed(
                                items = members,
                                key = { index, member -> "${member.uin}_$index" }
                            ) { _, member ->
                                SectionCard {
                                    MemberRowView(
                                        member = member,
                                        onQuery = onQueryMember,
                                        highlighted = member.uin == currentUin
                                    )
                                }
                            }
                        }

                        item(key = "tip") {
                            Text(
                                text = "房间信息由第三方接口提供，可能有延迟",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/** 服务端用这句文案表示"正在房间内" */
private fun isInRoom(status: String?): Boolean =
    status?.contains("正在房间中游玩") == true || status?.contains("房间") == true
