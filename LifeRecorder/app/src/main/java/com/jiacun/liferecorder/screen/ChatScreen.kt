package com.jiacun.liferecorder.screen

/**
 * ChatScreen
 *
 * 负责：
 * - 显示 AI/Agent 对话页面。
 * - 展示用户消息、AI 回复、输入框和发送按钮。
 * - 支持选择并上传附件（图片/文件），随消息发送给后端 Agent。
 * - 支持下载 Agent 生成的文件到本地 Downloads 目录。
 *
 * 不负责：
 * - 不直接保存 MiMo API Key。
 * - 不直接调用模型官方接口。
 * - 不实现 Agent 工具、文件同步或后端业务逻辑。
 *
 * 数据来源：
 * - 服务器地址来自 ServerConfig。
 * - 网络请求通过 data/Network.kt 的 sendAiChatMessage 统一发起。
 */

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jiacun.liferecorder.component.MarkdownMessage
import com.jiacun.liferecorder.data.GeneratedFileInfo
import com.jiacun.liferecorder.data.UploadedAttachment
import com.jiacun.liferecorder.data.downloadAgentFile
import com.jiacun.liferecorder.data.getAgentGeneratedFileContent
import com.jiacun.liferecorder.data.getServerBaseUrl
import com.jiacun.liferecorder.data.sendAiChatMessage
import com.jiacun.liferecorder.data.uploadAttachment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLDecoder

@Composable
fun ChatScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var inputText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val messages = remember { mutableStateListOf<ChatMessage>() }

    // 待发送的附件（上传后暂存）
    val pendingAttachments = remember { mutableStateListOf<PendingAttachment>() }

    // 图片选择器
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                scope.launch {
                    uploadAndAddAttachment(
                        context = context,
                        uri = uri,
                        type = "image",
                        pendingAttachments = pendingAttachments
                    )
                }
            }
        }
    }

    // 文件选择器
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                scope.launch {
                    uploadAndAddAttachment(
                        context = context,
                        uri = uri,
                        type = "file",
                        pendingAttachments = pendingAttachments
                    )
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF2F2F7))
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
            .padding(top = 12.dp)
    ) {
        // 标题栏
        Text(
            text = "AI",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1C1C1E)
        )

        // 对话列表
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages) { message ->
                ChatBubble(message = message)
            }
        }

        // 待发送附件预览
        if (pendingAttachments.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                pendingAttachments.forEach { att ->
                    AttachmentChip(
                        name = att.name,
                        isUploading = att.isUploading,
                        onRemove = { pendingAttachments.remove(att) }
                    )
                }
            }
        }

        // 输入行
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 附件按钮
            IconButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "*/*"
                        putExtra(Intent.EXTRA_MIME_TYPES, arrayOf(
                            "image/*",
                            "application/pdf",
                            "application/msword",
                            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                            "text/plain",
                            "text/markdown",
                            "application/json",
                            "text/csv"
                        ))
                    }
                    filePickerLauncher.launch(intent)
                },
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AttachFile,
                    contentDescription = "添加附件",
                    tint = Color(0xFF5F6368)
                )
            }

            // 文字输入框
            BasicTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .background(Color.White, RoundedCornerShape(22.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                textStyle = TextStyle(fontSize = 15.sp, color = Color(0xFF1C1C1E)),
                cursorBrush = SolidColor(Color(0xFF1C1C1E)),
                singleLine = true,
                decorationBox = { innerTextField ->
                    Box {
                        if (inputText.isEmpty()) {
                            Text(
                                text = "输入消息",
                                fontSize = 15.sp,
                                color = Color(0xFFAAAAAA)
                            )
                        }
                        innerTextField()
                    }
                }
            )

            // 发送按钮
            Button(
                onClick = {
                    val rawMessage = inputText.trim()
                    val messageText = decodePercentEncodedMessage(rawMessage)
                    if (messageText.isNotEmpty() || pendingAttachments.isNotEmpty()) {
                        val currentAttachments = pendingAttachments
                            .filter { !it.isUploading && it.uploadedAttachment != null }
                            .mapNotNull { it.uploadedAttachment }

                        messages.add(ChatMessage(text = messageText, fromUser = true,
                            attachments = currentAttachments.map {
                                AttachmentDisplayInfo(it.name, it.mimeType ?: "")
                            }))

                        val thinkingIndex = messages.size
                        messages.add(ChatMessage(
                            text = "正在思考...",
                            fromUser = false
                        ))

                        inputText = ""
                        val attachmentsToSend = currentAttachments.toList()
                        pendingAttachments.clear()
                        isLoading = true

                        scope.launch {
                            val baseUrl = getServerBaseUrl(context)
                            val reply = withContext(Dispatchers.IO) {
                                sendAiChatMessage(
                                    baseUrl = baseUrl,
                                    message = messageText,
                                    attachments = attachmentsToSend
                                )
                            }
                            isLoading = false

                            // 前端 JSON 兜底：不直接显示原始 JSON
                            val hasGeneratedFile = !reply.generatedPath.isNullOrBlank() ||
                                !reply.generatedFiles.isNullOrEmpty()
                            val displayReply = sanitizeReply(
                                reply = reply.reply,
                                hasGeneratedFile = hasGeneratedFile
                            )

                            if (thinkingIndex < messages.size) {
                                messages[thinkingIndex] = ChatMessage(
                                    text = displayReply,
                                    fromUser = false,
                                    mode = reply.mode,
                                    taskType = reply.taskType,
                                    generatedPath = reply.generatedPath,
                                    generatedFiles = reply.generatedFiles
                                )
                            }
                        }
                    }
                },
                modifier = Modifier.padding(start = 8.dp),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("发送")
                }
            }
        }
    }
}

