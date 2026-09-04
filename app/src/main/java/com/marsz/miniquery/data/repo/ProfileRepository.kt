package com.marsz.miniquery.data.repo

import com.marsz.miniquery.data.model.AlbumResp
import com.marsz.miniquery.data.model.DevResp
import com.marsz.miniquery.data.model.FamilyDetail
import com.marsz.miniquery.data.model.FamilyIdResp
import com.marsz.miniquery.data.model.GiftResp
import com.marsz.miniquery.data.model.MapResp
import com.marsz.miniquery.data.model.NowProfile
import com.marsz.miniquery.data.model.ProfileItem
import com.marsz.miniquery.data.model.RoomResp
import com.marsz.miniquery.data.model.SkinResp
import com.marsz.miniquery.data.model.TrackingResp
import com.marsz.miniquery.data.net.Api
import com.marsz.miniquery.data.net.ApiException
import com.marsz.miniquery.data.net.Http
import com.marsz.miniquery.data.net.ParseException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

object ProfileRepository {

    /* ==================== 基础资料 ==================== */

    suspend fun nowProfile(uin: String): NowProfile {
        val data = Http.get<NowProfile>(Api.nowProfile(uin))
        if (data.code != 0) throw ApiException(data.code, errorText(data.code, data.msg))
        return data
    }

    /**
     * code 规则：0 成功；2 / 3 / 23 使用固定文案；其余取后端 msg。
     */
    fun errorText(code: Int, msg: String?): String = when (code) {
        2 -> "查询失败，该迷你服务器正在维护中！"
        3 -> "查询失败，该迷你号注销了！"
        23 -> "请求过于频繁"
        else -> msg?.takeIf { it.isNotBlank() } ?: "查询失败（code: $code）"
    }

    /* ==================== 状态 / 房间 / 队伍 ==================== */

    /** tracking 失败不影响主流程，返回 null */
    suspend fun tracking(uin: String): TrackingResp? =
        runCatching { Http.get<TrackingResp>(Api.tracking(uin)) }
            .getOrNull()
            ?.takeIf { it.result == 0 && it.data != null }

    /** 房间详情，失败返回 null */
    suspend fun room(uin: String): RoomResp? =
        runCatching { Http.get<RoomResp>(Api.room(uin)) }.getOrNull()

    /** 创作者信息 */
    suspend fun developer(uin: String): DevResp? =
        runCatching { Http.get<DevResp>(Api.developerStatus(uin)) }.getOrNull()

    /* ==================== 家族 ==================== */

    suspend fun familyIds(uin: String): List<Long> {
        val resp = Http.get<FamilyIdResp>(Api.familyIdList(uin))
        if (resp.code != 0) throw ApiException(resp.code, errorText(resp.code, null))
        return resp.list ?: emptyList()
    }

    suspend fun familyDetail(id: Long): FamilyDetail? =
        runCatching { Http.get<FamilyDetail>(Api.familyDetail(id)) }
            .getOrNull()
            ?.takeIf { it.code == 0 }

    /**
     * 并发拉取多个家族详情。
     * 用信号量把并发限制在 4，既比串行快数倍，又不会把连接池打满。
     */
    suspend fun familyDetails(ids: List<Long>): List<FamilyDetail> = coroutineScope {
        val sem = Semaphore(4)
        ids.map { id ->
            async {
                sem.withPermit { runCatching { familyDetail(id) }.getOrNull() }
            }
        }.awaitAll().filterNotNull()
    }

    /* ==================== 批量资料 ==================== */

    /**
     * 批量查询资料，返回 uin -> ProfileItem 映射。
     * 该接口直接返回数组，解析失败时返回空 map（调用方回退占位显示）。
     */
    suspend fun profileList(uins: List<String>): Map<String, ProfileItem> {
        if (uins.isEmpty()) return emptyMap()
        return runCatching {
            val list = Http.get<List<ProfileItem>>(Api.profileList(uins.joinToString(",")))
            list.associateBy { it.uin.toString() }
        }.getOrDefault(emptyMap())
    }

    /* ==================== 皮肤 / 礼物 / 相册 / 地图 ==================== */

    suspend fun skins(uin: String): SkinResp = Http.get<SkinResp>(Api.skinList(uin))

    suspend fun gifts(uin: String): GiftResp {
        val resp = Http.get<GiftResp>(Api.giftList(uin))
        if (resp.code != 0) throw ApiException(resp.code, errorText(resp.code, null))
        return resp
    }

    suspend fun album(uin: String): AlbumResp {
        val resp = Http.get<AlbumResp>(Api.album(uin))
        if (resp.code != 0) throw ApiException(resp.code, "暂无相册图片")
        // 后端可能返回带转义斜杠的 URL：https:\/\/xxx → https://xxx
        return resp.copy(thumb_urls = resp.thumb_urls?.map { it.replace("\\/", "/") })
    }

    suspend fun maps(uin: String): MapResp {
        val resp = Http.get<MapResp>(Api.mapList(uin))
        if (resp.code != 0) throw ApiException(resp.code, errorText(resp.code, null))
        return resp
    }

    /** 统一的请求失败文案 */
    fun networkError(t: Throwable): String = when (t) {
        is ApiException -> t.message ?: "查询失败"
        is ParseException -> t.message ?: "服务器返回了异常数据"
        else -> "网络请求失败"
    }
}
