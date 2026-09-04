# ==================== 数据模型 ====================
# Gson 依赖字段名做反射解析，模型类与字段必须保留，否则线上接口解析会得到 null
-keepclassmembers,allowoptimization class com.marsz.miniquery.data.model.** { *; }
-keep class com.marsz.miniquery.data.model.** { *; }
-keepclassmembers class com.marsz.miniquery.data.net.** { *; }

# ==================== OkHttp / 网络栈 ====================
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-keepattributes Signature
-keepattributes *Annotation*
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-keep class okio.** { *; }
-dontnote okhttp3.**
-dontnote okio.**

# ==================== Gson ====================
-keepattributes EnclosingMethod
-dontwarn com.google.gson.**
-keep class com.google.gson.** { *; }
-keepclassmembers class * implements com.google.gson.TypeAdapterFactory { *; }
-keepclassmembers class * implements com.google.gson.JsonSerializer { *; }
-keepclassmembers class * implements com.google.gson.JsonDeserializer { *; }
# 反射用的泛型签名
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}

# ==================== Coil / 图片解码 ====================
-dontwarn coil.**
-dontwarn com.caverock.androidsvg.**
-keep class coil.** { *; }

# ==================== Kotlin ====================
-dontwarn kotlin.**
-dontwarn kotlinx.coroutines.**
-keepclassmembers class **$WhenMappings {
    <fields>;
}
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}
-dontwarn org.jetbrains.annotations.**

# ==================== Android 组件 ====================
-keep class com.marsz.miniquery.MainActivity { *; }
-keep class com.marsz.miniquery.MiniQueryApp { *; }

# 移除日志（release 构建下不产生任何 Log 调用开销）
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
}
