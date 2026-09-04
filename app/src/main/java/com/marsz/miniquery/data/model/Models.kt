package com.marsz.miniquery.data.model

/**
 * 全部数据模型对应后端接口字段。
 *
 * 后端同一字段在不同接口中可能返回 number 或 string，
 * 因此所有"仅用于展示"的字段统一声明为 String?，
 * 由 [com.marsz.miniquery.data.net.LenientStringAdapter] 做宽容解析。
 */

/* ==================== 基础资料 ==================== */
// GET /mnw1/api?act=get_now_profile
data class NowProfile(
    val code: Int = -1,
    val msg: String? = null,
    val uin: Long = 0,
    val nickname: String? = null,
    val online: String? = null,             // "在线" / "离线" —— 在线状态的权威来源
    val moodText: String? = null,
    val level: String? = null,
    val fans: String? = null,
    val following: String? = null,
    val popularity: String? = null,
    val creditScore: String? = null,
    val regist_account_time: String? = null,
    val last_login_time: String? = null,
    val IP: String? = null,
    val charmValue: String? = null,
    val thumbs_up: String? = null,
    val avatar_url: String? = null,
    val headframe_url: String? = null,
    val headframeID: String? = null,
    val developerLevel: String? = null,
    val opus: String? = null,
    val all_download_count: String? = null,
    val nowPlays: String? = null,
    val like: String? = null,
    val total: String? = null,
    val family: FamilyBrief? = null
)

data class FamilyBrief(
    val code: Int = -1,
    val id: Long = 0,
    val name: String? = null,
    val desc: String? = null,
    val member_count: String? = null,
    val active_val: String? = null,
    val day_active: String? = null,
    val last_day_active_val: String? = null,
    val header_flagm_url: String? = null,
    val header_url: String? = null,
    val header_type: String? = null          // "0" / "1"
)

/* ==================== 状态 / 房间 / 队伍 ==================== */
// GET /mnw1/tracking
data class TrackingResp(
    val result: Int = -1,
    val data: TrackingData? = null
)

data class TrackingData(
    val status: TrackingStatus? = null,
    val client_info: ClientInfo? = null
)

data class TrackingStatus(
    val code: String? = null,                // in_room / in_team / ...
    val text: String? = null,
    val room_details: TrackingRoom? = null,
    val team_details: TrackingTeam? = null
)

data class ClientInfo(
    val version: String? = null,
    val apiid: String? = null
)

data class TrackingRoom(
    val room_id: String? = null,
    val room_name: String? = null,
    val current_players: String? = null,
    val max_players: String? = null,
    val is_host: Boolean = false,
    val has_password: Boolean = false,
    val allow_search: Boolean = false,
    val allow_trace: Boolean = false,
    val visible_range: String? = null,
    val join_when_playing: Boolean = false,
    val map_wid: String? = null,
    val room_label: String? = null,
    val map_name: String? = null
)

data class TrackingTeam(
    val team_id: String? = null,
    val team_name: String? = null,
    val max_members: String? = null,
    val leader_uin: Long? = null,
    val is_public: Boolean = false,
    val players: List<TeamPlayer>? = null
)

data class TeamPlayer(
    val uin: Long = 0,
    val name: String? = null,
    val time: Long? = null
)

/* ==================== 房间（get_room.php） ==================== */
data class RoomResp(
    val success: Boolean = false,
    val data: RoomData? = null
)

data class RoomData(
    val room_status: String? = null,         // "正在房间中游玩" 表示在房间内
    val room_info: RoomInfo? = null
)

data class RoomInfo(
    val room_name: String? = null,
    val mode: String? = null,
    val map_name: String? = null,
    val map_id: String? = null,
    val player_count: String? = null,
    val lock_status: String? = null,
    val has_password: Boolean = false,
    val visibility: String? = null,
    val version: String? = null,
    val apiid: String? = null,
    val create_time: String? = null,
    val start_time: String? = null,
    val thumbnail: String? = null,
    val members: List<RoomMember>? = null,
    val owner: RoomOwner? = null
)

data class RoomMember(
    val uin: Long = 0,
    val name: String? = null,
    val is_owner: Boolean = false
)

data class RoomOwner(
    val uin: Long = 0,
    val name: String? = null
)

/* ==================== 创作者 ==================== */
data class DevResp(
    val code: Int = -1,
    val data: DevData? = null
)

data class DevData(
    val opus: String? = null,
    val all_download_count: String? = null,
    val nowPlays: String? = null,
    val like: String? = null,
    val total: String? = null
)

/* ==================== 家族 ==================== */
data class FamilyIdResp(
    val code: Int = -1,
    val list: List<Long>? = null
)

data class FamilyDetail(
    val code: Int = -1,
    val id: Long = 0,
    val name: String? = null,
    val desc: String? = null,
    val member_count: String? = null,
    val active_val: String? = null,
    val day_active: String? = null,
    val last_day_active_val: String? = null,
    val last_week_active_val: String? = null,
    val level: String? = null,
    val leader_uin: Long? = null,
    val creator_uin: Long? = null,
    val header_flagm_url: String? = null,
    val header_url: String? = null,
    val header_type: String? = null,
    val member_list: List<FamilyMember>? = null
)

data class FamilyMember(
    val uin: Long = 0,
    val time: Long? = null
)

/* ==================== 批量资料 ==================== */
data class ProfileItem(
    val uin: Long = 0,
    val nickname: String? = null,
    val avatar_url: String? = null,
    val head_frame_id: String? = null
)

/* ==================== 皮肤 / 礼物 / 相册 / 地图 ==================== */
data class SkinResp(
    val code: Int = -1,
    val skin_list: List<SkinItem>? = null
)

data class SkinItem(
    val id: String? = null,
    val name: String? = null,
    val head: String? = null              // 角色图标编号
)

data class GiftResp(
    val code: Int = -1,
    val gift_list: List<GiftItem>? = null
)

data class GiftItem(
    val id: String? = null,
    val name: String? = null,
    val num: String? = null
)

data class AlbumResp(
    val code: Int = -1,
    val thumb_urls: List<String>? = null
)

data class MapResp(
    val code: Int = -1,
    val map_info_list: MapInfoList? = null
)

data class MapInfoList(
    val map_count: String? = null,
    val map_info_list: List<MapItem>? = null
)

data class MapItem(
    val name: String? = null,
    val memo: String? = null,
    val cover_url: String? = null
)

/* ==================== UI 聚合模型 ==================== */

/** 成员行的展示模型（家族 / 队伍 / 房间成员统一结构） */
data class MemberRow(
    val uin: String,
    val name: String,
    val avatarUrl: String,
    val headframeUrl: String,
    val roleText: String = "成员",
    val role: MemberRole = MemberRole.NORMAL,
    val joinTimeText: String = "-",
    val sortWeight: Int = 0
)

/** LEADER=族长 / 队长，CREATOR=创始人 / 房主，SELF=被查询的本人 */
enum class MemberRole { LEADER, CREATOR, SELF, NORMAL }

/** 相册大图预览使用 */
data class AlbumPhoto(val url: String)
