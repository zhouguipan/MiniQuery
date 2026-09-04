package com.marsz.miniquery.ui.screen.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * 首次启动的隐私政策同意弹窗。
 *
 * 金标认证与《个人信息保护法》共同要求：涉及个人信息处理前，
 * 必须以显著方式取得用户的**明示同意**。要点：
 * - 弹窗不可绕过（无"关闭"按钮，仅「不同意」与「查看并同意」）；
 * - 「不同意」不退出应用，但禁止进入需要联网查询的功能；
 * - 用户可先查看完整政策再同意，政策页离线可用。
 */
@Composable
fun PrivacyConsentDialog(
    onAgree: () -> Unit,
    onViewPolicy: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { /* 必须主动选择，不允许点外部关闭 */ },
        title = {
            Text(
                text = "欢迎使用迷你查询",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "使用前请阅读并同意《隐私政策》。我们仅在本地缓存必要的查询数据，" +
                        "不收集手机号、位置、通讯录等个人信息，也不会上传任何数据到开发者服务器。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "点击「同意并继续」即表示你已阅读并同意上述政策。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
            ) {
                TextButton(onClick = onViewPolicy) {
                    Text("查看隐私政策")
                }
                TextButton(onClick = onAgree) {
                    Text("同意并继续")
                }
            }
        }
    )
}
