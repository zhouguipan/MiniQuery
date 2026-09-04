package com.marsz.miniquery.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.marsz.miniquery.ui.screen.album.AlbumScreen
import com.marsz.miniquery.ui.screen.family.FamilyDetailScreen
import com.marsz.miniquery.ui.screen.family.FamilyListScreen
import com.marsz.miniquery.ui.screen.gift.GiftScreen
import com.marsz.miniquery.ui.screen.home.HomeScreen
import com.marsz.miniquery.ui.screen.map.MapScreen
import com.marsz.miniquery.ui.screen.room.RoomScreen
import com.marsz.miniquery.ui.screen.settings.AboutScreen
import com.marsz.miniquery.ui.screen.settings.PrivacyScreen
import com.marsz.miniquery.ui.screen.settings.SettingsScreen
import com.marsz.miniquery.ui.screen.skin.SkinScreen
import com.marsz.miniquery.vm.MainViewModel

/**
 * 全局导航。
 *
 * 首页即"基础"页，其余功能都是二级页面：
 * 点进去、返回键或左上角箭头都能回到上一页，页面切换带统一的滑动动画。
 */
@Composable
fun AppNavHost(
    navController: NavHostController,
    vm: MainViewModel,
    twoPane: Boolean,
    modifier: Modifier = Modifier
) {
    val currentUin by vm.currentUin.collectAsStateWithLifecycle()

    /**
     * 从成员列表点某个人查询时：切换查询目标并回到首页。
     *
     * 关键修正：使用 `navigate(HOME) + popUpTo(HOME, inclusive = false)`，
     * 而不是直接 `popBackStack(HOME, ...)`。
     *
     * 原因：从深层页面（如 首页→家族列表→家族详情）点击成员时，HOME 并不在
     * *当前*返回栈的栈顶链路里，`popBackStack` 找不到该 destination 会**静默失败**，
     * 表现为"点了没反应 / 返回错页"。改用 navigate + popUpTo 主动把栈里所有
     * 中间页一并清空，无论当前在多深的层级都能可靠回到首页，且只产生一次入栈。
     */
    val queryMember: (String) -> Unit = { uin ->
        vm.search(uin)
        navController.navigate(Routes.HOME) {
            // 清空 HOME 之上的所有页面（inclusive=false：保留 HOME 本身）
            popUpTo(Routes.HOME) { inclusive = false }
            // 若栈里已有 HOME，复用同一个实例，避免连续查询时栈底堆积多个首页
            launchSingleTop = true
        }
    }

    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        modifier = modifier,
        enterTransition = { EnterSlide },
        exitTransition = { ExitSlide },
        popEnterTransition = { PopEnterSlide },
        popExitTransition = { PopExitSlide }
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                vm = vm,
                onNavigate = { route ->
                    // 未查询过就点功能入口：留在首页并提示，避免进入空页面
                    if (currentUin == null && route != Routes.SETTINGS) {
                        vm.notifyNeedQuery()
                    } else {
                        navController.navigate(route)
                    }
                }
            )
        }

        composable(Routes.FAMILY_LIST) {
            FamilyListScreen(
                vm = vm,
                twoPane = twoPane,
                onBack = { navController.popBackStack() },
                onOpenDetail = { id -> navController.navigate(Routes.familyDetail(id)) },
                onQueryMember = queryMember
            )
        }

        composable(
            route = Routes.FAMILY_DETAIL,
            arguments = listOf(
                navArgument(Routes.ARG_FAMILY_ID) { type = NavType.LongType }
            )
        ) { entry ->
            val id = entry.arguments?.getLong(Routes.ARG_FAMILY_ID) ?: 0L
            FamilyDetailScreen(
                vm = vm,
                familyId = id,
                onBack = { navController.popBackStack() },
                onQueryMember = queryMember
            )
        }

        composable(Routes.SKIN) {
            SkinScreen(vm = vm, onBack = { navController.popBackStack() })
        }

        composable(Routes.GIFT) {
            GiftScreen(vm = vm, onBack = { navController.popBackStack() })
        }

        composable(Routes.MAP) {
            MapScreen(vm = vm, onBack = { navController.popBackStack() })
        }

        composable(Routes.ALBUM) {
            AlbumScreen(vm = vm, onBack = { navController.popBackStack() })
        }

        composable(Routes.ROOM) {
            RoomScreen(
                vm = vm,
                onBack = { navController.popBackStack() },
                onQueryMember = queryMember
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onOpenAbout = { navController.navigate(Routes.ABOUT) },
                onOpenPrivacy = { navController.navigate(Routes.PRIVACY) }
            )
        }

        composable(Routes.ABOUT) {
            AboutScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.PRIVACY) {
            PrivacyScreen(onBack = { navController.popBackStack() })
        }
    }
}
