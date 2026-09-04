package com.marsz.miniquery.ui.screen.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.marsz.miniquery.data.net.UserAgent
import com.marsz.miniquery.ui.component.AppScaffold
import com.marsz.miniquery.ui.component.SectionCard
import com.marsz.miniquery.ui.component.SectionTitle
import com.marsz.miniquery.ui.theme.Dimens

private const val AUTHOR = "Marsz"
private const val QQ = "483018259"

/** 关于页：作者、联系方式、版本与设备信息 */
@Composable
fun AboutScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    AppScaffold(title = "关于", onBack = onBack, modifier = modifier) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Dimens.ScreenPadding, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(Dimens.CardGap)
        ) {
            /* ==================== 应用标识 ==================== */
            item(key = "app") {
                SectionCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 26.dp, horizontal = Dimens.CardPadding),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "迷",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                            )
                        }
                        Text(
                            text = "迷你查询",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "迷你世界 · 在线查主页",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            /* ==================== 作者与联系方式 ==================== */
            item(key = "author") {
                SectionCard {
                    Column(modifier = Modifier.padding(Dimens.CardPadding)) {
                        SectionTitle(text = "作者", icon = Icons.Outlined.Person)

                        AboutItem(
                            icon = Icons.Outlined.Person,
                            label = "作者",
                            value = AUTHOR,
                            onClick = { copyToClipboard(context, "作者", AUTHOR) }
                        )
                        AboutItem(
                            icon = Icons.Outlined.AlternateEmail,
                            label = "QQ",
                            value = QQ,
                            onClick = { copyToClipboard(context, "QQ", QQ) },
                            trailingAction = "复制"
                        )
                        AboutItem(
                            icon = Icons.Outlined.Code,
                            label = "QQ 交流",
                            value = "点击跳转 QQ 临时会话",
                            onClick = { openQq(context, QQ) }
                        )
                    }
                }
            }

            /* ==================== 版本与设备 ==================== */
            item(key = "device") {
                SectionCard {
                    Column(modifier = Modifier.padding(Dimens.CardPadding)) {
                        SectionTitle(text = "版本信息", icon = Icons.Outlined.PhoneAndroid)
                        Text(
                            text = UserAgent.get(context),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        )
                        Text(
                            text = "Android ${Build.VERSION.RELEASE}（SDK ${Build.VERSION.SDK_INT}）",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item(key = "footer") {
                Text(
                    text = "数据来自第三方接口，仅供查询参考",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp)
                )
            }
        }
    }
}

@Composable
private fun AboutItem(
    icon: ImageVector,
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailingAction: String? = null
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
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(19.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            trailingAction?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    manager?.setPrimaryClip(ClipData.newPlainText(label, text))
    Toast.makeText(context, "已复制$label", Toast.LENGTH_SHORT).show()
}

/** 拉起 QQ 临时会话；没装 QQ 时降级为复制号码 */
private fun openQq(context: Context, qq: String) {
    val url = "mqqwpa://im/chat?chat_type=wpa&uin=$qq"
    val intent = runCatching { Intent.parseUri(url, Intent.URI_INTENT_SCHEME) }.getOrNull()
    if (intent != null && intent.resolveActivity(context.packageManager) != null) {
        runCatching { context.startActivity(intent) }
            .onFailure { copyToClipboard(context, "QQ", qq) }
    } else {
        // 未安装 QQ：尝试网页版
        val web = Intent(Intent.ACTION_VIEW, Uri.parse("https://wpa.qq.com/msgrd?v=3&uin=$qq&site=qq&menu=yes"))
        runCatching { context.startActivity(web) }
            .onFailure { copyToClipboard(context, "QQ", qq) }
    }
}
