package com.marsz.miniquery

import android.app.Application
import android.content.ComponentCallbacks2
import android.content.res.Configuration
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.marsz.miniquery.cache.ImageCache
import com.marsz.miniquery.data.prefs.AppPrefs
import com.marsz.miniquery.util.EmojiAtlas
import com.marsz.miniquery.util.MemoryWatchdog

/**
 * 应用入口。
 *
 * 主要职责是**后台内存治理**：
 * App 退到后台时主动释放图片内存缓存与表情位图，把进程常驻内存压到最低，
 * 既降低被系统回收的概率，也不会拖慢系统；回到前台后按需重建，用户无感知。
 */
class MiniQueryApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // 设置项读入内存，避免 UI 线程反复读磁盘
        AppPrefs.hydrate(this)

        // 请求身份：带上应用版本与设备信息的 User-Agent
        com.marsz.miniquery.data.net.Http.init(this)

        // 表情图集：先用内置资源，再尝试用服务端最新版本覆盖
        EmojiAtlas.init(this)
        EmojiAtlas.refreshFromServer(this)

        watchBackground()

        // 金标"公平运行内存机制"：注册系统级内存回调，后台主动释放
        MemoryWatchdog.install(this)
    }

    /** 监听前后台切换：进入后台立即释放可回收的内存 */
    private fun watchBackground() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) {
                // 整个 App 已经不可见
                ImageCache.trimMemory(ComponentCallbacks2.TRIM_MEMORY_BACKGROUND)
                EmojiAtlas.releaseMemory()
            }
        })
    }

    override fun onTrimMemory(level: Int) {
        ImageCache.trimMemory(level)
        if (level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND) {
            EmojiAtlas.releaseMemory()
        }
        super.onTrimMemory(level)
    }

    override fun onLowMemory() {
        ImageCache.trimMemory(ComponentCallbacks2.TRIM_MEMORY_BACKGROUND)
        EmojiAtlas.releaseMemory()
        super.onLowMemory()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // 系统字体缩放等变化不需要额外处理，Compose 会随配置重建
    }
}
