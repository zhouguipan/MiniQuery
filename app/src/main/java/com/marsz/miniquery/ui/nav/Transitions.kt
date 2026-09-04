package com.marsz.miniquery.ui.nav

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally

/**
 * 页面切换动画。
 *
 * 统一时长与曲线，避免不同页面节奏不一致造成的"顿挫感"。
 * 新页面从右侧滑入并轻微放大，旧页面向左退出并淡出，
 * 返回时反向播放，符合 Android 用户的方向直觉。
 */
private const val DURATION = 260

private val easing = FastOutSlowInEasing

val EnterSlide: EnterTransition =
    slideInHorizontally(
        animationSpec = tween(DURATION, easing = easing),
        initialOffsetX = { fullWidth -> (fullWidth * 0.22f).toInt() }
    ) + fadeIn(animationSpec = tween(DURATION, easing = easing)) +
            scaleIn(
                animationSpec = tween(DURATION, easing = easing),
                initialScale = 0.985f
            )

val ExitSlide: ExitTransition =
    slideOutHorizontally(
        animationSpec = tween(DURATION, easing = easing),
        targetOffsetX = { fullWidth -> (-fullWidth * 0.10f).toInt() }
    ) + fadeOut(animationSpec = tween(DURATION, easing = easing))

val PopEnterSlide: EnterTransition =
    slideInHorizontally(
        animationSpec = tween(DURATION, easing = easing),
        initialOffsetX = { fullWidth -> (-fullWidth * 0.10f).toInt() }
    ) + fadeIn(animationSpec = tween(DURATION, easing = easing))

val PopExitSlide: ExitTransition =
    slideOutHorizontally(
        animationSpec = tween(DURATION, easing = easing),
        targetOffsetX = { fullWidth -> (fullWidth * 0.22f).toInt() }
    ) + fadeOut(animationSpec = tween(DURATION, easing = easing)) +
            scaleOut(
                animationSpec = tween(DURATION, easing = easing),
                targetScale = 0.99f
            )
