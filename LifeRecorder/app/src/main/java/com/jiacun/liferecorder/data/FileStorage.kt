package com.jiacun.liferecorder.data

/**
 * FileStorage
 *
 * 提供手机公共目录和 SAF 授权目录的浏览能力，负责读取目录、转换文件条目、
 * 保存树形 Uri，并用第三方 App 打开文件。
 */

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import java.io.File

data class LifeFileItem(
    // 文件或文件夹名称。
    val name: String,
    // 文件路径或 SAF Uri 字符串。
    val uriOrPath: String,
    // 文件扩展名或 MIME 类型辅助信息。
    val type: String?,
    // 是否为文件夹。
    val isDirectory: Boolean,
    // 访问受限时的错误说明。
    val accessError: String? = null
)

data class FileLoadResult(
    // 当前目录读取到的文件/文件夹列表。
    val items: List<LifeFileItem>,
    // 读取失败或受限时的提示文本。
    val errorMessage: String? = null
)

// 文件浏览配置保存的 SharedPreferences 文件名。
private const val FILE_PREFS_NAME = "file_storage"
// SAF 树形目录 Uri 的保存 key。
private const val KEY_TREE_URI = "tree_uri"

fun loadSharedStorageFiles(): FileLoadResult {
    return loadDirectoryChildren(Environment.getExternalStorageDirectory())
}

fun loadPathDirectoryFiles(path: String): FileLoadResult {
    return loadDirectoryChildren(File(path))
}

fun loadDirectoryChildren(directory: File): FileLoadResult {
    return try {
        if (!directory.exists() || !directory.isDirectory) {
            return FileLoadResult(
                items = emptyList(),
                errorMessage = "目录不存在：${directory.absolutePath}"
            )
        }

        if (!directory.canRead()) {
            return FileLoadResult(
                items = listOf(directory.toLifeFileItem(accessError = "权限不够或目录不可读")),
                errorMessage = "没有权限读取目录：${directory.absolutePath}"
            )
        }

        val items = directory.listFiles()
            ?.sortedWith(compareByDescending<java.io.File> { it.isDirectory }.thenBy { it.name.lowercase() })
            ?.map { file ->
                if (isRestrictedAndroidDirectory(file)) {
                    file.toLifeFileItem(accessError = "系统限制访问")
                } else {
                    file.toLifeFileItem()
                }
            }
            ?: emptyList()

        FileLoadResult(items = items)
    } catch (e: Exception) {
        FileLoadResult(
            items = emptyList(),
            errorMessage = "读取目录失败：${e.message}"
        )
    }
}

private fun isRestrictedAndroidDirectory(file: File): Boolean {
    val path = file.absolutePath.replace("\\", "/")
    return path.endsWith("/Android/data") || path.endsWith("/Android/obb")
}

private fun File.toLifeFileItem(accessError: String? = null): LifeFileItem {
    return LifeFileItem(
        name = name.ifBlank { absolutePath },
        uriOrPath = absolutePath,
        type = extension.ifBlank { null },
        isDirectory = isDirectory,
        accessError = accessError
    )
}

