package com.jiacun.liferecorder.data

/**
 * RecentStorage
 *
 * 聚合最近 30 天内的笔记、系统图片和导入文件，统一转换成 RecentItem 供最近页展示。
 */

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

data class RecentItem(
    // 最近资源唯一 id。
    val id: String,
    // 展示名称。
    val name: String,
    // 图片 Uri、文件 Uri 或路径；笔记可以为空。
    val uriOrPath: String?,
    // 资源类型：note / image / file。
    val type: String,
    // 来源标签，例如 笔记 / 截图 / 相册 / 导入文件。
    val source: String,
    // 资源发生或更新时间戳。
    val timeMillis: Long,
    // 文件或图片大小文本。
    val sizeText: String?,
    // 笔记或文件预览文本。
    val previewText: String?
)

fun loadRecentItems(context: Context, days: Int = 30): List<RecentItem> {
    // 只读取指定天数内的资源。
    val sinceMillis = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(days.toLong())

    return buildList {
        addAll(loadRecentNotes(context, sinceMillis))
        addAll(loadRecentImages(context, sinceMillis))
        addAll(loadRecentImportedFiles(context, sinceMillis))
    }.sortedByDescending { it.timeMillis }
}

private fun loadRecentNotes(context: Context, sinceMillis: Long): List<RecentItem> {
    val prefs = getNotePrefs(context)
    val noteCount = getNoteCount(prefs)
    val timeFormat = SimpleDateFormat("yyyy/M/d HH:mm", Locale.getDefault())

    return getActiveNoteIds(prefs, noteCount)
        .map { id -> getNote(prefs, id) }
        .mapNotNull { note ->
            val timeMillis = runCatching {
                timeFormat.parse(note.updatedTime)?.time
            }.getOrNull() ?: return@mapNotNull null

            if (timeMillis < sinceMillis) {
                return@mapNotNull null
            }

            if (note.title.isBlank() && note.content.isBlank()) {
                return@mapNotNull null
            }

            RecentItem(
                id = note.id.toString(),
                name = note.title.ifBlank { "无标题笔记" },
                uriOrPath = null,
                type = "note",
                source = "笔记",
                timeMillis = timeMillis,
                sizeText = null,
                previewText = note.content.take(80)
            )
        }
}

private fun loadRecentImages(context: Context, sinceMillis: Long): List<RecentItem> {
    return runCatching {
        val sinceSeconds = TimeUnit.MILLISECONDS.toSeconds(sinceMillis)
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.DATE_MODIFIED,
            MediaStore.Images.Media.SIZE
        )
        val selection = "${MediaStore.Images.Media.DATE_ADDED} >= ? OR ${MediaStore.Images.Media.DATE_MODIFIED} >= ?"
        val selectionArgs = arrayOf(sinceSeconds.toString(), sinceSeconds.toString())
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
        val items = mutableListOf<RecentItem>()

        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val addedColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            val modifiedColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn) ?: "图片"
                val addedMillis = TimeUnit.SECONDS.toMillis(cursor.getLong(addedColumn))
                val modifiedMillis = TimeUnit.SECONDS.toMillis(cursor.getLong(modifiedColumn))
                val timeMillis = maxOf(addedMillis, modifiedMillis)
                val uri = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id
                )

                items.add(
                    RecentItem(
                        id = "image-$id",
                        name = name,
                        uriOrPath = uri.toString(),
                        type = "image",
                        source = if (name.contains("screenshot", ignoreCase = true) || name.contains("截屏")) {
                            "截图"
                        } else {
                            "相册"
                        },
                        timeMillis = timeMillis,
                        sizeText = formatSize(cursor.getLong(sizeColumn)),
                        previewText = null
                    )
                )
            }
        }

        items
    }.getOrDefault(emptyList())
}

private fun loadRecentImportedFiles(context: Context, sinceMillis: Long): List<RecentItem> {
    return getImportedFiles(context)
        .filter { file -> file.addedTime >= sinceMillis }
        .map { file ->
            RecentItem(
                id = "file-${file.uri}",
                name = file.name,
                uriOrPath = file.uri,
                type = "file",
                source = "导入文件",
                timeMillis = file.addedTime,
                sizeText = "${file.mimeType ?: "未知类型"} · ${formatImportedFileSize(file.sizeBytes)}",
                previewText = null
            )
        }
}

private fun formatSize(bytes: Long): String {
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
