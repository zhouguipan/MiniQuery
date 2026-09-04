package com.marsz.miniquery.vm

/** 通用加载状态：一次加载的完整生命周期 */
data class TabState<T>(
    val loading: Boolean = false,
    val loaded: Boolean = false,
    val error: String? = null,
    val data: T? = null
)

/** 家族成员的分页状态 */
data class FamilyMemberState(
    val rows: List<com.marsz.miniquery.data.model.MemberRow> = emptyList(),
    val offset: Int = 0,
    val total: Int = 0,
    val loading: Boolean = false,
    val allLoaded: Boolean = false
)

/** 一次性 UI 事件（提示条） */
sealed interface UiEvent {
    data class Toast(val message: String) : UiEvent
    data class Info(val message: String) : UiEvent
    /** 请求打开隐私政策页（用户尚未同意时由 ViewModel 发出） */
    data object ConsentRequired : UiEvent
}