fun saveAuthorizedTreeUri(context: Context, uri: Uri) {
    context.getSharedPreferences(FILE_PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString(KEY_TREE_URI, uri.toString())
        .apply()
}

fun getAuthorizedTreeUri(context: Context): Uri? {
    val uriText = context.getSharedPreferences(FILE_PREFS_NAME, Context.MODE_PRIVATE)
        .getString(KEY_TREE_URI, null)

    return uriText?.let(Uri::parse)
}

fun loadAuthorizedTreeFiles(context: Context, treeUri: Uri): FileLoadResult {
    return try {
        val tree = DocumentFile.fromTreeUri(context, treeUri)
            ?: return FileLoadResult(
                items = emptyList(),
                errorMessage = "无法读取授权文件夹"
            )

        val items = tree.listFiles()
            .sortedWith(compareByDescending<DocumentFile> { it.isDirectory }.thenBy { it.name.orEmpty().lowercase() })
            .map { file ->
                LifeFileItem(
                    name = file.name ?: "未命名",
                    uriOrPath = file.uri.toString(),
                    type = file.type,
                    isDirectory = file.isDirectory
                )
            }

        FileLoadResult(items = items)
    } catch (e: Exception) {
        FileLoadResult(
            items = emptyList(),
            errorMessage = "读取授权文件夹失败：${e.message}"
        )
    }
}

fun loadDocumentDirectoryFiles(context: Context, directoryUri: Uri): FileLoadResult {
    return loadContentDirectoryFiles(context, directoryUri)
}

fun loadContentDirectoryFiles(context: Context, directoryUri: Uri): FileLoadResult {
    return try {
        val documentId = directoryUri.resolveDocumentId()
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
            directoryUri,
            documentId
        )

        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE
        )

        val items = mutableListOf<LifeFileItem>()

        context.contentResolver.query(
            childrenUri,
            projection,
            null,
            null,
            null
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID
            )
            val nameColumn = cursor.getColumnIndexOrThrow(
                DocumentsContract.Document.COLUMN_DISPLAY_NAME
            )
            val mimeColumn = cursor.getColumnIndexOrThrow(
                DocumentsContract.Document.COLUMN_MIME_TYPE
            )

            while (cursor.moveToNext()) {
                val childDocumentId = cursor.getString(idColumn)
                val name = cursor.getString(nameColumn) ?: "未命名"
                val mimeType = cursor.getString(mimeColumn)
                val isDirectory = mimeType == DocumentsContract.Document.MIME_TYPE_DIR
                val childUri = DocumentsContract.buildDocumentUriUsingTree(
                    directoryUri,
                    childDocumentId
                )

                items.add(
                    LifeFileItem(
                        name = name,
                        uriOrPath = childUri.toString(),
                        type = if (isDirectory) null else mimeType,
                        isDirectory = isDirectory
                    )
                )
            }
        } ?: return FileLoadResult(
            items = emptyList(),
            errorMessage = "无法读取授权目录"
        )

        FileLoadResult(
            items = items.sortedWith(
                compareByDescending<LifeFileItem> { it.isDirectory }
                    .thenBy { it.name.lowercase() }
            )
        )
    } catch (e: Exception) {
        FileLoadResult(
            items = emptyList(),
            errorMessage = "读取授权目录失败：${e.message}"
        )
    }
}

fun openLifeFileItem(context: Context, item: LifeFileItem) {
    if (item.isDirectory) {
        Toast.makeText(context, "暂时不支持打开文件夹", Toast.LENGTH_SHORT).show()
        return
    }

    if (item.accessError != null) {
        Toast.makeText(context, "权限不够，无法打开", Toast.LENGTH_SHORT).show()
        return
    }

    try {
        val uri = item.toOpenableUri(context)
        val mimeType = item.resolveMimeType(context, uri)
        openFileWithThirdPartyApp(context, uri, mimeType)
    } catch (e: Exception) {
        Toast.makeText(context, "无法打开文件：${e.message}", Toast.LENGTH_SHORT).show()
    }
}

fun openFileWithThirdPartyApp(
    context: Context,
    uri: Uri,
    mimeType: String?
) {
    val readFlag = Intent.FLAG_GRANT_READ_URI_PERMISSION

    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, mimeType ?: "*/*")
        clipData = ClipData.newUri(context.contentResolver, "LifeRecorder file", uri)
        addFlags(readFlag)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    context.packageManager.queryIntentActivities(intent, 0).forEach { resolveInfo ->
        context.grantUriPermission(
            resolveInfo.activityInfo.packageName,
            uri,
            readFlag
        )
    }

    val chooser = Intent.createChooser(intent, "打开方式").apply {
        clipData = intent.clipData
        addFlags(readFlag)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    try {
        context.startActivity(chooser)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "没有可用应用打开此文件", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "打开失败：${e.message}", Toast.LENGTH_SHORT).show()
    }
}

private fun LifeFileItem.toOpenableUri(context: Context): Uri {
    val parsedUri = Uri.parse(uriOrPath)

    if (parsedUri.scheme == "content") {
        return parsedUri
    }

    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        File(uriOrPath)
    )
}

private fun LifeFileItem.resolveMimeType(context: Context, uri: Uri): String? {
    if (type?.contains("/") == true) {
        return type
    }

    val resolverType = if (uri.scheme == "content") {
        context.contentResolver.getType(uri)
    } else {
        null
    }

    if (!resolverType.isNullOrBlank()) {
        return resolverType
    }

    val extension = File(name).extension.ifBlank {
        File(uriOrPath).extension
    }

    return MimeTypeMap.getSingleton()
        .getMimeTypeFromExtension(extension.lowercase())
}

private fun Uri.toTreeDocumentUri(): Uri {
    return try {
        val documentId = DocumentsContract.getDocumentId(this)
        DocumentsContract.buildTreeDocumentUri(authority ?: return this, documentId)
    } catch (e: Exception) {
        this
    }
}

private fun Uri.resolveDocumentId(): String {
    return try {
        DocumentsContract.getDocumentId(this)
    } catch (e: Exception) {
        DocumentsContract.getTreeDocumentId(this)
    }
}
