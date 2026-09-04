package com.marsz.miniquery.data.net

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * 后端字段类型不稳定（同一字段可能是 number / string / null），
 * 这里把 JSON 标量统一按"原始字面量"读成 String，避免解析崩溃。
 */
class LenientStringAdapter : TypeAdapter<String>() {
    override fun read(reader: JsonReader): String? {
        return when (reader.peek()) {
            JsonToken.NULL -> { reader.nextNull(); null }
            JsonToken.STRING,
            JsonToken.NUMBER,
            JsonToken.BOOLEAN -> reader.nextString()
            else -> { reader.skipValue(); null }
        }
    }

    override fun write(out: JsonWriter, value: String?) {
        if (value == null) out.nullValue() else out.value(value)
    }
}

/** 业务异常：code 非 0 时抛出，携带面向用户的文案 */
class ApiException(val code: Int, message: String) : Exception(message)

/** 网络 / 解析异常的统一包装 */
class ParseException(message: String = "服务器返回了异常数据") : Exception(message)

object Http {

    /**
     * 由 Application 注入的 User-Agent（含应用版本与设备信息）。
     * 未注入时请求仍可正常发出，只是不带该头。
     */
    @Volatile
    private var userAgent: String? = null

    /** 在 Application.onCreate 中调用一次 */
    fun init(context: android.content.Context) {
        userAgent = UserAgent.get(context)
    }

    /**
     * 全局共享的 OkHttpClient。
     * 复用同一份连接池与线程池，避免每次请求新建连接带来的延迟与内存开销。
     *
     * 注：后端接口为 http://，因此这里不做 TLS 限制，也不拦截明文流量，
     * 由 AndroidManifest 的 usesCleartextTraffic 与网络安全配置统一放行。
     */
    @PublishedApi
    internal val client: OkHttpClient by lazy {
        val builder = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .callTimeout(20, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            // 复用连接：减少建连耗时，同时控制空闲连接数量避免占用内存
            .connectionPool(ConnectionPool(6, 30, TimeUnit.SECONDS))
            // 限制并发请求数，防止批量拉取成员资料时把线程池打满导致界面卡顿
            .dispatcher(
                Dispatcher(Executors.newFixedThreadPool(4)).apply {
                    maxRequests = 12
                    maxRequestsPerHost = 8
                }
            )

        // 所有请求统一附加带设备信息的 User-Agent
        userAgent?.let { ua ->
            builder.addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", ua)
                    .build()
                chain.proceed(request)
            }
        }

        builder.build()
    }

    @PublishedApi
    internal val gson: Gson by lazy {
        GsonBuilder()
            .registerTypeAdapter(String::class.java, LenientStringAdapter())
            .create()
    }

    /** 在 IO 线程发起 GET 并解析为 [T] */
    suspend inline fun <reified T : Any> get(url: String): T = withContext(Dispatchers.IO) {
        val json = raw(url)
        try {
            gson.fromJson<T>(json, object : TypeToken<T>() {}.type)
                ?: throw ParseException()
        } catch (e: ParseException) {
            throw e
        } catch (_: Exception) {
            throw ParseException()
        }
    }

    /** 原始字符串（用于返回结构不固定的接口） */
    suspend fun raw(url: String): String = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json, text/plain, */*")
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            response.body?.string() ?: throw ParseException()
        }
    }

    /** 下载二进制（表情图集远程更新时使用） */
    suspend fun download(url: String): ByteArray? = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@runCatching null
                response.body?.bytes()
            }
        }.getOrNull()
    }
}
