package com.marsz.miniquery.ui.theme

import androidx.compose.ui.unit.dp

/**
 * 统一的间距 / 圆角尺度。
 * 所有页面共用一套，保证不同屏幕（手机 / 平板）上视觉节奏一致。
 */
object Dimens {
    /** 页面左右外边距 */
    val ScreenPadding = 14.dp
    val ScreenPaddingLarge = 24.dp

    /** 卡片之间的间隔 */
    val CardGap = 10.dp

    /** 卡片内边距 */
    val CardPadding = 14.dp

    val RadiusSmall = 10.dp
    val RadiusMedium = 14.dp
    val RadiusLarge = 18.dp
    val RadiusXLarge = 24.dp

    /** 头像尺寸 */
    val AvatarSmall = 40.dp
    val AvatarMedium = 56.dp
    val AvatarLarge = 76.dp

    /** 网格最小列宽：屏幕越宽自动排越多列，平板自然形成多列布局 */
    val GridMinColumn = 96.dp
    val GridMinColumnLarge = 168.dp
    val EntryMinColumn = 150.dp
}
