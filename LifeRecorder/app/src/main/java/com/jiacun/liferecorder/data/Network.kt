package com.jiacun.liferecorder.data

/**
 * Network
 *
 * Android App 的统一网络层，负责请求 Spring Boot 后端的 ping、笔记上传、
 * AI/Agent 聊天、Daily Context、phone_sync 和 Agent 生成文件接口。
 */

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.documentfile.provider.DocumentFile
import java.io.DataOutputStream
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

data class AiChatResponse(
    // 聊天回复正文。
    val reply: String,
    // 后端回复模式：ai 或 agent。
    val mode: String? = null,
    // Agent 任务类型，例如 daily_summary / file_read。
    val taskType: String? = null,
    // Agent 生成文件的相对路径（兼容旧版）。
    val generatedPath: String? = null,
    // Agent 生成的文件列表。
    val generatedFiles: List<GeneratedFileInfo>? = null
)

data class GeneratedFileInfo(
    val name: String,
    val relativePath: String,
    val mimeType: String?,
    val size: Long?,
    val preview: String?
)

/**
 * 已上传到后端的附件信息，用于发送消息时附带给后端
 */
data class UploadedAttachment(
    val attachmentId: String,
    val name: String,
    val relativePath: String,
    val mimeType: String?,
    val type: String  // "file" 或 "image"
)

data class AgentGeneratedFileItem(
    // 生成文件名称。
    val name: String,
    // 文件内容预览。
    val preview: String,
    // 后端工作区内的相对路径。
    val relativePath: String,
    // 文件大小，单位字节。
    val size: Long,
    // 文件创建时间。
    val createdTime: Long,
    // 文件更新时间。
    val updatedTime: Long
)

// 测试公网连接
fun testPublicPing(baseUrl: String): String {
    var conn: HttpURLConnection? = null

    return try {
        val url = URL("${baseUrl.removeSuffix("/")}/debug/process-test")

        conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 5000
        conn.readTimeout = 5000

        val responseCode = conn.responseCode
        val responseText = if (responseCode in 200..299) {
            conn.inputStream.bufferedReader(Charsets.UTF_8).readText()
        } else {
            conn.errorStream?.bufferedReader(Charsets.UTF_8)?.readText()
                ?: "HTTP $responseCode"
        }

        when {
            responseText.contains("SPRING_PROCESS_OK") -> {
                "连接正常"
            }
            isSakuraTunnelUnavailable(responseText) -> {
                "隧道不可用，请检查樱花客户端是否在线"
            }
            responseCode == 503 -> {
                "隧道不可用，请检查樱花客户端是否在线"
            }
            else -> {
                "连接失败：HTTP $responseCode，${summarizeNetworkBody(responseText)}"
            }
        }
    } catch (e: java.net.SocketTimeoutException) {
        "连接超时，请检查后端或隧道"
    } catch (e: Exception) {
        "连接失败：${summarizeNetworkBody(e.message ?: e.javaClass.simpleName)}"
    } finally {
        conn?.disconnect()
    }
}

private fun isSakuraTunnelUnavailable(text: String): Boolean {
    return text.contains("503 Service Unavailable", ignoreCase = true) ||
        text.contains("Sakura Frp", ignoreCase = true) ||
        text.contains("隧道后端暂时不可用")
}

