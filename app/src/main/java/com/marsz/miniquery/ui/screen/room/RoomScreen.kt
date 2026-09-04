package com.marsz.miniquery.ui.screen.room

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.MeetingRoom
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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

    // 刷新中的旋转状态，仅用于控制按钮的转圈动画，不影响数据本身
    var refreshing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { vm.loadRoom() }
    // 数据返回后收起转圈；无论成功失败，ViewModel 都会把 roomLoaded 置回 true
    LaunchedEffect(roomLoaded) { if (roomLoaded) refreshing = false }

    val inRoom = isInRoom(room?.room_status)

    AppScaffold(
        title = "房间",
        onBack = onBack,
        modifier = modifier,
        actions = {
            if (currentUin != null && inRoom) {
                if (refreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    IconButton(
                        onClick = {
                            refreshing = true
                            vm.loadRoom(force = true)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "刷新房间信息"
                        )
                    }
                }
            }
        }
    ) { padding ->
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
                                            "客户端版本" to info?.version.dash(),
                                            "设备" to info?.device.dash(),
                                            "创建时间" to info?.create_time.dash(),
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
                                text = "房间信息由官方接口提供，可能有延迟",
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
