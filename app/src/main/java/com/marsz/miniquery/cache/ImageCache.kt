package com.marsz.miniquery.cache

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * 图片缓存分类。
 *
 * 每一类都拥有**独立的** ImageLoader（各自的内存缓存 + 磁盘缓存目录），
 * 因此可以在设置里按分类查看占用、按分类清理，互不干扰。
 */
enum class CacheCategory(
    /** 磁盘缓存子目录名 */
    val dirName: String,
    /** 设置页展示名 */
    val label: String,
    /** 磁盘缓存上限（MB） */
    val diskMb: Long,
    /** 内存缓存上限（MB），低端设备会自动减半 */
    val memMb: Int
) {
    AVATAR("avatar", "头像", 40, 8),
    HEADFRAME("headframe", "头像框", 30, 6),
    SKIN("skin", "皮肤图片", 60, 10),
    GIFT("gift", "礼物图标", 20, 5),
    MAP("map", "地图封面", 50, 8),
    ALBUM("album", "相册图片", 100, 6),
    FAMILY("family", "家族旗帜", 20, 4),
}

object ImageCache {

    private val loaders = ConcurrentHashMap<CacheCategory, ImageLoader>()
    // 0 表示尚未探测；探测完成后必为正数。
    // 注意：初值不能写 1.0，否则下面的判断永远不成立，低端设备减半逻辑会失效。
    private var memScale = 0.0

    /** 依据设备内存等级缩放内存缓存：低端机减半，避免 OOM */
    private fun resolveScale(context: Context): Double {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memClass = am?.memoryClass ?: 192
        return if (memClass <= 128) 0.5 else 1.0
    }

    /** 获取指定分类的 ImageLoader（懒创建，全局复用） */
    fun loader(context: Context, category: CacheCategory): ImageLoader {
        val app = context.applicationContext
        return loaders.getOrPut(category) {
            if (memScale == 0.0) memScale = resolveScale(app)
            val memBytes = (category.memMb * 1024 * 1024 * memScale).toInt()
            ImageLoader.Builder(app)
                .components {
                    // 头像框可能是 gif，必须挂上动画解码器；API 28+ 用系统解码器，效率更高
                    if (Build.VERSION.SDK_INT >= 28) {
                        add(ImageDecoderDecoder.Factory())
                    } else {
                        add(GifDecoder.Factory())
                    }
                }
                .memoryCache {
                    MemoryCache.Builder(app)
                        .maxSizeBytes(memBytes)
                        .build()
                }
                .diskCache {
                    DiskCache.Builder()
                        .directory(File(app.cacheDir, "image_cache/${category.dirName}"))
                        .maxSizeBytes(category.diskMb * 1024 * 1024)
                        .build()
                }
                // 后端未给缓存头，这里强制复用磁盘缓存，二次打开秒出
                .respectCacheHeaders(false)
                .build()
        }
    }

    /** 某分类的磁盘缓存目录 */
    fun dir(context: Context, category: CacheCategory): File =
        File(context.applicationContext.cacheDir, "image_cache/${category.dirName}")

    /** 统计某分类的磁盘占用（字节）。耗时操作，请在 IO 线程调用。 */
    suspend fun sizeOf(context: Context, category: CacheCategory): Long =
        withContext(Dispatchers.IO) { dirSize(dir(context, category)) }

    /** 统计全部分类的磁盘占用 */
    suspend fun totalSize(context: Context): Long = withContext(Dispatchers.IO) {
        CacheCategory.entries.sumOf { dirSize(dir(context, it)) }
    }

    /** 清空某分类（磁盘 + 内存） */
    suspend fun clear(context: Context, category: CacheCategory) = withContext(Dispatchers.IO) {
        loaders[category]?.let {
            it.memoryCache?.clear()
            it.diskCache?.clear()
        }
        dir(context, category).deleteRecursively()
    }

    /** 清空全部分类 */
    suspend fun clearAll(context: Context) = withContext(Dispatchers.IO) {
        CacheCategory.entries.forEach { cat ->
            loaders[cat]?.let {
                it.memoryCache?.clear()
                it.diskCache?.clear()
            }
            dir(context, cat).deleteRecursively()
        }
        File(context.applicationContext.cacheDir, "image_cache").deleteRecursively()
    }

    /**
     * 应用进入后台 / 系统内存紧张时释放内存缓存。
     * 只清内存、保留磁盘，回到前台仍能秒加载，同时把后台驻留内存压到最低。
     */
    fun trimMemory(level: Int) {
        // 达到这几个等级时界面已经不可见或系统内存告急，
        // 直接全量释放收益最大；磁盘缓存保留，回到前台依然秒加载。
        val shouldClear = level >= android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL
        if (!shouldClear) return

        loaders.values.forEach { loader ->
            loader.memoryCache?.clear()
        }
    }

    private fun dirSize(file: File): Long {
        if (!file.exists()) return 0L
        return try {
            file.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        } catch (_: Exception) {
            0L
        }
    }
}