private fun summarizeNetworkBody(text: String): String {
    val withoutTags = text
        .replace(Regex("<[^>]+>"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()

    return withoutTags
        .ifBlank { "无响应内容" }
        .take(120)
}

// 上传当前笔记到公网后端
fun uploadCurrentNote(baseUrl: String, title: String, content: String): String {
    val url = URL("${baseUrl.removeSuffix("/")}/life/upload-note")

    val conn = url.openConnection() as HttpURLConnection
    conn.requestMethod = "POST"
    conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
    conn.doOutput = true
    conn.connectTimeout = 5000
    conn.readTimeout = 5000

    val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    val safeTitle = if (title.isBlank()) {
        "无标题笔记"
    } else {
        title
    }

    val json = """
        {
          "date": "${escapeJson(today)}",
          "title": "${escapeJson(safeTitle)}",
          "content": "${escapeJson(content)}"
        }
    """.trimIndent()

    OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
        writer.write(json)
    }

    val responseText = if (conn.responseCode in 200..299) {
        conn.inputStream.bufferedReader(Charsets.UTF_8).readText()
    } else {
        conn.errorStream?.bufferedReader(Charsets.UTF_8)?.readText()
            ?: "服务器错误：${conn.responseCode}"
    }

    conn.disconnect()
    return responseText
}

// 读取今日总结
fun readTodaySummary(baseUrl: String): String {
    val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    val url = URL("${baseUrl.removeSuffix("/")}/life/summary?date=$today")

    return url.readText(Charsets.UTF_8)
}

// 发送 AI 对话消息到 Spring Boot 后端
fun sendAiChatMessage(baseUrl: String, message: String, attachments: List<UploadedAttachment>? = null): AiChatResponse {
    var conn: HttpURLConnection? = null

    return try {
        val url = URL("${baseUrl.removeSuffix("/")}/ai/chat-plus")

        conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
        conn.doOutput = true
        conn.connectTimeout = 15_000
        conn.readTimeout = 180_000

        // 构建请求体
        val json = JSONObject()
        json.put("message", message)

        // 附加上传过的附件信息
        if (!attachments.isNullOrEmpty()) {
            val attachmentArray = JSONArray()
            for (att in attachments) {
                val attObj = JSONObject()
                attObj.put("attachmentId", att.attachmentId)
                attObj.put("name", att.name)
                attObj.put("relativePath", att.relativePath)
                attObj.put("mimeType", att.mimeType ?: "application/octet-stream")
                attObj.put("type", att.type)
                attachmentArray.put(attObj)
            }
            json.put("attachments", attachmentArray)
        }

        OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
            writer.write(json.toString())
        }

        val responseCode = conn.responseCode
        val responseText = if (responseCode in 200..299) {
            conn.inputStream.bufferedReader(Charsets.UTF_8).readText()
        } else {
            val errorText = conn.errorStream?.bufferedReader(Charsets.UTF_8)?.readText()
            return AiChatResponse(
                reply = "AI 后端请求失败：HTTP $responseCode${if (errorText.isNullOrBlank()) "" else "，$errorText"}"
            )
        }

        val responseJson = try {
            JSONObject(responseText)
        } catch (e: Exception) {
            return AiChatResponse(
                reply = "AI 后端返回格式错误：${e.message ?: "无法解析 reply"}"
            )
        }

        val reply = responseJson.optString("reply", "")
        val generatedPath = responseJson.optString("generatedPath").ifBlank { null }

        // 解析 generatedFiles 数组
        val generatedFiles: List<GeneratedFileInfo>? = try {
            val filesArray = responseJson.optJSONArray("generatedFiles")
            if (filesArray != null && filesArray.length() > 0) {
                val list = mutableListOf<GeneratedFileInfo>()
                for (i in 0 until filesArray.length()) {
                    val fileObj = filesArray.getJSONObject(i)
                    list.add(GeneratedFileInfo(
                        name = fileObj.optString("name", ""),
                        relativePath = fileObj.optString("relativePath", ""),
                        mimeType = fileObj.optString("mimeType").ifBlank { null },
                        size = if (fileObj.has("size") && !fileObj.isNull("size")) fileObj.getLong("size") else null,
                        preview = fileObj.optString("preview").ifBlank { null }
                    ))
                }
                list
            } else null
        } catch (e: Exception) {
            null
        }

        if (reply.isBlank()) {
            AiChatResponse(
                reply = "AI 后端返回为空：$responseText",
                mode = responseJson.optString("mode").ifBlank { null },
                taskType = responseJson.optString("taskType").ifBlank { null },
                generatedPath = generatedPath,
                generatedFiles = generatedFiles
            )
        } else {
            AiChatResponse(
                reply = reply,
                mode = responseJson.optString("mode").ifBlank { null },
                taskType = responseJson.optString("taskType").ifBlank { null },
                generatedPath = generatedPath,
                generatedFiles = generatedFiles
            )
        }
    } catch (e: java.net.SocketTimeoutException) {
        AiChatResponse(
            reply = "AI/Agent 处理时间较长，请稍后重试或等待结果：${e.message ?: "timeout"}"
        )
    } catch (e: Exception) {
        AiChatResponse(
            reply = "连接 AI 后端失败，请检查服务器地址、Cloudflare Tunnel 和后端服务：${e.message ?: e.javaClass.simpleName}"
        )
    } finally {
        conn?.disconnect()
    }
}

