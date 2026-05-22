package com.jiacun.liferecorder.data

/**
 * PhoneSyncStorage
 *
 * 构造 phone_sync 相关请求体，生成稳定 fileId/contentHash，
 * 并保存 fileId 到本地 Uri 的授权映射。
 */

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.documentfile.provider.DocumentFile

data class PhoneFileIndexRequest(
    // 请求体 schema 版本。
    val schemaVersion: Int,
    // 当前设备标识，第一版固定 android_main。
    val deviceId: String,
    // 本次索引更新时间。
    val updatedTime: Long,
    // 手机文件索引列表。
    val files: List<PhoneFileIndexItem>
)

data class PhoneFileIndexItem(
    // 稳定文件身份 id。
    val fileId: String,
    // 文件名。
    val name: String,
    // 手机侧相对路径，第一版使用显示名称。
    val relativePhonePath: String,
    // MIME type。
    val mimeType: String?,
    // 文件大小，单位字节。
    val size: Long,
    // 文件最后修改时间。
    val lastModified: Long,
    // 内容变化 hash，不用于文件身份。
    val contentHash: String,
    // 后端是否已缓存真实文件。
    val availableLocally: Boolean,
    // 后端缓存路径。
    val cachedPath: String?
)

data class AppFileIndexRequest(
    // 请求体 schema 版本。
    val schemaVersion: Int,
    // 当前设备标识。
    val deviceId: String,
    // App 文件索引更新时间。
    val updatedTime: Long,
    // LifeRecorder 虚拟文件列表。
    val files: List<AppFileIndexItem>
)

data class AppFileIndexItem(
    // 文件身份 id，和手机文件 id 保持一致。
    val fileId: String,
    // 文件名。
    val name: String,
    // LifeRecorder 内部展示用虚拟路径。
    val virtualPath: String,
    // 文件来源，例如 file_picker。
    val source: String,
    // MIME type。
    val mimeType: String?,
    // 文件大小，单位字节。
    val size: Long,
    // 文件最后修改时间。
    val lastModified: Long,
    // 内容变化 hash。
    val contentHash: String,
    // 后端是否已缓存真实文件。
    val availableLocally: Boolean,
    // 后端缓存路径。
    val cachedPath: String?,
    // 关联的手机文件 id。
    val linkedPhoneFileId: String
)

data class AppFilesIndexResponse(
    // 响应 schema 版本。
    val schemaVersion: Int,
    // 设备标识。
    val deviceId: String,
    // 文件索引更新时间。
    val updatedTime: Long,
    // App 虚拟文件列表。
    val files: List<AppFileItem>
)

data class AppFileItem(
    // 文件身份 id。
    val fileId: String,
    // 文件名。
    val name: String,
    // LifeRecorder 虚拟路径。
    val virtualPath: String,
    // 文件来源。
    val source: String,
    // MIME type。
    val mimeType: String?,
    // 文件大小，单位字节。
    val size: Long,
    // 文件最后修改时间。
    val lastModified: Long,
    // 内容变化 hash。
    val contentHash: String,
    // 后端是否已缓存真实文件。
    val availableLocally: Boolean,
    // 后端缓存路径。
    val cachedPath: String?,
    // 关联的手机文件 id。
    val linkedPhoneFileId: String?
)

data class PendingPhoneFileRequest(
    // Agent 请求 id。
    val id: String,
    // 请求类型，目前处理 fetch_phone_file。
    val type: String,
    // 请求状态，目前只处理 pending。
    val status: String,
    // 请求目标设备。
    val deviceId: String,
    // Agent 需要获取的文件 id。
    val fileId: String
)

// 保存 fileId -> Uri 授权映射的 SharedPreferences 文件名。
private const val PHONE_FILE_URI_PREFS = "phone_file_uri_mapping"

fun buildPhoneFileIndexRequest(
    context: Context,
    uri: Uri
): PhoneFileIndexRequest {
    val now = System.currentTimeMillis()
    val name = readDisplayName(context, uri).ifBlank { "unknown_file" }
    val mimeType = context.contentResolver.getType(uri)
    val documentFile = DocumentFile.fromSingleUri(context, uri)
    val size = readSize(context, uri, documentFile).coerceAtLeast(0L)
    val lastModified = documentFile?.lastModified()
        ?.takeIf { it > 0L }
        ?: now
    val identityHash = stableHash(uri.toString())
    val contentHash = stableHash("$name|$size|$lastModified")

    val item = PhoneFileIndexItem(
        fileId = "phone_file_$identityHash",
        name = name,
        relativePhonePath = name,
        mimeType = mimeType,
        size = size,
        lastModified = lastModified,
        contentHash = contentHash,
        availableLocally = false,
        cachedPath = null
    )

    return PhoneFileIndexRequest(
        schemaVersion = 1,
        deviceId = "android_main",
        updatedTime = now,
        files = listOf(item)
    )
}

fun buildAppFileIndexRequest(phoneRequest: PhoneFileIndexRequest): AppFileIndexRequest {
    return AppFileIndexRequest(
        schemaVersion = phoneRequest.schemaVersion,
        deviceId = phoneRequest.deviceId,
        updatedTime = phoneRequest.updatedTime,
        files = phoneRequest.files.map { phoneFile ->
            AppFileIndexItem(
                fileId = phoneFile.fileId,
                name = phoneFile.name,
                virtualPath = "LifeRecorder文件/${phoneFile.name}",
                source = "file_picker",
                mimeType = phoneFile.mimeType,
                size = phoneFile.size,
                lastModified = phoneFile.lastModified,
                contentHash = phoneFile.contentHash,
                availableLocally = false,
                cachedPath = null,
                linkedPhoneFileId = phoneFile.fileId
            )
        }
    )
}

fun savePhoneFileUriMapping(
    context: Context,
    fileId: String,
    uri: Uri
) {
    context.getSharedPreferences(PHONE_FILE_URI_PREFS, Context.MODE_PRIVATE)
        .edit()
        .putString(fileId, uri.toString())
        .apply()
}

fun getPhoneFileUriMapping(
    context: Context,
    fileId: String
): String? {
    return context.getSharedPreferences(PHONE_FILE_URI_PREFS, Context.MODE_PRIVATE)
        .getString(fileId, null)
}

private fun readDisplayName(context: Context, uri: Uri): String {
    context.contentResolver.query(
        uri,
        arrayOf(OpenableColumns.DISPLAY_NAME),
        null,
        null,
        null
    )?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (nameIndex >= 0 && cursor.moveToFirst()) {
            return cursor.getString(nameIndex).orEmpty()
        }
    }

    return DocumentFile.fromSingleUri(context, uri)?.name.orEmpty()
}

private fun readSize(
    context: Context,
    uri: Uri,
    documentFile: DocumentFile?
): Long {
    context.contentResolver.query(
        uri,
        arrayOf(OpenableColumns.SIZE),
        null,
        null,
        null
    )?.use { cursor ->
        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
        if (sizeIndex >= 0 && cursor.moveToFirst() && !cursor.isNull(sizeIndex)) {
            return cursor.getLong(sizeIndex)
        }
    }

    return documentFile?.length() ?: 0L
}

private fun stableHash(value: String): String {
    return Integer.toUnsignedString(value.hashCode(), 16)
}
