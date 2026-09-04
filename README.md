# 迷你查询 MiniQuery

迷你世界在线查主页 —— 基于 `MiniProfile_Android` 完全重写的 Android 原生工程（Kotlin + Jetpack Compose）。

包名 `com.marsz.miniquery`，应用名 **迷你查询**。

---

## 一、这一版改了什么

| 项目 | 原版 | 本版 |
|---|---|---|
| 顶部区域 | 大标题 + 大输入框，占用整屏上半部分 | 单行紧凑搜索栏，纵向空间全部让给内容 |
| 页面结构 | 顶部 7 个 Tab 横滑切换 | 首页 + 6 个独立二级页，点进点出，带滑动动画 |
| 返回 | 依赖系统返回 | 左上角箭头 + 系统返回键均可返回；首页再按一次返回桌面（带提示） |
| 头像框 | 按 png 处理，gif 无法显示 | 按实际内容解码，png / gif 都能显示，gif 自动播放 |
| 家族 | 进页面就把所有家族成员一起拉下来 | 列表只加载家族本身；进入详情后滚动到底自动续加载 |
| 皮肤 | 默认只显示 12 个，需点"展开" | 默认全部展开，网格惰性加载 |
| 礼物 | 列表行，图标小 | 卡片网格，图标居中、数量高亮 |
| 地图 | 窄行列表 | 16:9 封面卡片网格，平板自动多列 |
| 图片缓存 | 无分类，退出即丢 | 7 个分类各自独立缓存，可分别查看占用与清理 |
| 后台内存 | 无处理 | 进入后台自动释放图片内存，回到前台按需重建 |
| 平板 / 折叠屏 | 拉伸显示 | 两栏布局 + 网格自适应列数 |
| 请求身份 | 无 | 每个请求带含版本与设备信息的 User-Agent |

---

## 二、环境要求

| 项 | 版本 |
|---|---|
| Android Studio | Ladybug (2024.2) 及以上 |
| JDK | 17 |
| Gradle | 8.9（Wrapper 已自带 jar，首次打开自动下载） |
| Kotlin | 1.9.25 |
| AGP | 8.7.3 |
| compileSdk / targetSdk | 35（Android 15） |
| minSdk | 24（Android 7.0），覆盖到最新 Android |

---

## 三、如何编译

```bash
# 方式一：命令行
./gradlew assembleRelease     # 产物 app/build/outputs/apk/release/app-release.apk
./gradlew installDebug        # 直接装到连接的设备

# 方式二：Android Studio
File → New → Import Project → 选中本目录 → 等待 Sync → Run ▶
```

**关于签名**：`app/build.gradle.kts` 里给 release 配了 `auto` 签名配置，直接复用系统自动生成的 debug 签名，因此**不配任何 keystore 也能打包出可安装的 APK**。要上架应用市场时，把 `signingConfigs.auto` 换成自己的签名信息即可。

---

## 四、目录结构

```
app/src/main/java/com/marsz/miniquery/
├── MainActivity.kt              # 入口：Splash、edge-to-edge、返回键与退出确认、全局 Snackbar
├── MiniQueryApp.kt              # Application：后台内存回收、User-Agent 初始化、表情图集
├── cache/ImageCache.kt          # 分类图片缓存（7 类，各自独立内存/磁盘）
├── data/
│   ├── model/Models.kt          # 全部数据模型
│   ├── net/Api.kt               # 接口与静态资源地址（改 BASE 一行即可换服务）
│   ├── net/Http.kt              # OkHttp + Gson（宽容解析）
│   ├── net/UserAgent.kt         # 带设备信息的请求身份
│   ├── prefs/AppPrefs.kt        # 设置持久化 + 查询历史
│   └── repo/ProfileRepository.kt
├── ui/
│   ├── theme/                   # Material3 主题（明/暗/动态配色）
│   ├── nav/                     # 路由 + 页面切换动画
│   ├── component/               # 通用组件（卡片、网格、头像框、图片预览…）
│   └── screen/                  # 首页 / 家族 / 皮肤 / 礼物 / 地图 / 相册 / 房间 / 设置
├── util/                        # 数字与容量格式化、表情图集、moodText 解析
└── vm/MainViewModel.kt          # 全部状态与业务逻辑
```

---

## 五、功能说明

### 5.1 缓存分类

设置 → 缓存管理，可查看每一类的磁盘占用并单独清理：

| 分类 | 内容 | 磁盘上限 |
|---|---|---|
| 头像 | 角色头像 | 40 MB |
| 头像框 | 头像框（含 gif） | 30 MB |
| 皮肤图片 | 皮肤头像 | 60 MB |
| 礼物图标 | 礼物图标 | 20 MB |
| 地图封面 | 地图缩略图 | 50 MB |
| 相册图片 | 相册缩略图 | 100 MB |
| 家族旗帜 | 家族旗帜与头像 | 20 MB |