// 处理 JSON 特殊字符
/**
 * 上传附件到后端（先上传，返回 attachmentId，再随消息发送）
 * @param context Android Context
 * @param uri 文件的 Content URI
 * @param type "file" 或 "image"
 * @return UploadedAttachment（成功）或 null（失败）
 */
fun uploadAttachment(context: Context, baseUrl: String, uri: Uri, type: String): UploadedAttachment? {
    var conn: HttpURLConnection? = null
    return try {
        val url = URL("${baseUrl.removeSuffix("/")}/ai/upload-attachment")
        conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.connectTimeout = 20_000
        conn.readTimeout = 60_000

        // multipart/form-data
        val boundary = "----LifeRecorderBoundary${System.currentTimeMillis()}"
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")

        val output = conn.outputStream
        val dos = DataOutputStream(output)

        // 从 ContentResolver 读取文件名和 MIME 类型
        val mimeType = context.contentResolver.getType(uri)
            ?: "application/octet-stream"
        val fileName = getFileName(context, uri) ?: "attachment"

        // 文件字段
        dos.writeBytes("--$boundary\r\n")
        dos.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"${fileName.toByteArray().joinToString("") { String.format("%%%02X", it.toInt()) }}\"\r\n")
        dos.writeBytes("Content-Type: $mimeType\r\n\r\n")

        context.contentResolver.openInputStream(uri)?.use { input ->
            input.copyTo(dos)
        }
        dos.writeBytes("\r\n")

        // type 字段
        dos.writeBytes("--$boundary\r\n")
        dos.writeBytes("Content-Disposition: form-data; name=\"type\"\r\n\r\n")
        dos.writeBytes(type + "\r\n")
        dos.writeBytes("--$boundary--\r\n")
        dos.flush()

        val responseCode = conn.responseCode
        val responseText = if (responseCode in 200..299) {
            conn.inputStream.bufferedReader(Charsets.UTF_8).readText()
        } else {
            conn.errorStream?.bufferedReader(Charsets.UTF_8)?.readText() ?: "HTTP $responseCode"
        }

        if (responseCode !in 200..299) {
            android.util.Log.e("Network", "uploadAttachment failed: $responseText")
            return null
        }

        val json = JSONObject(responseText)
        if (!json.optBoolean("success", false)) {
            android.util.Log.e("Network", "uploadAttachment success=false: ${json.optString("message")}")
            return null
        }

        UploadedAttachment(
            attachmentId = json.optString("attachmentId", ""),
            name = json.optString("name", fileName),
            relativePath = json.optString("relativePath", ""),
            mimeType = mimeType,
            type = type
        )
    } catch (e: Exception) {
        android.util.Log.e("Network", "uploadAttachment exception: ${e.message}")
        null
    } finally {
        conn?.disconnect()
    }
}

/**
 * 从 ContentResolver URI 获取文件名
 */
private fun getFileName(context: Context, uri: Uri): String? {
    var name: String? = null
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0) {
                name = cursor.getString(nameIndex)
            }
        }
    }
    return name
}

/**
 * 下载 LifeRecorder 工作区文件到本地 Downloads 目录
 * @param relativePath 后端工作区相对路径
 * @return 下载后本地文件路径，或带错误信息的失败结果
 */
data class DownloadResult(
    val success: Boolean,
    val filePath: String? = null,
    val fileName: String? = null,
    val error: String? = null
)

