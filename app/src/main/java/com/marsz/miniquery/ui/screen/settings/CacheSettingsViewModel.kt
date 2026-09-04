package com.marsz.miniquery.ui.screen.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.marsz.miniquery.cache.CacheCategory
import com.marsz.miniquery.cache.ImageCache
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 设置页的缓存统计。
 * 按分类分别统计磁盘占用，清理在 IO 线程执行，不阻塞界面。
 */
class CacheSettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val _sizes = MutableStateFlow<Map<CacheCategory, Long>>(emptyMap())
    val sizes: StateFlow<Map<CacheCategory, Long>> = _sizes.asStateFlow()

    private val _total = MutableStateFlow(0L)
    val total: StateFlow<Long> = _total.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    init {
        refresh()
    }

    /** 重新统计占用（扫描目录，放在 IO 线程） */
    fun refresh() {
        viewModelScope.launch {
            val app = getApplication<Application>()
            val map = CacheCategory.entries.associateWith { ImageCache.sizeOf(app, it) }
            _sizes.value = map
            _total.value = map.values.sum()
        }
    }

    /** 清空单个分类 */
    fun clear(category: CacheCategory) {
        viewModelScope.launch {
            _busy.value = true
            ImageCache.clear(getApplication(), category)
            refresh()
            _busy.value = false
        }
    }

    /** 清空全部分类 */
    fun clearAll() {
        viewModelScope.launch {
            _busy.value = true
            ImageCache.clearAll(getApplication())
            refresh()
            _busy.value = false
        }
    }
}
