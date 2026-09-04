package com.marsz.miniquery.ui.screen.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.marsz.miniquery.data.model.NowProfile
import com.marsz.miniquery.ui.component.HeadframeAvatar
import com.marsz.miniquery.ui.component.MoodText
import com.marsz.miniquery.ui.component.SectionCard
import com.marsz.miniquery.ui.component.StatusChip
import com.marsz.miniquery.ui.component.rememberColumns
import com.marsz.miniquery.ui.theme.Dimens

/**
 * 用户资料卡。
 *
 * 改为横向紧凑排布：头像与文字在同一行，整体高度只有一行多一点，
 * 相比竖排大头像方案省下近一半的纵向空间，小屏手机上内容首屏可见面积明显变大。
 */
@Composable
fun UserHeaderCard(
    profile: NowProfile,
    modifier: Modifier = Modifier
) {
    // 注意：SectionCard 内部是 Surface，而 Surface 的 content 装在 Box 里。
    // Box 的子元素默认都对齐到左上角、互相堆叠，不会自动纵向排列，
    // 所以这里必须显式套一层 Column，否则签名块会盖在昵称行上面。
    SectionCard(modifier = modifier) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimens.CardPadding),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HeadframeAvatar(
                    avatarUrl = profile.avatar_url,
                    headframeUrl = profile.headframe_url ?: profile.headframeID?.let {
                        com.marsz.miniquery.data.net.Api.headframe(it)
                    },
                    size = Dimens.AvatarMedium
                )

                Column(
                    modifier = Modifier
                        .padding(start = 13.dp)
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = profile.nickname?.takeIf { it.isNotBlank() } ?: "未知用户",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        StatusChip(online = profile.online == "在线")
                    }
                    Text(
                        text = "迷你号 ${profile.uin}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 个性签名单独一行，与网页一致地支持颜色码与表情
            profile.moodText?.takeIf { it.isNotBlank() }?.let { mood ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = RoundedCornerShape(Dimens.RadiusSmall),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = Dimens.CardPadding,
                            end = Dimens.CardPadding,
                            bottom = Dimens.CardPadding
                        )
                ) {
                    MoodText(
                        text = mood,
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 9.dp)
                    )
                }
            }
        }
    }
}

/* ==================== 功能入口 ==================== */

/** 首页功能入口。图标 + 名称，点击进入对应页面。 */
enum class HomeEntry(
    val label: String,
    val icon: ImageVector,
    val route: String
) {
    FAMILY("家族", Icons.Outlined.Groups, com.marsz.miniquery.ui.nav.Routes.FAMILY_LIST),
    SKIN("皮肤", Icons.Outlined.Checkroom, com.marsz.miniquery.ui.nav.Routes.SKIN),
    GIFT("礼物", Icons.Outlined.CardGiftcard, com.marsz.miniquery.ui.nav.Routes.GIFT),
    MAP("地图", Icons.Outlined.Map, com.marsz.miniquery.ui.nav.Routes.MAP),
    ALBUM("相册", Icons.Outlined.PhotoLibrary, com.marsz.miniquery.ui.nav.Routes.ALBUM),
    ROOM("房间", Icons.Outlined.SportsEsports, com.marsz.miniquery.ui.nav.Routes.ROOM)
}

@Composable
fun EntryGrid(
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val columns = rememberColumns(minColumnWidth = Dimens.EntryMinColumn, maxColumns = 6)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Dimens.CardGap)
    ) {
        // 按计算出的列数分行，保证任何屏宽下每行都是满的、不会留出半格空白
        HomeEntry.entries.chunked(columns).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimens.CardGap)
            ) {
                row.forEach { entry ->
                    EntryItem(
                        entry = entry,
                        onClick = { onNavigate(entry.route) },
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat(columns - row.size) {
                    androidx.compose.foundation.layout.Box(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun EntryItem(
    entry: HomeEntry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    SectionCard(modifier = modifier, onClick = onClick) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 15.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Icon(
                    imageVector = entry.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .padding(9.dp)
                        .size(22.dp)
                )
            }
            Text(
                text = entry.label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
