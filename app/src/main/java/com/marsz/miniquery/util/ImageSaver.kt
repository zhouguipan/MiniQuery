package com.marsz.miniquery.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.marsz.miniquery.cache.CacheCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 图片保存到相册。
 *
 * 相册/皮肤等分类不落磁盘缓存，因此一律**从原始 URL 下载**后写入公共相册，
 * 不依赖任何本地缓存文件。
 */
object ImageSaver {

    /**
     * 保存图片（suspend，需在协程中调用）。
     * @param url 图片原始 URL（cos_url 等）
     */
    suspend fun save(context: Context, url: String) {
        // 1) 下载原图到临时文件
        val tmp = withContext(Dispatchers.IO) {
            runCatching { downloadToTemp(context, url) }.getOrNull()
        }

        if (tmp == null || !tmp.exists() || tmp.length() == 0L) {
            Toast.makeText(context, "下载失败，请检查网络", Toast.LENGTH_SHORT).show()
            return
        }

        // 2) 写入相册
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            writeViaMediaStore(context, tmp)      // Android 10+ 免权限
        } else {
            val activity = context as? ComponentActivity
            if (activity != null) {
                // Android 9- 需申请存储权限
                requestLegacyPermission(activity) { writeViaMediaStore(context, tmp) }
            } else {
                writeViaMediaStore(context, tmp)
            }
        }
    }

    /** 从 URL 下载到应用私有临时文件 */
    private fun downloadToTemp(context: Context, url: String): File {
        val tmp = File(context.cacheDir, "save_${System.currentTimeMillis()}_${url.hashCode()}.jpg")
        URL(url).openStream().use { input ->
            FileOutputStream(tmp).use { output -> input.copyTo(output) }
        }
        return tmp
    }

    /** 通过 MediaStore 写入 Pictures/MiniQuery */
    private fun writeViaMediaStore(context: Context, src: File) {
        val resolver = context.contentResolver
        val filename = "MiniQuery_${
            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        }.jpg"

        val uri: Uri? = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/MiniQuery")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            } else {
                @Suppress("DEPRECATION")
                val pictures = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES
                )
                val target = File(pictures, "MiniQuery/$filename").apply {
                    parentFile?.mkdirs()
                }
                Uri.fromFile(target)
            }
        } catch (e: Exception) { null }

        if (uri == null) {
            Toast.makeText(context, "保存失败", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            resolver.openOutputStream(uri)?.use { os ->
                val bitmap = BitmapFactory.decodeFile(src.absolutePath)
                if (bitmap != null) {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 92, os)
                    bitmap.recycle()
                } else {
                    src.inputStream().use { it.copyTo(os) }   // webp/gif 直接拷贝
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                resolver.update(
                    uri,
                    ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) },
                    null, null
                )
            }
            Toast.makeText(context, "已保存到相册/MiniQuery", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "保存失败：${e.message}", Toast.LENGTH_SHORT).show()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                runCatching { resolver.delete(uri, null, null) }
            }
        } finally {
            // 清理临时文件
            runCatching { if (src.name.startsWith("save_")) src.delete() }
        }
    }

    /** Android 9- 申请 WRITE_EXTERNAL_STORAGE */
    private fun requestLegacyPermission(
        activity: ComponentActivity,
        onGranted: () -> Unit
    ) {
        val launcher = activity.activityResultRegistry.register(
            "image_saver_perm_${System.currentTimeMillis()}",
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) onGranted()
            else Toast.makeText(activity, "需要存储权限才能保存到相册", Toast.LENGTH_SHORT).show()
        }
        launcher.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }
}