/**
 * 上传附件并添加到 pending 列表
 */
private suspend fun uploadAndAddAttachment(
    context: android.content.Context,
    uri: Uri,
    type: String,
    pendingAttachments: MutableList<PendingAttachment>
) {
    val name = getAttachmentName(context, uri) ?: "附件"
    val pending = PendingAttachment(name = name, isUploading = true)
    pendingAttachments.add(pending)

    val baseUrl = getServerBaseUrl(context)
    val uploaded = withContext(Dispatchers.IO) {
        uploadAttachment(context, baseUrl, uri, type)
    }

    val index = pendingAttachments.indexOf(pending)
    if (index >= 0) {
        if (uploaded != null) {
            pendingAttachments[index] = pending.copy(
                isUploading = false,
                uploadedAttachment = uploaded
            )
        } else {
            pendingAttachments.removeAt(index)
            Toast.makeText(context, "附件上传失败：$name", Toast.LENGTH_SHORT).show()
        }
    }
}

private fun getAttachmentName(context: android.content.Context, uri: Uri): String? {
    var name: String? = null
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (idx >= 0) name = cursor.getString(idx)
        }
    }
    return name
}

/**
 * 待发送附件状态
 */
private data class PendingAttachment(
    val name: String,
    val isUploading: Boolean,
    val uploadedAttachment: UploadedAttachment? = null
)

/**
 * 附件预览标签
 */
