package com.marsz.miniquery.ui.screen.family

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.marsz.miniquery.cache.CacheCategory
import com.marsz.miniquery.data.model.FamilyDetail
import com.marsz.miniquery.ui.component.CachedImage

/**
 * 家族标识：旗帜底图 + 家族头像叠加。
 * 叠加比例由后端 header_type 决定，与网页端保持一致。
 */
@Composable
fun FamilyAvatar(
    flagUrl: String?,
    headerUrl: String?,
    headerType: String?,
    size: Dp = 64.dp,
    modifier: Modifier = Modifier
) {
    val ratio = if (headerType == "1") 0.35f else 0.56f
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        CachedImage(
            url = flagUrl,
            category = CacheCategory.FAMILY,
            contentDescription = "家族旗帜",
            contentScale = ContentScale.Crop,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxSize()
        )
        CachedImage(
            url = headerUrl,
            category = CacheCategory.FAMILY,
            contentDescription = "家族头像",
            contentScale = ContentScale.Crop,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.size(size * ratio)
        )
    }
}

/** 家族统计项：上行数值，下行标签 */
@Composable
fun FamilyStat(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

/** 家族详细信息头部：标识 + 名称 / ID / 简介 + 五列统计 */
@Composable
fun FamilyDetailHeader(
    family: FamilyDetail,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            FamilyAvatar(
                flagUrl = family.header_flagm_url,
                headerUrl = family.header_url,
                headerType = family.header_type,
                size = 64.dp
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = family.name?.takeIf { it.isNotBlank() } ?: "未命名家族",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "ID ${family.id} · Lv.${family.level ?: "0"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        family.desc?.takeIf { it.isNotBlank() }?.let { desc ->
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            FamilyStat("成员", family.member_count ?: "-", Modifier.weight(1f))
            FamilyStat("活跃度", family.active_val ?: "-", Modifier.weight(1f))
            FamilyStat("日活跃", family.day_active ?: "-", Modifier.weight(1f))
            FamilyStat("昨活跃", family.last_day_active_val ?: "-", Modifier.weight(1f))
            FamilyStat("周活跃", family.last_week_active_val ?: "-", Modifier.weight(1f))
        }
    }
}
