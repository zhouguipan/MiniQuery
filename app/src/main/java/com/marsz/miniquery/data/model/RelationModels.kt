package com.marsz.miniquery.data.model

import com.google.gson.annotations.SerializedName

/* ==================== 关系列表：好友 / 关注 / 黑名单 ==================== */
/* 三个接口返回结构相同（仅 act 不同）：
 *   get_friend_list / get_follow_list / get_blacklist
 * 返回示例：
 * {"code":0,"count":7,"queryTime":"0.08 秒",
 *  "list":[{"uin":2225672,"relation":8,"mark":1755768339,"nickname":"...#cff02ffShayKite"}]}
 */

data class RelationListResp(
    @SerializedName("code")       val code: Int = -1,
    @SerializedName("count")      val count: Int = 0,
    @SerializedName("queryTime") val queryTime: String? = null,
    @SerializedName("list")       val list: List<RelationItem>? = null
)

data class RelationItem(
    @SerializedName("uin")       val uin: Long = 0,
    @SerializedName("relation")  val relation: Int = 0,
    @SerializedName("mark")      val mark: Long = 0,
    @SerializedName("nickname")  val nickname: String? = null
) {
    /** uin 可能重复（迷你小号 1/4/100），叠加 mark 做稳定唯一 key */
    val stableId: String get() = "${uin}_${mark}"
}