fun downloadAgentFile(context: Context, baseUrl: String, relativePath: String): DownloadResult {
    // 1. 参数校验
    if (relativePath.isNullOrBlank() || relativePath.equals("null", ignoreCase = true)) {
        return DownloadResult(success = false, error = "下载失败：relativePath 为空")
    }

    var conn: HttpURLConnection? = null
    return try {
        // 2. URL encode
        val encodedPath = URLEncoder.encode(relativePath, Charsets.UTF_8.name())
        val url = URL("${baseUrl.removeSuffix("/")}/life/download-file?relativePath=$encodedPath")

        conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 30_000
        conn.readTimeout = 60_000

        // 3. 发请求
        val responseCode = conn.responseCode
        if (responseCode == 404) {
            return DownloadResult(success = false, error = "下载失败：文件不存在（404）")
        }
        if (responseCode == 400 || responseCode == 403) {
            return DownloadResult(success = false, error = "下载失败：路径非法或无权限（$responseCode）")
        }
        if (responseCode !in 200..299) {
            val errorBody = conn.errorStream?.bufferedReader(Charsets.UTF_8)?.readText()
            return DownloadResult(success = false, error = "下载失败：网络请求失败（HTTP $responseCode）${if (errorBody.isNullOrBlank()) "" else " $errorBody"}")
        }

        // 4. 读取内容
        val content = conn.inputStream.readBytes()
        if (content.isEmpty()) {
            return DownloadResult(success = false, error = "下载失败：文件内容为空")
        }

        // 5. 提取文件名
        val filename = relativePath.substringAfterLast('/')

        // 6. 保存到 App 私有 Download/LifeRecorder 目录
        val saveDir = java.io.File(context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS), "LifeRecorder")
        if (!saveDir.exists()) saveDir.mkdirs()
        val localFile = java.io.File(saveDir, filename)
        localFile.writeBytes(content)

        DownloadResult(
            success = true,
            filePath = localFile.absolutePath,
            fileName = filename
        )
    } catch (e: java.net.UnknownHostException) {
        DownloadResult(success = false, error = "下载失败：无法连接服务器（${e.message}）")
    } catch (e: java.net.SocketTimeoutException) {
        DownloadResult(success = false, error = "下载失败：连接超时")
    } catch (e: java.io.IOException) {
        DownloadResult(success = false, error = "下载失败：写入本地文件失败（${e.message}）")
    } catch (e: Exception) {
        DownloadResult(success = false, error = "下载失败：${e.message ?: e.javaClass.simpleName}")
    } finally {
        conn?.disconnect()
    }
}

fun uploadDailyContext(baseUrl: String, request: DailyContextRequest): String {
    var conn: HttpURLConnection? = null

    return try {
        val url = URL("${baseUrl.removeSuffix("/")}/life/daily-context")

        conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
        conn.doOutput = true
        conn.connectTimeout = 10000
        conn.readTimeout = 20000

        val json = JSONObject()
            .put("date", request.date)
            .put("timezone", request.timezone)
            .put("city", request.city ?: JSONObject.NULL)
            .put("country", request.country ?: JSONObject.NULL)
            .put("batteryPercent", request.batteryPercent ?: JSONObject.NULL)
            .put("isCharging", request.isCharging ?: JSONObject.NULL)
            .put("networkType", request.networkType ?: JSONObject.NULL)
            .put("deviceName", request.deviceName ?: JSONObject.NULL)
            .put("androidVersion", request.androidVersion ?: JSONObject.NULL)
            .put("appVersion", request.appVersion ?: JSONObject.NULL)
            .put("createdTime", request.createdTime)
            .put("updatedTime", request.updatedTime)
            .toString()

        OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
            writer.write(json)
        }

        val responseCode = conn.responseCode
        val responseText = if (responseCode in 200..299) {
            conn.inputStream.bufferedReader(Charsets.UTF_8).readText()
        } else {
            conn.errorStream?.bufferedReader(Charsets.UTF_8)?.readText()
                ?: "服务器错误：HTTP $responseCode"
        }

        if (responseCode !in 200..299) {
            return "上传今日上下文失败：HTTP $responseCode，$responseText"
        }

        val message = try {
            JSONObject(responseText).optString("message", "")
        } catch (e: Exception) {
            ""
        }

        message.ifBlank { responseText.ifBlank { "今日上下文上传成功" } }
    } catch (e: java.net.SocketTimeoutException) {
        "上传今日上下文超时，请检查后端服务和网络：${e.message ?: "timeout"}"
    } catch (e: Exception) {
        "上传今日上下文失败，请检查服务器地址、Cloudflare Tunnel 和后端服务：${e.message ?: e.javaClass.simpleName}"
    } finally {
        conn?.disconnect()
    }
}

