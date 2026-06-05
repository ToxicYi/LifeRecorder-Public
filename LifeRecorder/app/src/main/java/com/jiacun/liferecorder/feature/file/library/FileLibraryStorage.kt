package com.jiacun.liferecorder.feature.file.library

/**
 * FileLibraryStorage
 *
 * 管理 LifeRecorder 自己的文件库：保存用户导入/分享进来的文件记录，
 * 并把真实文件复制到 App 私有目录。
 */

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ImportedFileItem(
    // 文件显示名称。
    val name: String,
    // App 内部保存后的文件 Uri。
    val uri: String,
    // 文件 MIME type。
    val mimeType: String?,
    // 导入时间戳。
    val addedTime: Long,
    // 原始分享或选择文件的 Uri，主要用于追踪来源。
    val originalUri: String? = null,
    // 文件大小，单位字节。
    val sizeBytes: Long = 0L
)

// 文件库元数据保存的 SharedPreferences 文件名。
private const val FILE_LIBRARY_PREFS_NAME = "file_library"
// 已导入文件列表在 SharedPreferences 中的 key。
private const val KEY_IMPORTED_FILES = "imported_files"

fun saveImportedFile(context: Context, item: ImportedFileItem) {
    val files = getImportedFiles(context).toMutableList()
    files.removeAll { it.uri == item.uri }
    files.add(0, item)

    val json = JSONArray()
    files.forEach { file ->
        json.put(
            JSONObject()
                .put("name", file.name)
                .put("uri", file.uri)
                .put("mimeType", file.mimeType)
                .put("addedTime", file.addedTime)
                .put("originalUri", file.originalUri)
                .put("sizeBytes", file.sizeBytes)
        )
    }

    context.getSharedPreferences(FILE_LIBRARY_PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString(KEY_IMPORTED_FILES, json.toString())
        .apply()
}

fun getImportedFiles(context: Context): List<ImportedFileItem> {
    val text = context.getSharedPreferences(FILE_LIBRARY_PREFS_NAME, Context.MODE_PRIVATE)
        .getString(KEY_IMPORTED_FILES, null)
        ?: return emptyList()

    return runCatching {
        val json = JSONArray(text)
        buildList {
            for (index in 0 until json.length()) {
                val item = json.getJSONObject(index)
                add(
                    ImportedFileItem(
                        name = item.optString("name"),
                        uri = item.optString("uri"),
                        mimeType = item.optString("mimeType").ifBlank { null },
                        addedTime = item.optLong("addedTime"),
                        originalUri = item.optString("originalUri").ifBlank { null },
                        sizeBytes = item.optLong("sizeBytes")
                    )
                )
            }
        }.sortedByDescending { it.addedTime }
    }.getOrDefault(emptyList())
}

fun importSharedUriToFileLibrary(
    context: Context,
    uri: Uri,
    mimeType: String? = null
): ImportedFileItem {
    val addedTime = System.currentTimeMillis()
    val displayName = uri.resolveDisplayName(context)
    val resolvedMimeType = mimeType ?: context.contentResolver.getType(uri)
    val directory = File(
        File(context.filesDir, "life_files"),
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(addedTime))
    )
    directory.mkdirs()

    val targetFile = File(directory, uniqueFileName(directory, displayName, addedTime))
    var sizeBytes = 0L

    context.contentResolver.openInputStream(uri)?.use { input ->
        targetFile.outputStream().use { output ->
            sizeBytes = input.copyTo(output)
        }
    } ?: error("无法读取分享文件")

    val item = ImportedFileItem(
        name = displayName,
        uri = targetFile.absolutePath,
        mimeType = resolvedMimeType,
        addedTime = addedTime,
        originalUri = uri.toString(),
        sizeBytes = sizeBytes
    )
    saveImportedFile(context, item)
    return item
}

fun importSharedTextToFileLibrary(
    context: Context,
    text: String
): ImportedFileItem {
    val addedTime = System.currentTimeMillis()
    val directory = File(
        File(context.filesDir, "life_files"),
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(addedTime))
    )
    directory.mkdirs()

    val fileName = "shared_text_$addedTime.txt"
    val targetFile = File(directory, fileName)
    targetFile.writeText(text, Charsets.UTF_8)

    val item = ImportedFileItem(
        name = fileName,
        uri = targetFile.absolutePath,
        mimeType = "text/plain",
        addedTime = addedTime,
        originalUri = null,
        sizeBytes = targetFile.length()
    )
    saveImportedFile(context, item)
    return item
}

fun formatImportedFileSize(bytes: Long): String {
    if (bytes <= 0L) {
        return "未知大小"
    }

    val kb = bytes / 1024.0
    if (kb < 1024) {
        return String.format(Locale.getDefault(), "%.0f KB", kb)
    }

    val mb = kb / 1024.0
    return String.format(Locale.getDefault(), "%.1f MB", mb)
}

private fun Uri.resolveDisplayName(context: Context): String {
    val fallbackName = lastPathSegment ?: "shared_file_${System.currentTimeMillis()}"
    var displayName = fallbackName

    context.contentResolver.query(
        this,
        arrayOf(OpenableColumns.DISPLAY_NAME),
        null,
        null,
        null
    )?.use { cursor ->
        if (cursor.moveToFirst()) {
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0) {
                displayName = cursor.getString(nameIndex) ?: fallbackName
            }
        }
    }

    return displayName.ifBlank { fallbackName }
}

private fun uniqueFileName(directory: File, displayName: String, addedTime: Long): String {
    val cleanName = displayName
        .replace("\\", "_")
        .replace("/", "_")
        .ifBlank { "shared_file" }

    val candidate = File(directory, cleanName)
    if (!candidate.exists()) {
        return cleanName
    }

    val dotIndex = cleanName.lastIndexOf('.')
    val baseName = if (dotIndex > 0) cleanName.substring(0, dotIndex) else cleanName
    val extension = if (dotIndex > 0) cleanName.substring(dotIndex) else ""
    return "${baseName}_$addedTime$extension"
}
