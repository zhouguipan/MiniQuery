package com.marsz.miniquery.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.marsz.miniquery.ui.theme.Dimens
import com.marsz.miniquery.ui.theme.Offline
import com.marsz.miniquery.ui.theme.Online
import com.marsz.miniquery.ui.theme.RoleCreator
import com.marsz.miniquery.ui.theme.RoleLeader
import com.marsz.miniquery.ui.theme.RoleNormal
import com.marsz.miniquery.ui.theme.RoleSelf
import com.marsz.miniquery.data.model.MemberRole
import kotlin.math.floor

/* ==================== 布局辅助 ==================== */

/**
 * 依据屏幕宽度计算网格列数。
 * 手机两列，平板 / 横屏自动增加列数，避免大屏上被拉成"又宽又空"的一条。
 */
@Composable
fun rememberColumns(minColumnWidth: Dp, maxColumns: Int = 6): Int {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val usable = screenWidth - Dimens.ScreenPadding * 2
    return floor((usable.value / minColumnWidth.value)).toInt().coerceIn(1, maxColumns)
}

/* ==================== 卡片 ==================== */

/** 统一风格的分区卡片，页面内所有白色区块都用它，保证视觉一致 */
@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(Dimens.RadiusLarge)
    if (onClick != null) {
        Card(
            onClick = onClick,
            shape = shape,
            colors = CardDefaults.cardColors(
                containerColor = containerColor,
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            modifier = modifier.fillMaxWidth()
        ) {
            content()
        }
    } else {
        Surface(
            shape = shape,
            color = containerColor,
            modifier = modifier.fillMaxWidth()
        ) {
            content()
        }
    }
}

/** 卡片标题行：小图标 + 文字 */
@Composable
fun SectionTitle(
    text: String,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(17.dp)
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        trailing?.invoke()
    }
}

/* ==================== 信息项 ==================== */

/** 单个信息项：上行标签，下行数值 */
@Composable
fun InfoItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Column(
        modifier = modifier.padding(vertical = 7.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = valueColor,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * 信息网格。
 * 列数随屏幕宽度自适应：手机 2 列，平板 3~4 列。
 */
@Composable
fun InfoGrid(
    items: List<Pair<String, String>>,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return
    val columns = rememberColumns(minColumnWidth = 132.dp, maxColumns = 4)
    Column(modifier = modifier.fillMaxWidth()) {
        items.chunked(columns).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth()) {
                row.forEach { (label, value) ->
                    InfoItem(
                        label = label,
                        value = value,
                        modifier = Modifier.weight(1f)
                    )
                }
                // 末行不足时用空白补齐，保证每格等宽
                repeat(columns - row.size) {
                    Box(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

/* ==================== 标签 ==================== */

/** 状态标签：在线 / 离线 */
@Composable
fun StatusChip(online: Boolean) {
    val (text, color) = if (online) "在线" to Online else "离线" to Offline
    Surface(
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.14f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(50),
                color = color,
                modifier = Modifier.size(6.dp)
            ) {}
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = color
            )
        }
    }
}

/** 角色标签：族长 / 创始人 / 本人 / 成员 */
@Composable
fun RoleChip(role: MemberRole, text: String) {
    val color = when (role) {
        MemberRole.LEADER -> RoleLeader
        MemberRole.CREATOR -> RoleCreator
        MemberRole.SELF -> RoleSelf
        MemberRole.NORMAL -> RoleNormal
    }
    Surface(
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.14f)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

/* ==================== 状态页 ==================== */

/** 空数据 / 错误 / 加载中的统一占位，保证任何页面都不会出现白屏 */
@Composable
fun StateContent(
    state: com.marsz.miniquery.vm.TabState<*>,
    modifier: Modifier = Modifier,
    emptyText: String = "暂无数据",
    emptyIcon: ImageVector = Icons.Outlined.Inbox,
    onRetry: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier.fillMaxSize()) {
        when {
            state.loading && state.data == null -> LoadingState(modifier = Modifier.align(Alignment.Center))
            state.error != null && state.data == null -> ErrorState(
                message = state.error,
                onRetry = onRetry,
                modifier = Modifier.align(Alignment.Center)
            )
            // 数据为空（尚未拉取 / 拉回来就是空列表）都不该出现白屏
            state.data == null || (state.data as? List<*>)?.isEmpty() == true -> EmptyState(
                text = emptyText,
                icon = emptyIcon,
                modifier = Modifier.align(Alignment.Center)
            )
            else -> content()
        }
    }
}

@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        androidx.compose.material3.CircularProgressIndicator(
            modifier = Modifier.size(32.dp),
            strokeWidth = 3.dp
        )
        Text(
            text = "加载中…",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun EmptyState(
    text: String,
    icon: ImageVector = Icons.Outlined.Inbox,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
            modifier = Modifier.size(48.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun ErrorState(
    message: String?,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.ErrorOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
            modifier = Modifier.size(44.dp)
        )
        Text(
            text = message ?: "加载失败",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (onRetry != null) {
            androidx.compose.material3.TextButton(onClick = onRetry) {
                Text("重试")
            }
        }
    }
}