fun uploadPhoneFileIndex(baseUrl: String, request: PhoneFileIndexRequest): String {
    var conn: HttpURLConnection? = null

    return try {
        val url = URL("${baseUrl.removeSuffix("/")}/phone-sync/upload-file-index")

        conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
        conn.doOutput = true
        conn.connectTimeout = 10_000
        conn.readTimeout = 20_000

        val filesJson = JSONArray()
        request.files.forEach { file ->
            filesJson.put(
                JSONObject()
                    .put("fileId", file.fileId)
                    .put("name", file.name)
                    .put("relativePhonePath", file.relativePhonePath)
                    .put("mimeType", file.mimeType ?: JSONObject.NULL)
                    .put("size", file.size)
                    .put("lastModified", file.lastModified)
                    .put("contentHash", file.contentHash)
                    .put("availableLocally", file.availableLocally)
                    .put("cachedPath", file.cachedPath ?: JSONObject.NULL)
            )
        }

        val json = JSONObject()
            .put("schemaVersion", request.schemaVersion)
            .put("deviceId", request.deviceId)
            .put("updatedTime", request.updatedTime)
            .put("files", filesJson)
            .toString()

        OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
            writer.write(json)
        }

        val responseCode = conn.responseCode
        val responseText = if (responseCode in 200..299) {
            conn.inputStream.bufferedReader(Charsets.UTF_8).readText()
        } else {
            conn.errorStream?.bufferedReader(Charsets.UTF_8)?.readText()
                ?: "服务器错误：HTTP $responseCode"
        }

        if (responseCode !in 200..299) {
            return "HTTP $responseCode，$responseText"
        }

        val message = try {
            JSONObject(responseText).optString("message", "")
        } catch (e: Exception) {
            ""
        }

        message.ifBlank { responseText.ifBlank { "文件索引已同步" } }
    } catch (e: java.net.SocketTimeoutException) {
        "文件索引同步超时：${e.message ?: "timeout"}"
    } catch (e: Exception) {
        "文件索引同步失败：${e.message ?: e.javaClass.simpleName}"
    } finally {
        conn?.disconnect()
    }
}

fun uploadAppFilesIndex(baseUrl: String, request: AppFileIndexRequest): String {
    var conn: HttpURLConnection? = null

    return try {
        val url = URL("${baseUrl.removeSuffix("/")}/phone-sync/upload-app-files-index")

        conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
        conn.doOutput = true
        conn.connectTimeout = 10_000
        conn.readTimeout = 20_000

        val filesJson = JSONArray()
        request.files.forEach { file ->
            filesJson.put(
                JSONObject()
                    .put("fileId", file.fileId)
                    .put("name", file.name)
                    .put("virtualPath", file.virtualPath)
                    .put("source", file.source)
                    .put("mimeType", file.mimeType ?: JSONObject.NULL)
                    .put("size", file.size)
                    .put("lastModified", file.lastModified)
                    .put("contentHash", file.contentHash)
                    .put("availableLocally", file.availableLocally)
                    .put("cachedPath", file.cachedPath ?: JSONObject.NULL)
                    .put("linkedPhoneFileId", file.linkedPhoneFileId)
            )
        }

        val json = JSONObject()
            .put("schemaVersion", request.schemaVersion)
            .put("deviceId", request.deviceId)
            .put("updatedTime", request.updatedTime)
            .put("files", filesJson)
            .toString()

        OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
            writer.write(json)
        }

        val responseCode = conn.responseCode
        val responseText = if (responseCode in 200..299) {
            conn.inputStream.bufferedReader(Charsets.UTF_8).readText()
        } else {
            conn.errorStream?.bufferedReader(Charsets.UTF_8)?.readText()
                ?: "服务器错误：HTTP $responseCode"
        }

        if (responseCode !in 200..299) {
            return "HTTP $responseCode，$responseText"
        }

        val message = try {
            JSONObject(responseText).optString("message", "")
        } catch (e: Exception) {
            ""
        }

        message.ifBlank { responseText.ifBlank { "App 文件索引已同步" } }
    } catch (e: java.net.SocketTimeoutException) {
        "App 文件索引同步超时：${e.message ?: "timeout"}"
    } catch (e: Exception) {
        "App 文件索引同步失败：${e.message ?: e.javaClass.simpleName}"
    } finally {
        conn?.disconnect()
    }
}

