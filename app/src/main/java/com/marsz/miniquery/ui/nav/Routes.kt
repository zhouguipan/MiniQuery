package com.marsz.miniquery.ui.nav

/** 全部页面路由。首页即"基础"页，其余功能页都由它进入并可返回。 */
object Routes {
    const val HOME = "home"
    const val FAMILY_LIST = "family_list"
    const val FAMILY_DETAIL = "family_detail/{familyId}"
    const val SKIN = "skin"
    const val GIFT = "gift"
    const val MAP = "map"
    const val ALBUM = "album"
    const val ROOM = "room"
    const val SETTINGS = "settings"
    const val ABOUT = "about"
    const val PRIVACY = "privacy"

    const val ARG_FAMILY_ID = "familyId"

    fun familyDetail(id: Long) = "family_detail/$id"
}
