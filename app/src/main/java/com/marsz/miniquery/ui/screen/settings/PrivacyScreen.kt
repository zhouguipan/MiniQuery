package com.marsz.miniquery.ui.screen.settings

import android.annotation.SuppressLint
import android.content.Context
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.marsz.miniquery.ui.component.AppScaffold

/**
 * 隐私政策页。
 *
 * 金标认证审核必需项：应用内必须能访问到完整隐私政策。
 * 用 WebView 加载内置 assets，离线可用、不依赖网络，也不触发额外权限。
 *
 * 安全约束：
 * - 关闭 JS（`setJavaScriptEnabled(false)`）：纯静态 HTML，无需脚本，杜绝 XSS / 远程代码风险；
 * - 只允许加载 `file:///android_asset/` 与 `about:`  scheme，其他链接一律在外部浏览器打开；
 * - 禁用文件访问 / 同源绕过，避免 WebView 被利用读取应用私有数据。
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PrivacyScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    AppScaffold(title = "隐私政策", onBack = onBack, modifier = modifier) { padding ->
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            factory = { context: Context ->
                WebView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    settings.apply {
                        javaScriptEnabled = false
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        builtInZoomControls = true
                        displayZoomControls = false
                        // 金标安全：禁止 WebView 访问文件系统
                        allowFileAccess = false
                        allowContentAccess = false
                        allowFileAccessFromFileURLs = false
                        allowUniversalAccessFromFileURLs = false
                    }
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): Boolean {
                            val url = request?.url?.toString().orEmpty()
                            // 仅允许加载应用内 assets；外链交给系统浏览器
                            if (url.startsWith("file:///android_asset/") ||
                                url.startsWith("about:")
                            ) {
                                return false
                            }
                            openInBrowser(context, url)
                            return true
                        }
                    }
                    loadUrl("file:///android_asset/privacy_policy.html")
                }
            }
        )
    }
}

private fun openInBrowser(context: Context, url: String) {
    runCatching {
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        }
    }
}