fun getAppFilesIndex(baseUrl: String): AppFilesIndexResponse {
    var conn: HttpURLConnection? = null

    return try {
        val url = URL("${baseUrl.removeSuffix("/")}/phone-sync/app-files-index")

        conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 10_000
        conn.readTimeout = 20_000

        val responseCode = conn.responseCode
        val responseText = if (responseCode in 200..299) {
            conn.inputStream.bufferedReader(Charsets.UTF_8).readText()
        } else {
            val errorText = conn.errorStream?.bufferedReader(Charsets.UTF_8)?.readText()
            throw IllegalStateException("HTTP $responseCode${if (errorText.isNullOrBlank()) "" else "，$errorText"}")
        }

        val root = JSONObject(responseText)
        val filesJson = root.optJSONArray("files") ?: JSONArray()
        val files = mutableListOf<AppFileItem>()

        for (index in 0 until filesJson.length()) {
            val item = filesJson.optJSONObject(index) ?: continue
            files.add(
                AppFileItem(
                    fileId = item.optString("fileId"),
                    name = item.optString("name"),
                    virtualPath = item.optString("virtualPath"),
                    source = item.optString("source"),
                    mimeType = item.optString("mimeType").ifBlank { null },
                    size = item.optLong("size", 0L),
                    lastModified = item.optLong("lastModified", 0L),
                    contentHash = item.optString("contentHash"),
                    availableLocally = item.optBoolean("availableLocally", false),
                    cachedPath = item.optString("cachedPath").ifBlank { null },
                    linkedPhoneFileId = item.optString("linkedPhoneFileId").ifBlank { null }
                )
            )
        }

        AppFilesIndexResponse(
            schemaVersion = root.optInt("schemaVersion", 1),
            deviceId = root.optString("deviceId", "android_main"),
            updatedTime = root.optLong("updatedTime", 0L),
            files = files
        )
    } finally {
        conn?.disconnect()
    }
}

fun getAgentGeneratedFiles(baseUrl: String): List<AgentGeneratedFileItem> {
    var conn: HttpURLConnection? = null

    return try {
        val url = URL("${baseUrl.removeSuffix("/")}/life/agent-generated-files")

        conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 10_000
        conn.readTimeout = 20_000

        val responseCode = conn.responseCode
        val responseText = if (responseCode in 200..299) {
            conn.inputStream.bufferedReader(Charsets.UTF_8).readText()
        } else {
            val errorText = conn.errorStream?.bufferedReader(Charsets.UTF_8)?.readText()
            throw IllegalStateException(
                "HTTP $responseCode，${summarizeNetworkBody(errorText.orEmpty())}"
            )
        }

        parseAgentGeneratedFiles(responseText)
    } finally {
        conn?.disconnect()
    }
}