@Composable
private fun AttachmentChip(name: String, isUploading: Boolean, onRemove: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFE8F0FE)
    ) {
        Row(
            modifier = Modifier.padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isUploading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                    color = Color(0xFF4285F4)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.AttachFile,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = Color(0xFF4285F4)
                )
            }
            Text(
                text = name,
                fontSize = 12.sp,
                color = Color(0xFF1C1C1E),
                modifier = Modifier.padding(horizontal = 6.dp)
            )
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(20.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "移除",
                    modifier = Modifier.size(12.dp),
                    tint = Color(0xFF5F6368)
                )
            }
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var generatedContent by remember(message.generatedPath) { mutableStateOf("") }
    var isDownloading by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.fromUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            modifier = if (message.fromUser) Modifier else Modifier.fillMaxWidth(0.94f),
            shape = RoundedCornerShape(18.dp),
            color = if (message.fromUser) Color(0xFF1C1C1E) else Color.White
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                // 用户发送的附件标签
                if (message.fromUser && message.attachments.isNotEmpty()) {
                    message.attachments.forEach { att ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AttachFile,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = Color(0xFFAAAAAA)
                            )
                            Text(
                                text = att.name,
                                fontSize = 11.sp,
                                color = Color(0xFFAAAAAA),
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }
                }

                Text(
                    text = message.text,
                    color = if (message.fromUser) Color.White else Color(0xFF1C1C1E),
                    fontSize = 15.sp
                )

                // AI 回复：模式标签 + Markdown + 生成文件卡片
                if (!message.fromUser) {
                    val modeLabel = modeLabelForMessage(message)
                    if (modeLabel != null) {
                        ModeLabel(text = modeLabel, modifier = Modifier.padding(top = 8.dp))
                    }

                    if (message.text.isNotBlank()) {
                        MarkdownMessage(
                            text = message.text,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    // 生成文件列表（优先用 generatedFiles）
                    val filesToShow = message.generatedFiles
                    if (!filesToShow.isNullOrEmpty()) {
                        filesToShow.forEach { file ->
                            GeneratedFileCard(
                                file = file,
                                generatedContent = generatedContent,
                                isDownloading = isDownloading,
                                onPreview = {
                                    generatedContent = "正在读取 ${file.name}..."
                                    scope.launch {
                                        generatedContent = try {
                                            withContext(Dispatchers.IO) {
                                                getAgentGeneratedFileContent(
                                                    baseUrl = getServerBaseUrl(context),
                                                    relativePath = file.relativePath
                                                )
                                            }
                                        } catch (e: Exception) {
                                            "读取失败：${e.message ?: e.javaClass.simpleName}"
                                        }
                                    }
                                },
                                onDownload = { doDownload(context, scope, file.relativePath, { isDownloading = it }) }
                            )
                        }
                    } else {
                        // 兼容旧版 generatedPath
                        val generatedPath = message.generatedPath
                        val isValidPath = !generatedPath.isNullOrBlank() &&
                            !generatedPath.equals("null", ignoreCase = true) &&
                            !generatedPath.equals("NULL", ignoreCase = true)
                        if (isValidPath) {
                            GeneratedFileCard(
                                file = GeneratedFileInfo(
                                    name = generatedPath.substringAfterLast('/'),
                                    relativePath = generatedPath,
                                    mimeType = null,
                                    size = null,
                                    preview = null
                                ),
                                generatedContent = generatedContent,
                                isDownloading = isDownloading,
                                onPreview = {
                                    generatedContent = "正在读取 ${generatedPath.substringAfterLast('/')}..."
                                    scope.launch {
                                        generatedContent = try {
                                            withContext(Dispatchers.IO) {
                                                getAgentGeneratedFileContent(
                                                    baseUrl = getServerBaseUrl(context),
                                                    relativePath = generatedPath
                                                )
                                            }
                                        } catch (e: Exception) {
                                            "读取失败：${e.message ?: e.javaClass.simpleName}"
                                        }
                                    }
                                },
                                onDownload = { doDownload(context, scope, generatedPath, { isDownloading = it }) }
                            )
                        }
                    }

                    // 预览内容
                    if (generatedContent.isNotBlank()) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF8F8F8)
                        ) {
                            MarkdownMessage(
                                text = generatedContent,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class ChatMessage(
    val text: String,
    val fromUser: Boolean,
    val mode: String? = null,
    val taskType: String? = null,
    val generatedPath: String? = null,
    val generatedFiles: List<GeneratedFileInfo>? = null,
    val attachments: List<AttachmentDisplayInfo> = emptyList()
)

private data class AttachmentDisplayInfo(
    val name: String,
    val mimeType: String
)

@Composable
private fun GeneratedFileCard(
    file: GeneratedFileInfo,
    generatedContent: String,
    isDownloading: Boolean,
    onPreview: () -> Unit,
    onDownload: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .clickable(onClick = onPreview),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFF2F3F5)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📄 ${file.name}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF3A3A3C),
                    modifier = Modifier.weight(1f)
                )
                Row {
                    Text(
                        text = if (isDownloading) "下载中..." else "下载",
                        fontSize = 12.sp,
                        color = Color(0xFF4285F4),
                        modifier = Modifier
                            .clickable(enabled = !isDownloading) { onDownload() }
                            .padding(start = 8.dp)
                    )
                }
            }
            if (file.size != null) {
                Text(
                    text = formatFileSize(file.size),
                    fontSize = 11.sp,
                    color = Color(0xFF9E9E9E),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            if (!file.preview.isNullOrBlank()) {
                Text(
                    text = file.preview,
                    fontSize = 11.sp,
                    color = Color(0xFF757575),
                    modifier = Modifier.padding(top = 4.dp),
                    maxLines = 2
                )
            }
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
    }
}

@Composable
private fun ModeLabel(text: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = Color(0xFFF1F2F4)
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF5F6368),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

private fun modeLabelForMessage(message: ChatMessage): String? {
    val mode = message.mode?.lowercase()
    val baseLabel = when (mode) {
        "ai" -> "AI"
        "agent" -> "Agent"
        else -> return null
    }
    return if (mode == "agent" && !message.taskType.isNullOrBlank()) {
        "$baseLabel · ${message.taskType}"
    } else {
        baseLabel
    }
}