每类都是**独立的 ImageLoader**（独立内存缓存 + 独立磁盘目录），清理互不影响。
低端设备（内存 ≤ 128 MB 等级）内存缓存自动减半，避免 OOM。

### 5.2 后台内存优化

- 整个 App 进入后台 → 立即清空图片内存缓存 + 释放已裁切的表情位图；
- 系统内存紧张（onTrimMemory / onLowMemory）→ 按等级分级释放；
- 磁盘缓存保留，回到前台仍然秒开。

### 5.3 家族成员自动加载

- 列表页只请求 `query_uin_family_id_list` + `get_family_detail`，**不预取任何成员资料**；
- 进入详情后先加载 25 个，滚动距列表尾部还剩 10 个时自动预取下一页；
- 成员资料按每批 80 个并发批量查询，带 LRU 缓存（上限 2000 条），滑动不会重复请求。

### 5.4 页面切换动画

统一 260ms `FastOutSlowIn`：新页从右侧滑入 + 轻微放大 + 淡入，返回时反向播放。

### 5.5 大屏适配

- 手机（Compact）：单页导航；
- 平板 / 折叠屏展开（Medium / Expanded）：家族页自动变左右两栏，网格按屏宽自动增列；
- 已在 Manifest 声明 `resizeableActivity` 与 `supports-screens`，支持自由窗口与分屏。

### 5.6 返回逻辑

- 二级页面：左上角箭头、系统返回键、手势返回都能回上一页；
- 首页（栈底）：首次按返回提示"再按一次返回桌面"，2 秒内再按则回到桌面（用 `moveTaskToBack`，任务栈保留，下次进入更快）；
- 采用 `BackHandler` 实现，Android 13+ 的**预测式返回手势**同样生效。

---

## 六、兼容性相关

- **targetSdk 35**，适配 Android 15 的 edge-to-edge 强制行为；
- **edge-to-edge**：内容绘制到系统栏之下，状态栏/导航栏图标颜色随主题反转，任何 ROM 上都不会出现黑条或看不清的图标；
- **预测式返回**：Manifest 已开启 `enableOnBackInvokedCallback`；
- **64 位**：依赖库均为 64 位，满足应用市场要求；
- **自适应图标**：提供 `adaptive-icon`（含 `monochrome` 图层），支持各家桌面的主题图标；
- **明文流量**：后端接口与图片资源均为 `http://81.71.23.66:18088`，
  已通过 `usesCleartextTraffic="true"` + `network_security_config.xml` 放行，否则 Android 9+ 全部请求会失败。
  若后续后端升级为 HTTPS，把这两处改回禁止明文即可；
- **动态配色**：Android 12+ 支持 Material You 取色，可在设置中关闭。

> 注：HarmonyOS NEXT（纯鸿蒙）不再支持安装 Android APK，如需覆盖该系统需要单独做 ArkTS 版本；
> HarmonyOS 4.x 及更早版本可直接安装运行。

---

## 七、User-Agent 说明

每个请求都带上形如下面格式的头：

```
MiniQuery/2.0.0 (Android 14; SDK 34; 23046RP50C/cmi; arm64-v8a; zh-CN)
```

包含应用名与版本、Android 版本与 SDK 等级、机型与设备代号、CPU 架构、系统语言，
服务端可据此区分来源、定位机型问题、按版本灰度。
实现见 `data/net/UserAgent.kt`，启动时构建一次并缓存，不产生重复开销。

---

## 八、注意事项

1. **表情图集 `assets/emoticon.png`**：内置的是与线上坐标一致的图集，App 启动后会
   自动尝试下载服务端最新版本覆盖，联网即可看到真实表情。
2. **后端地址**：统一在 `data/net/Api.kt` 的 `BASE` 常量中，改一行即可整体切换。
3. **错误码** `2 / 3 / 23` 沿用固定文案，其余取后端 `msg`，与原网页版保持一致。

---

## 九、隐私与安全（已内置）

- **隐私政策**：`assets/privacy_policy.html`，设置 → 隐私与安全 → 隐私政策 可离线查看；
- **首次启动同意弹窗**：未同意前不发起任何联网查询，同意状态存本地，只触发一次；
- **安全环境检测**：设置页展示 Root / 代理 / 模拟器 / 调试状态，仅本地检测、不上传；
- **权限最小化**：仅 `INTERNET` 与 `ACCESS_NETWORK_STATE` 两个普通权限，无危险权限；
- **User-Agent**：含应用版本、系统版本、机型、CPU 架构、语言，不含 IMEI / AndroidId / 手机号。

## 十、作者

- 作者：**Marsz**
- QQ：**483018259**