fun getAgentGeneratedFileContent(baseUrl: String, relativePath: String): String {
    var conn: HttpURLConnection? = null

    return try {
        val encodedPath = URLEncoder.encode(relativePath, Charsets.UTF_8.name())
        val url = URL("${baseUrl.removeSuffix("/")}/life/agent-generated-file?relativePath=$encodedPath")

        conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 10_000
        conn.readTimeout = 30_000

        val responseCode = conn.responseCode
        val responseText = if (responseCode in 200..299) {
            conn.inputStream.bufferedReader(Charsets.UTF_8).readText()
        } else {
            val errorText = conn.errorStream?.bufferedReader(Charsets.UTF_8)?.readText()
            throw IllegalStateException(
                "HTTP $responseCode，${summarizeNetworkBody(errorText.orEmpty())}"
            )
        }

        parseAgentGeneratedFileContent(responseText)
    } finally {
        conn?.disconnect()
    }
}

fun fetchPendingPhoneFileRequests(baseUrl: String): List<PendingPhoneFileRequest> {
    var conn: HttpURLConnection? = null

    return try {
        val url = URL("${baseUrl.removeSuffix("/")}/phone-sync/pending-requests")

        conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 10_000
        conn.readTimeout = 20_000

        val responseCode = conn.responseCode
        val responseText = if (responseCode in 200..299) {
            conn.inputStream.bufferedReader(Charsets.UTF_8).readText()
        } else {
            val errorText = conn.errorStream?.bufferedReader(Charsets.UTF_8)?.readText()
            throw IllegalStateException("HTTP $responseCode${if (errorText.isNullOrBlank()) "" else "，$errorText"}")
        }

        parsePendingPhoneFileRequests(responseText)
    } finally {
        conn?.disconnect()
    }
}

fun uploadRequestedPhoneFile(
    baseUrl: String,
    context: Context,
    request: PendingPhoneFileRequest,
    uri: Uri
): String {
    var conn: HttpURLConnection? = null

    return try {
        val boundary = "LifeRecorderBoundary${System.currentTimeMillis()}"
        val url = URL("${baseUrl.removeSuffix("/")}/phone-sync/upload-requested-file")

        conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
        conn.doOutput = true
        conn.connectTimeout = 15_000
        conn.readTimeout = 120_000

        val fileName = readUploadFileName(context, uri)
        val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw SecurityException("无法读取文件，请重新选择文件")

        DataOutputStream(conn.outputStream).use { output ->
            writeMultipartText(output, boundary, "requestId", request.id)
            writeMultipartText(output, boundary, "deviceId", request.deviceId.ifBlank { "android_main" })
            writeMultipartText(output, boundary, "fileId", request.fileId)

            output.writeBytes("--$boundary\r\n")
            output.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"${escapeMultipartFileName(fileName)}\"\r\n")
            output.writeBytes("Content-Type: $mimeType\r\n")
            output.writeBytes("\r\n")
            inputStream.use { input ->
                input.copyTo(output)
            }
            output.writeBytes("\r\n")
            output.writeBytes("--$boundary--\r\n")
            output.flush()
        }

        val responseCode = conn.responseCode
        val responseText = if (responseCode in 200..299) {
            conn.inputStream.bufferedReader(Charsets.UTF_8).readText()
        } else {
            conn.errorStream?.bufferedReader(Charsets.UTF_8)?.readText()
                ?: "服务器错误：HTTP $responseCode"
        }

        if (responseCode !in 200..299) {
            return "HTTP $responseCode，$responseText"
        }

        val message = try {
            JSONObject(responseText).optString("message", "")
        } catch (e: Exception) {
            ""
        }

        message.ifBlank { responseText.ifBlank { "已上传 Agent 请求的文件" } }
    } catch (e: SecurityException) {
        "找不到该文件的本地授权，请重新选择文件"
    } catch (e: java.net.SocketTimeoutException) {
        "上传 Agent 请求的文件超时：${e.message ?: "timeout"}"
    } catch (e: Exception) {
        "上传 Agent 请求的文件失败：${e.message ?: e.javaClass.simpleName}"
    } finally {
        conn?.disconnect()
    }
}

