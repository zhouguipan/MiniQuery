package com.marsz.miniquery.ui.screen.relation

import androidx.compose.runtime.Composable
import com.marsz.miniquery.vm.MainViewModel

/**
 * 好友 / 关注 / 黑名单 三个入口 Screen。
 *
 * 均复用 [RelationListScreen]，仅 title、state、load 不同；
 * friendList / followList / blacklist 与 loadFriendList / loadFollowList / loadBlacklist
 * 均在 [MainViewModel] 中定义（已完整合并）。
 *
 * 参数命名 `onQueryMember` 与 AppNavHost 的调用保持一致。
 */
@Composable
fun FriendListScreen(
    vm: MainViewModel,
    onBack: () -> Unit,
    onQueryMember: (String) -> Unit,
) = RelationListScreen(
    title = "好友",
    state = vm.friendList,
    load = vm::loadFriendList,
    vm = vm,
    onQueryMember = onQueryMember,
    onBack = onBack,
)

@Composable
fun FollowListScreen(
    vm: MainViewModel,
    onBack: () -> Unit,
    onQueryMember: (String) -> Unit,
) = RelationListScreen(
    title = "关注",
    state = vm.followList,
    load = vm::loadFollowList,
    vm = vm,
    onQueryMember = onQueryMember,
    onBack = onBack,
)

@Composable
fun BlacklistScreen(
    vm: MainViewModel,
    onBack: () -> Unit,
    onQueryMember: (String) -> Unit,
) = RelationListScreen(
    title = "黑名单",
    state = vm.blacklist,
    load = vm::loadBlacklist,
    vm = vm,
    onQueryMember = onQueryMember,
    onBack = onBack,
)
