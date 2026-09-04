package com.marsz.miniquery.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.collection.LruCache
import androidx.compose.runtime.mutableStateOf
import com.marsz.miniquery.data.net.Api
import com.marsz.miniquery.data.net.Http
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File

/**
 * 表情图集管理（对应 HTML 中的 EMOTICON_XML + cropToObjectURLAsync）。
 *
 * 加载优先级：
 *   1. filesDir/emoticon.png —— 联网后从服务端下载的最新图集
 *   2. assets/emoticon.png   —— App 内置的图集（离线可用）
 */
object EmojiAtlas {

    data class Sprite(val x: Int, val y: Int, val w: Int, val h: Int)

    /** 图集切片表（与 emoticon.png 配套） */
    val SPRITES: Map<String, Sprite> = mapOf(
        "xieyanxiao" to Sprite(2, 186, 90, 90),
        "baimu" to Sprite(554, 94, 90, 90),
        "shengqi" to Sprite(2, 554, 90, 90),
        "xianqi" to Sprite(2, 278, 90, 90),
        "daku" to Sprite(370, 94, 90, 90),
        "qinqin" to Sprite(2, 738, 90, 90),
        "xihuan" to Sprite(370, 2, 90, 90),
        "wabikong" to Sprite(646, 2, 90, 90),
        "dianzan" to Sprite(94, 646, 90, 90),
        "haixiu" to Sprite(94, 554, 90, 90),
        "liulei" to Sprite(2, 830, 90, 90),
        "keai" to Sprite(94, 2, 90, 90),
        "shouqibao" to Sprite(2, 462, 90, 90),
        "kun" to Sprite(2, 922, 90, 90),
        "jingya" to Sprite(186, 2, 90, 90),
        "yun" to Sprite(2, 2, 90, 90),
        "shaojiao" to Sprite(2, 646, 90, 90),
        "bye" to Sprite(94, 370, 90, 90),
        "hua_xieyanxiao" to Sprite(462, 2, 90, 90),
        "hua_baimu" to Sprite(554, 94, 90, 90),
        "hua_shengqi" to Sprite(830, 2, 90, 90),
        "hua_xianqi" to Sprite(554, 2, 90, 90),
        "hua_daku" to Sprite(94, 738, 90, 90),
        "hua_qinqin" to Sprite(94, 94, 90, 90),
        "hua_xihuan" to Sprite(370, 2, 90, 90),
        "hua_wabikong" to Sprite(646, 2, 90, 90),
        "hua_dianzan" to Sprite(94, 646, 90, 90),
        "hua_haixiu" to Sprite(94, 554, 90, 90),
        "hua_liulei" to Sprite(94, 186, 90, 90),
        "hua_keai" to Sprite(94, 370, 90, 90),
        "hua_shouqibao" to Sprite(738, 2, 90, 90),
        "hua_kun" to Sprite(94, 278, 90, 90),
        "hua_jingya" to Sprite(94, 462, 90, 90),
        "hua_yun" to Sprite(278, 2, 90, 90),
        "hua_shaojiao" to Sprite(922, 2, 90, 90),
        "hua_bye" to Sprite(830, 2, 90, 90),
    )

    /** 表情码 → 图集切片名 */
    val CODE_ICON: Map<String, String> = mapOf(
        "#A101" to "xieyanxiao", "#A102" to "baimu", "#A103" to "shengqi", "#A104" to "xianqi", "#A105" to "daku",
        "#A106" to "qinqin", "#A107" to "xihuan", "#A108" to "wabikong", "#A109" to "dianzan", "#A110" to "haixiu",
        "#A111" to "liulei", "#A112" to "keai", "#A113" to "shouqibao", "#A114" to "kun", "#A115" to "jingya",
        "#A116" to "yun", "#A117" to "shaojiao", "#A118" to "bye",
        "#A301" to "hua_xieyanxiao", "#A302" to "hua_baimu", "#A303" to "hua_shengqi", "#A304" to "hua_xianqi",
        "#A305" to "hua_daku", "#A306" to "hua_qinqin", "#A307" to "hua_xihuan", "#A308" to "hua_wabikong",
        "#A309" to "hua_dianzan", "#A310" to "hua_haixiu", "#A311" to "hua_liulei", "#A312" to "hua_keai",
        "#A313" to "hua_shouqibao", "#A314" to "hua_kun", "#A315" to "hua_jingya", "#A316" to "hua_yun",
        "#A317" to "hua_shaojiao", "#A318" to "hua_bye"
    )

    private const val ASSET_NAME = "emoticon.png"
    private const val CACHE_FILE = "emoticon.png"
    private const val MAX_CACHED_SPRITES = 64

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val cache = object : LruCache<String, Bitmap>(MAX_CACHED_SPRITES) {}

    @Volatile
    private var atlas: Bitmap? = null

    @Volatile
    private var atlasReady = false

    /** 图集就绪版本号：Compose 读取后可在图集加载完成时自动重组 */
    private val _version = mutableStateOf(0)
    val version: Int get() = _version.value

    /** 在 Application / MainActivity 中调用一次 */
    fun init(context: Context) {
        if (atlasReady) return
        scope.launch {
            atlas = loadAtlas(context)
            atlasReady = true
            _version.value = _version.value + 1
        }
    }

    /** 后台尝试用服务端最新图集覆盖本地缓存（失败无副作用） */
    fun refreshFromServer(context: Context) {
        scope.launch {
            val bytes = Http.download(Api.emoticonAtlas) ?: return@launch
            if (bytes.size < 1024) return@launch
            val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return@launch
            runCatching { File(context.filesDir, CACHE_FILE).writeBytes(bytes) }
            atlas = decoded
            cache.evictAll()
            atlasReady = true
            _version.value = _version.value + 1
        }
    }

    private fun loadAtlas(context: Context): Bitmap? {
        // 1. 远程缓存
        runCatching {
            val f = File(context.filesDir, CACHE_FILE)
            if (f.exists() && f.length() > 1024) {
                BitmapFactory.decodeFile(f.absolutePath)?.let { return it }
            }
        }
        // 2. 内置 assets
        return runCatching {
            context.assets.open(ASSET_NAME).use { BitmapFactory.decodeStream(it) }
        }.getOrNull()
    }

    /** 裁切单个表情；图集未就绪或切片不存在时返回 null（调用方回退为文本） */
    fun crop(name: String): Bitmap? {
        val sprite = SPRITES[name] ?: return null
        val src = atlas ?: return null
        val cached = cache.get(name)
        if (cached != null) return cached
        if (sprite.x + sprite.w > src.width || sprite.y + sprite.h > src.height) return null
        return runCatching {
            val bmp = Bitmap.createBitmap(src, sprite.x, sprite.y, sprite.w, sprite.h)
            cache.put(name, bmp)
            bmp
        }.getOrNull()
    }

    /** 表情码（#A101）对应的 Bitmap */
    fun fromCode(code: String): Bitmap? {
        val name = CODE_ICON[code] ?: return null
        return crop(name)
    }

    /**
     * 释放已裁切的表情位图（图集本身保留，重新裁切成本极低）。
     * 应用进入后台或系统内存紧张时调用，可省下数 MB 常驻内存。
     */
    fun releaseMemory() {
        cache.evictAll()
    }
}
