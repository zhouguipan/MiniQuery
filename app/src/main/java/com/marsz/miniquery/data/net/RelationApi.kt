package com.marsz.miniquery.data.net

/**
 * 关系列表接口地址：好友 / 关注 / 黑名单。
 * 与 [Api] 同级，BASE 复用同一份（改 [Api.BASE] 一行即整体切换）。
 */
object RelationApi {

    private const val API = "${Api.BASE}/mnw1/api"

    fun friendList(uin: String) = "$API?act=get_friend_list&uin=$uin"
    fun followList(uin: String) = "$API?act=get_follow_list&uin=$uin"
    fun blacklist(uin: String)  = "$API?act=get_blacklist&uin=$uin"
}