/**
 * 下载 Agent 生成文件到 App 私有目录
 */
private fun doDownload(
    context: Context,
    scope: kotlinx.coroutines.CoroutineScope,
    relativePath: String,
    onLoadingChange: (Boolean) -> Unit
) {
    val displayName = relativePath.substringAfterLast('/')
    android.util.Log.d("Download", "displayName=$displayName")
    android.util.Log.d("Download", "relativePath=$relativePath")
    onLoadingChange(true)
    scope.launch {
        val msg = try {
            val result = withContext(Dispatchers.IO) {
                downloadAgentFile(context, getServerBaseUrl(context), relativePath)
            }

            if (result.success) {
                "已保存到 LifeRecorder 下载目录：${result.fileName}"
            } else {
                result.error ?: "下载失败"
            }
        } catch (e: Exception) {
            "下载失败：${e.message ?: e.javaClass.simpleName}"
        } finally {
            onLoadingChange(false)
        }

        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
    }
}

/**
 * JSON 兜底：不直接显示原始 JSON
 */
private fun sanitizeReply(
    reply: String,
    hasGeneratedFile: Boolean
): String {
    if (reply.isBlank()) {
        return if (hasGeneratedFile) generatedFileFallbackReply() else reply
    }

    val trimmed = reply.trim()

    if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
        val extractedText = extractReadableReplyFromJson(trimmed)
        if (!extractedText.isNullOrBlank() && !looksLikeDebugJson(extractedText)) {
            return extractedText
        }

        if (hasGeneratedFile) {
            return generatedFileFallbackReply()
        }

        if (looksLikeDebugJson(trimmed)) {
            return "Agent 返回了调试数据，但没有解析出可展示文本。"
        }
    }

    return reply
}

private fun generatedFileFallbackReply(): String {
    return "文件已生成，可以点击下方卡片下载或查看。"
}

private fun looksLikeDebugJson(text: String): Boolean {
    val trimmed = text.trim()
    return (trimmed.startsWith("{") || trimmed.startsWith("[")) &&
        (trimmed.contains("\"completion\"") ||
            trimmed.contains("\"payloads\"") ||
            trimmed.contains("\"transport\"") ||
            trimmed.contains("\"backFrom\""))
}

private fun extractReadableReplyFromJson(text: String): String? {
    return runCatching {
        val trimmed = text.trim()
        if (trimmed.startsWith("{")) {
            extractReadableReplyFromObject(JSONObject(trimmed))
        } else {
            extractReadableReplyFromArray(JSONArray(trimmed))
        }
    }.getOrNull()
}

private fun extractReadableReplyFromObject(json: JSONObject): String? {
    json.optJSONObject("result")
        ?.optJSONArray("payloads")
        ?.let { payloads ->
            findLastReadablePayloadText(payloads)?.let { return it }
        }

    json.optJSONArray("payloads")
        ?.let { payloads ->
            findLastReadablePayloadText(payloads)?.let { return it }
        }

    json.optJSONObject("message")
        ?.optString("content")
        ?.takeIf { it.isNotBlank() }
        ?.let { return it }

    listOf("content", "text", "reply").forEach { key ->
        json.optString(key)
            .takeIf { it.isNotBlank() }
            ?.let { return it }
    }

    return null
}

private fun extractReadableReplyFromArray(array: JSONArray): String? {
    for (index in array.length() - 1 downTo 0) {
        val item = array.optJSONObject(index) ?: continue
        extractReadableReplyFromObject(item)
            ?.takeIf { it.isNotBlank() && !looksLikeDebugJson(it) }
            ?.let { return it }
    }

    return null
}

private fun findLastReadablePayloadText(payloads: JSONArray): String? {
    for (index in payloads.length() - 1 downTo 0) {
        val text = payloads.optJSONObject(index)
            ?.optString("text")
            ?.trim()
            .orEmpty()

        if (text.isNotBlank() && !looksLikeDebugJson(text)) {
            return text
        }
    }

    return null
}

private fun decodePercentEncodedMessage(text: String): String {
    if (!Regex("%[0-9A-Fa-f]{2}").containsMatchIn(text)) {
        return text
    }

    return runCatching {
        URLDecoder.decode(text, Charsets.UTF_8.name())
    }.getOrDefault(text)
}
