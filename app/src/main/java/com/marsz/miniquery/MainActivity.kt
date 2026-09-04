package com.marsz.miniquery

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.marsz.miniquery.data.prefs.AppPrefs
import com.marsz.miniquery.ui.nav.AppNavHost
import com.marsz.miniquery.ui.nav.Routes
import com.marsz.miniquery.ui.screen.settings.PrivacyConsentDialog
import com.marsz.miniquery.ui.theme.MiniQueryTheme
import com.marsz.miniquery.vm.MainViewModel
import com.marsz.miniquery.vm.UiEvent
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        // 内容绘制到系统栏之下，配合主题里的状态栏处理，任何机型都不会出现突兀的黑条
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            // 展开态与平板走两栏布局，手机与普通折叠态走单页
            val twoPane = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact

            MiniQueryTheme(
                themeMode = AppPrefs.themeMode,
            ) {
                AppRoot(
                    twoPane = twoPane,
                    deepLinkUin = intent?.data?.getQueryParameter("uin")
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // launchMode=singleTop：从浏览器再次唤起时在这里拿到新的迷你号
        setIntent(intent)
        pendingUin = intent.data?.getQueryParameter("uin")
    }

    companion object {
        /** 深链带来的迷你号，由 AppRoot 消费 */
        @Volatile
        var pendingUin: String? = null
    }
}

/**
 * 应用根视图。
 *
 * 三件事：
 * 1. 统一处理返回键与"再按一次退出"提示；
 * 2. 把 ViewModel 的一次性事件转成 Snackbar；
 * 3. 承载导航栈。
 */
@Composable
private fun AppRoot(
    twoPane: Boolean,
    deepLinkUin: String?
) {
    val context = LocalContext.current
    val vm: MainViewModel = viewModel()
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    // 一级事件（提示条）统一在这里消费，页面内不各自处理
    LaunchedEffect(Unit) {
        vm.events.collectLatest { event ->
            when (event) {
                is UiEvent.Toast -> snackbarHostState.showSnackbar(event.message)
                is UiEvent.Info -> snackbarHostState.showSnackbar(event.message)
                // 未同意隐私政策时，引导用户先去查看并同意
                is UiEvent.ConsentRequired -> navController.navigate(Routes.PRIVACY)
            }
        }
    }

    // 深链：从浏览器 http://xxx/profile.html?uin=123456 打开时直接查询
    LaunchedEffect(Unit) {
        val uin = deepLinkUin ?: MainActivity.pendingUin
        if (!uin.isNullOrBlank()) {
            MainActivity.pendingUin = null
            vm.search(uin)
        }
    }

    val activity = context as? MainActivity

    // 金标 / 个保法：涉及个人信息处理前须取得用户明示同意，仅首次安装触发一次
    var consentGiven by remember { mutableStateOf(AppPrefs.isPrivacyAccepted(context)) }

    // 二级页面返回键修复：
    // 实时跟踪当前路由，只有"在首页"时才启用退出确认（enabled=true 会拦截系统返回键）；
    // 在二级页时 BackHandler 的 enabled=false，完全不拦截，返回键原样交给 NavHost 出栈，
    // 避免原实现 "enabled 恒为 true + popBackStack() 返回值判断" 在某些栈状态下吞掉返回事件，
    // 表现为"二级页按返回没反应 / 要多按几次"。
    val backStackEntry by navController.currentBackStackEntryAsState()
    val onHome = backStackEntry?.destination?.route == Routes.HOME

    BackHandler(enabled = onHome) {
        // 只有首页才走"再按一次退出"；二级页 BackHandler 未启用，不会拦截
        activity?.let { handleExit(it) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AppNavHost(
            navController = navController,
            vm = vm,
            twoPane = twoPane
        )

        // 全局提示条：贴底居中，不遮挡顶部内容
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 12.dp)
        )

        // 首次启动：必须先阅读并同意隐私政策才能使用查询功能
        if (!consentGiven) {
            PrivacyConsentDialog(
                onAgree = {
                    AppPrefs.setPrivacyAccepted(context)
                    consentGiven = true
                },
                onViewPolicy = { navController.navigate(Routes.PRIVACY) }
            )
        }
    }
}

/**
 * 退出确认：2 秒内再按一次返回桌面，否则给出提示。
 * 用 moveTaskToBack 而不是 finish，保证回到桌面时任务栈仍在、下次进入更快。
 */
private var lastBackPressedAt = 0L
private const val EXIT_CONFIRM_INTERVAL = 2000L

private fun handleExit(activity: MainActivity) {
    val now = android.os.SystemClock.elapsedRealtime()
    if (now - lastBackPressedAt < EXIT_CONFIRM_INTERVAL) {
        lastBackPressedAt = 0L
        activity.moveTaskToBack(true)
    } else {
        lastBackPressedAt = now
        Toast.makeText(activity, R.string.press_back_again, Toast.LENGTH_SHORT).show()
    }
}