private fun parsePendingPhoneFileRequests(responseText: String): List<PendingPhoneFileRequest> {
    val trimmed = responseText.trim()
    val array = if (trimmed.startsWith("[")) {
        JSONArray(trimmed)
    } else {
        val root = JSONObject(trimmed)
        root.optJSONArray("requests")
            ?: root.optJSONArray("pendingRequests")
            ?: root.optJSONArray("data")
            ?: JSONArray()
    }

    val result = mutableListOf<PendingPhoneFileRequest>()
    for (index in 0 until array.length()) {
        val item = array.optJSONObject(index) ?: continue
        val payload = item.optJSONObject("request")
        val type = item.optString("type", payload?.optString("type").orEmpty())
        val status = item.optString("status", payload?.optString("status").orEmpty())
        val fileId = item.optString("fileId", payload?.optString("fileId").orEmpty())

        if (type == "fetch_phone_file" && status == "pending" && fileId.isNotBlank()) {
            result.add(
                PendingPhoneFileRequest(
                    id = item.optString("id", payload?.optString("id").orEmpty()),
                    type = type,
                    status = status,
                    deviceId = item.optString(
                        "deviceId",
                        payload?.optString("deviceId").orEmpty()
                    ),
                    fileId = fileId
                )
            )
        }
    }

    return result
}

private fun parseAgentGeneratedFiles(responseText: String): List<AgentGeneratedFileItem> {
    val trimmed = responseText.trim()
    val array = if (trimmed.startsWith("[")) {
        JSONArray(trimmed)
    } else {
        val root = JSONObject(trimmed)
        root.optJSONArray("files")
            ?: root.optJSONArray("items")
            ?: root.optJSONArray("data")
            ?: JSONArray()
    }

    val result = mutableListOf<AgentGeneratedFileItem>()
    for (index in 0 until array.length()) {
        val item = array.optJSONObject(index) ?: continue
        val relativePath = item.optString(
            "relativePath",
            item.optString("path", item.optString("filePath"))
        )

        if (relativePath.isBlank()) {
            continue
        }

        result.add(
            AgentGeneratedFileItem(
                name = item.optString("name", relativePath.substringAfterLast('/')),
                preview = item.optString("preview"),
                relativePath = relativePath,
                size = item.optLong("size", item.optLong("sizeBytes", 0L)),
                createdTime = item.optLong("createdTime", 0L),
                updatedTime = item.optLong("updatedTime", 0L)
            )
        )
    }

    return result
}

private fun parseAgentGeneratedFileContent(responseText: String): String {
    val trimmed = responseText.trim()
    if (!trimmed.startsWith("{")) {
        return responseText
    }

    return runCatching {
        val root = JSONObject(trimmed)
        root.optString("content")
            .ifBlank { root.optString("markdown") }
            .ifBlank { root.optString("text") }
            .ifBlank { responseText }
    }.getOrDefault(responseText)
}

private fun writeMultipartText(
    output: DataOutputStream,
    boundary: String,
    name: String,
    value: String
) {
    output.writeBytes("--$boundary\r\n")
    output.writeBytes("Content-Disposition: form-data; name=\"$name\"\r\n")
    output.writeBytes("\r\n")
    output.write(value.toByteArray(Charsets.UTF_8))
    output.writeBytes("\r\n")
}

private fun readUploadFileName(context: Context, uri: Uri): String {
    context.contentResolver.query(
        uri,
        arrayOf(OpenableColumns.DISPLAY_NAME),
        null,
        null,
        null
    )?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (nameIndex >= 0 && cursor.moveToFirst()) {
            val name = cursor.getString(nameIndex)
            if (!name.isNullOrBlank()) {
                return name
            }
        }
    }

    return DocumentFile.fromSingleUri(context, uri)?.name ?: "requested_file"
}

private fun escapeMultipartFileName(fileName: String): String {
    return fileName
        .replace("\\", "_")
        .replace("\"", "_")
        .replace("\r", "_")
        .replace("\n", "_")
}

fun escapeJson(text: String): String {
    return text
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
}
