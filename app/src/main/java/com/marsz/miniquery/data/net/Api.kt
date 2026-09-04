package com.marsz.miniquery.data.net

/**
 * 所有后端接口与静态资源的地址定义。
 * 改 [BASE] 一行即可整体切换服务地址。
 */
object Api {

    const val BASE = "http://81.71.23.66:18088"

    private const val API = "$BASE/mnw1/api"

    /* ==================== 接口 ==================== */

    fun nowProfile(uin: String) = "$API?act=get_now_profile&uin=$uin"

    fun tracking(uin: String) = "$BASE/mnw1/tracking?uin=$uin"

    fun room(uin: String) = "$BASE/mini/get_room.php?test&target=$uin"

    fun developerStatus(uin: String) = "$API?act=get_developer_status&uin=$uin"

    fun familyIdList(uin: String) = "$API?act=query_uin_family_id_list&uin=$uin"

    fun familyDetail(id: Long) = "$API?act=get_family_detail&id=$id"

    /** 批量资料查询（多个 uin 用英文逗号连接，建议 <= 80 个） */
    fun profileList(uins: String) = "$API?act=get_Profile_list&uin=$uins"

    fun skinList(uin: String) = "$API?act=get_profile_skin_list&uin=$uin"

    fun giftList(uin: String) = "$API?act=get_profile_gift_list&uin=$uin"

    fun album(uin: String) = "$API?act=get_camera_photo_urls&uin=$uin"

    fun mapList(uin: String) = "$API?act=get_profile_maps&uin=$uin"

    /* ==================== 静态资源 ==================== */

    /** 角色头像 */
    fun avatar(head: String?) = "$BASE/mini/res/roleicons/${head ?: "1"}.png"

    /** 默认头像 */
    val defaultAvatar = "$BASE/mini/res/roleicons/1.png"

    /**
     * 头像框。后端返回的 headframeID 可能对应 .png 或 .gif，
     * 这里保持原样拼接（无扩展名），由图片加载器按实际内容解码。
     */
    fun headframe(id: String?) = "$BASE/mnw1/headframes/${id ?: "1"}"

    fun familyFlag(id: String?) = "$BASE/mini/res/family/flagm/${id ?: "1"}.png"

    fun familyIcon(id: String?) = "$BASE/mini/res/family/icon/${id ?: "1"}.png"

    fun giftIcon(id: String?) = "$BASE/mini/res/gift/${id ?: "1"}.png"

    val unknownMapCover = "$BASE/mini/res/map/unknown.png"

    /** 表情图集（远程，可覆盖内置的 assets/emoticon.png） */
    val emoticonAtlas = "$BASE/emoticon.png"
}
