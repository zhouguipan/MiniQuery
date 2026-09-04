package com.marsz.miniquery.ui.screen.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.marsz.miniquery.cache.CacheCategory
import com.marsz.miniquery.data.prefs.AppPrefs
import com.marsz.miniquery.data.prefs.ThemeMode
import com.marsz.miniquery.ui.component.AppScaffold
import com.marsz.miniquery.ui.component.SectionCard
import com.marsz.miniquery.ui.component.SectionTitle
import com.marsz.miniquery.ui.theme.Dimens
import com.marsz.miniquery.util.SecurityStatus
import com.marsz.miniquery.util.formatSize

/**
 * 设置页。
 * 主题模式 / 分类缓存管理 / 隐私与安全 / 关于入口。
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenPrivacy: () -> Unit,
    modifier: Modifier = Modifier,
    cacheVm: CacheSettingsViewModel = viewModel()
) {
    val sizes by cacheVm.sizes.collectAsStateWithLifecycle()
    val total by cacheVm.total.collectAsStateWithLifecycle()
    val busy by cacheVm.busy.collectAsStateWithLifecycle()

    val context = LocalContext.current

    var showThemeDialog by remember { mutableStateOf(false) }
    var showClearAllDialog by remember { mutableStateOf(false) }

    AppScaffold(title = "设置", onBack = onBack, modifier = modifier) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Dimens.ScreenPadding, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(Dimens.CardGap)
        ) {
            /* ==================== 外观 ==================== */
            item(key = "appearance") {
                SectionCard {
                    Column(modifier = Modifier.padding(Dimens.CardPadding)) {
                        SectionTitle(text = "外观", icon = Icons.Outlined.Palette)

                        SettingsItem(
                            title = "主题模式",
                            subtitle = AppPrefs.themeMode.label,
                            onClick = { showThemeDialog = true }
                        )
                    }
                }
            }

            /* ==================== 缓存 ==================== */
            item(key = "cache") {
                SectionCard {
                    Column(modifier = Modifier.padding(Dimens.CardPadding)) {
                        SectionTitle(
                            text = "缓存管理",
                            icon = Icons.Outlined.Storage,
                            trailing = {
                                if (busy) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text(
                                        text = formatSize(total),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        )

                        // 缓存项数量固定且很少，直接展开即可，无需再嵌一层懒加载
                        CacheCategory.entries.forEach { category ->
                            CacheItem(
                                label = category.label,
                                size = sizes[category] ?: 0L,
                                onClear = { cacheVm.clear(category) }
                            )
                        }

                        TextButton(
                            onClick = { showClearAllDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CleaningServices,
                                contentDescription = null,
                                modifier = Modifier.size(17.dp)
                            )
                            Text(
                                text = "  清空全部缓存",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }

            /* ==================== 隐私与安全 ==================== */
            item(key = "privacy") {
                SectionCard {
                    Column(modifier = Modifier.padding(Dimens.CardPadding)) {
                        SectionTitle(text = "隐私与安全", icon = Icons.Outlined.Security)

                        SettingsItem(
                            title = "隐私政策",
                            subtitle = "查看我们如何保护你的数据",
                            onClick = onOpenPrivacy
                        )

                        SecurityStatusItem()
                    }
                }
            }

            /* ==================== 关于 ==================== */
            item(key = "about") {
                SectionCard(onClick = onOpenAbout) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Dimens.CardPadding),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "关于",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "作者与联系方式",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.Outlined.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }

    if (showThemeDialog) {
        ThemeDialog(
            current = AppPrefs.themeMode,
            onDismiss = { showThemeDialog = false },
            onSelect = {
                AppPrefs.setThemeMode(context, it)
                showThemeDialog = false
            }
        )
    }

    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            title = { Text("清空全部缓存") },
            text = { Text("将删除全部图片缓存（共 ${formatSize(total)}），下次查看需要重新加载。") },
            confirmButton = {
                TextButton(onClick = {
                    cacheVm.clearAll()
                    showClearAllDialog = false
                }) {
                    Text("清空")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

/* ==================== 子组件 ==================== */

@Composable
private fun SettingsItem(
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                subtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun CacheItem(
    label: String,
    size: Long,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = formatSize(size),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        TextButton(onClick = onClear) {
            Text("清理", style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun ThemeDialog(
    current: ThemeMode,
    onDismiss: () -> Unit,
    onSelect: (ThemeMode) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("主题模式") },
        text = {
            Column {
                ThemeMode.entries.forEach { mode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = mode == current,
                            onClick = { onSelect(mode) }
                        )
                        Text(
                            text = mode.label,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 6.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

/**
 * 安全环境状态。
 *
 * 金标安全检测关注点：调试环境、Root、代理、模拟器都可能被用于二次打包或流量劫持。
 * 这里仅做**本地只读检测**并展示给用户，检测结果不上传任何服务器；
 * 发现风险时在设置页给出醒目提示，提醒用户注意账号安全。
 */
@Composable
private fun SecurityStatusItem() {
    val status = remember { SecurityStatus.detect() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.Security,
            contentDescription = null,
            tint = if (status.safe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            modifier = Modifier.size(20.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (status.safe) "运行环境正常" else "检测到风险环境",
                style = MaterialTheme.typography.bodyMedium,
                color = if (status.safe) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.error
            )
            Text(
                text = status.summary,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
