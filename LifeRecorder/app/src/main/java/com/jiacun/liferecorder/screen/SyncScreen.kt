package com.jiacun.liferecorder.screen

/**
 * SyncScreen
 *
 * 负责：
 * - 显示同步与设置页面。
 * - 管理服务器地址输入、连接测试、笔记上传、今日总结读取。
 * - 提供 Daily Context 上传和 phone_sync 手动测试入口。
 * - 展示 LifeRecorder App 虚拟文件列表和同步状态。
 *
 * 不负责：
 * - 不实现后端接口本身。
 * - 不处理 AI 聊天发送逻辑。
 * - 不扫描全手机或做后台自动同步。
 * - 不修改笔记保存、相册读取或底部栏导航。
 *
 * 数据来源：
 * - 服务器地址来自 ServerConfig。
 * - 网络请求通过 data/Network.kt 发起。
 * - 文件索引元数据和 fileId 到 Uri 的映射由 data/PhoneSyncStorage.kt 管理。
 */

import android.widget.Toast
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jiacun.liferecorder.data.AgentGeneratedFileItem
import com.jiacun.liferecorder.data.AppFileItem
import com.jiacun.liferecorder.data.buildAppFileIndexRequest
import com.jiacun.liferecorder.data.buildDailyContextRequest
import com.jiacun.liferecorder.data.buildPhoneFileIndexRequest
import com.jiacun.liferecorder.data.collectDeviceContextSnapshot
import com.jiacun.liferecorder.data.fetchPendingPhoneFileRequests
import com.jiacun.liferecorder.data.getAgentGeneratedFileContent
import com.jiacun.liferecorder.data.getAgentGeneratedFiles
import com.jiacun.liferecorder.data.getDailyContextCity
import com.jiacun.liferecorder.data.getDailyContextCountry
import com.jiacun.liferecorder.data.getAppFilesIndex
import com.jiacun.liferecorder.data.getPhoneFileUriMapping
import com.jiacun.liferecorder.data.getServerBaseUrl
import com.jiacun.liferecorder.data.readTodaySummary
import com.jiacun.liferecorder.data.saveDailyContextLocation
import com.jiacun.liferecorder.data.savePhoneFileUriMapping
import com.jiacun.liferecorder.data.saveServerBaseUrl
import com.jiacun.liferecorder.data.testPublicPing
import com.jiacun.liferecorder.data.uploadAppFilesIndex
import com.jiacun.liferecorder.data.uploadCurrentNote
import com.jiacun.liferecorder.data.uploadDailyContext
import com.jiacun.liferecorder.data.uploadPhoneFileIndex
import com.jiacun.liferecorder.data.uploadRequestedPhoneFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SyncScreen(
    currentTitle: String,
    currentContent: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var serverUrl by remember {
        mutableStateOf(getServerBaseUrl(context))
    }
    var saveResult by remember {
        mutableStateOf("")
    }
    var connectionStatus by remember {
        mutableStateOf("未测试")
    }
    var pingResult by remember {
        mutableStateOf("还没有测试连接")
    }
    var uploadResult by remember {
        mutableStateOf("还没有上传")
    }
    var summaryText by remember {
        mutableStateOf("还没有读取总结")
    }
    var city by remember {
        mutableStateOf(getDailyContextCity(context))
    }
    var country by remember {
        mutableStateOf(getDailyContextCountry(context))
    }
    var deviceSnapshot by remember {
        mutableStateOf(collectDeviceContextSnapshot(context))
    }
    var dailyContextResult by remember {
        mutableStateOf("还没有上传今日上下文")
    }
    var phoneSyncResult by remember {
        mutableStateOf("还没有同步文件索引")
    }
    var pendingRequestResult by remember {
        mutableStateOf("还没有检查 Agent 文件请求")
    }
    var appFilesResult by remember {
        mutableStateOf("还没有刷新文件列表")
    }
    var appFiles by remember {
        mutableStateOf(emptyList<AppFileItem>())
    }
    var agentGeneratedResult by remember {
        mutableStateOf("还没有刷新 Agent 生成结果")
    }
    var agentGeneratedFiles by remember {
        mutableStateOf(emptyList<AgentGeneratedFileItem>())
    }
    var selectedAgentGeneratedName by remember {
        mutableStateOf("")
    }
    var selectedAgentGeneratedContent by remember {
        mutableStateOf("")
    }
    val fileIndexLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) {
            return@rememberLauncherForActivityResult
        }

        phoneSyncResult = "正在同步文件索引..."
        scope.launch {
            try {
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (e: SecurityException) {
                    phoneSyncResult = "文件已选择，但持久读取授权保存失败：${e.message}"
                    Toast.makeText(context, "文件授权保存失败，请重新选择文件", Toast.LENGTH_LONG).show()
                    return@launch
                }

                val phoneRequest = buildPhoneFileIndexRequest(context, uri)
                val appRequest = buildAppFileIndexRequest(phoneRequest)
                phoneRequest.files.firstOrNull()?.let { file ->
                    savePhoneFileUriMapping(context, file.fileId, uri)
                }

                val baseUrl = getServerBaseUrl(context)
                val results = withContext(Dispatchers.IO) {
                    val phoneResult = uploadPhoneFileIndex(
                        baseUrl = baseUrl,
                        request = phoneRequest
                    )
                    val appResult = uploadAppFilesIndex(
                        baseUrl = baseUrl,
                        request = appRequest
                    )
                    phoneResult to appResult
                }

                val phoneResult = results.first
                val appResult = results.second
                phoneSyncResult = "phone: $phoneResult\napp: $appResult"

                if (appResult.contains("失败") || appResult.contains("超时") || appResult.startsWith("HTTP")) {
                    Toast.makeText(context, "文件索引同步失败：$appResult", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "App 文件索引已同步", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                val errorMessage = e.message ?: e.javaClass.simpleName
                phoneSyncResult = "文件索引同步失败：$errorMessage"
                Toast.makeText(context, "文件索引同步失败：$errorMessage", Toast.LENGTH_LONG).show()
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF2F2F7))
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SettingsCard(
                title = "服务器地址",
                description = "Android App 只连接你的 Spring Boot 后端。"
            ) {
                OutlinedTextField(
                    value = serverUrl,
                    onValueChange = {
                        serverUrl = it
                        saveResult = ""
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = {
                        Text("Cloudflare 或局域网地址")
                    },
                    singleLine = true
                )

                Button(
                    onClick = {
                        saveServerBaseUrl(context, serverUrl)
                        serverUrl = getServerBaseUrl(context)
                        saveResult = "已保存服务器地址"
                    },
                    modifier = Modifier.padding(top = 12.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = primaryButtonColors()
                ) {
                    Text("保存")
                }

                ResultText(saveResult.ifBlank { "当前地址：$serverUrl" })
            }
        }

        item {
            SettingsCard(
                title = "连接测试",
                description = "确认手机可以访问后端 /debug/process-test。"
            ) {
                StatusRow(
                    label = "当前连接状态",
                    value = connectionStatus
                )

                Button(
                    onClick = {
                        connectionStatus = "测试中"
                        pingResult = "正在测试..."

                        scope.launch {
                            try {
                                val result = withContext(Dispatchers.IO) {
                                    testPublicPing(serverUrl)
                                }

                                connectionStatus = if (result == "连接正常") {
                                    "连接正常"
                                } else {
                                    "连接失败"
                                }
                                pingResult = result
                            } catch (e: Exception) {
                                connectionStatus = "连接失败"
                                pingResult = "连接失败：${e.message ?: e.javaClass.simpleName}"
                            }
                        }
                    },
                    modifier = Modifier.padding(top = 12.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = primaryButtonColors()
                ) {
                    Text("测试连接")
                }

                ResultText(pingResult)
            }
        }

        item {
            SettingsCard(
                title = "现实世界数据",
                description = "上传今日基础上下文；天气由后端补全。"
            ) {
                OutlinedTextField(
                    value = city,
                    onValueChange = {
                        city = it
                        saveDailyContextLocation(context, city, country)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = {
                        Text("城市")
                    },
                    singleLine = true
                )

                OutlinedTextField(
                    value = country,
                    onValueChange = {
                        country = it
                        saveDailyContextLocation(context, city, country)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = {
                        Text("国家")
                    },
                    singleLine = true
                )

                StatusRow(
                    label = "当前电量",
                    value = deviceSnapshot.batteryPercent?.let { percent ->
                        if (deviceSnapshot.isCharging == true) {
                            "$percent% · 充电中"
                        } else {
                            "$percent%"
                        }
                    } ?: "Unknown"
                )
                StatusRow(
                    label = "当前网络",
                    value = deviceSnapshot.networkType
                )
                StatusRow(
                    label = "当前时区",
                    value = deviceSnapshot.timezone
                )

                Button(
                    onClick = {
                        saveDailyContextLocation(context, city, country)
                        deviceSnapshot = collectDeviceContextSnapshot(context)
                        dailyContextResult = "正在上传今日上下文..."

                        scope.launch {
                            try {
                                val request = buildDailyContextRequest(
                                    context = context,
                                    city = city,
                                    country = country
                                )
                                val result = withContext(Dispatchers.IO) {
                                    uploadDailyContext(
                                        baseUrl = getServerBaseUrl(context),
                                        request = request
                                    )
                                }

                                deviceSnapshot = collectDeviceContextSnapshot(context)
                                dailyContextResult = result
                            } catch (e: Exception) {
                                dailyContextResult = "上传失败：${e.message}"
                            }
                        }
                    },
                    modifier = Modifier.padding(top = 12.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = primaryButtonColors()
                ) {
                    Text("上传今日上下文")
                }

                ResultText(dailyContextResult)
            }
        }

        item {
            SettingsCard(
                title = "LifeRecorder 文件",
                description = "手动同步文件索引，响应 Agent 文件请求，并查看 App 虚拟文件列表。"
            ) {
                Button(
                    onClick = {
                        fileIndexLauncher.launch(arrayOf("*/*"))
                    },
                    modifier = Modifier.padding(top = 12.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = primaryButtonColors()
                ) {
                    Text("选择文件并同步索引")
                }

                ResultText(phoneSyncResult)

                Button(
                    onClick = {
                        pendingRequestResult = "正在检查 Agent 文件请求..."

                        scope.launch {
                            try {
                                val baseUrl = getServerBaseUrl(context)
                                val pendingRequests = withContext(Dispatchers.IO) {
                                    fetchPendingPhoneFileRequests(baseUrl)
                                }.filter { request ->
                                    request.status == "pending" &&
                                        request.type == "fetch_phone_file" &&
                                        request.deviceId == "android_main"
                                }

                                if (pendingRequests.isEmpty()) {
                                    pendingRequestResult = "没有待处理的 Agent 文件请求"
                                    Log.d("PhoneSync", "uploadedCount=0, skippedCount=0")
                                    Toast.makeText(context, "暂无 Agent 文件请求", Toast.LENGTH_SHORT).show()
                                    return@launch
                                }

                                var uploadedCount = 0
                                var skippedCount = 0
                                val resultLines = mutableListOf<String>()

                                pendingRequests.forEach { request ->
                                    Log.d("PhoneSync", "request fileId=${request.fileId}")

                                    val uriText = getPhoneFileUriMapping(context, request.fileId)
                                    if (uriText.isNullOrBlank()) {
                                        skippedCount++
                                        Log.d("PhoneSync", "missing uri")
                                        resultLines.add("${request.fileId}: 缺少本地授权")
                                        return@forEach
                                    }

                                    Log.d("PhoneSync", "found uri")
                                    val result = withContext(Dispatchers.IO) {
                                        uploadRequestedPhoneFile(
                                            baseUrl = baseUrl,
                                            context = context,
                                            request = request,
                                            uri = Uri.parse(uriText)
                                        )
                                    }

                                    if (result.contains("失败") ||
                                        result.contains("超时") ||
                                        result.contains("重新选择文件") ||
                                        result.startsWith("HTTP")
                                    ) {
                                        skippedCount++
                                        resultLines.add("${request.fileId}: $result")
                                    } else {
                                        uploadedCount++
                                        resultLines.add("${request.fileId}: 已上传")
                                    }
                                }

                                Log.d("PhoneSync", "uploadedCount=$uploadedCount, skippedCount=$skippedCount")
                                pendingRequestResult = "已上传 $uploadedCount 个，跳过 $skippedCount 个\n${resultLines.joinToString("\n")}"

                                val toastMessage = if (uploadedCount > 0) {
                                    "已上传 $uploadedCount 个 Agent 请求的文件，跳过 $skippedCount 个"
                                } else if (skippedCount > 0) {
                                    "没有可上传文件，$skippedCount 个请求缺少本地授权"
                                } else {
                                    "暂无 Agent 文件请求"
                                }
                                Toast.makeText(context, toastMessage, Toast.LENGTH_LONG).show()
                            } catch (e: Exception) {
                                val errorMessage = e.message ?: e.javaClass.simpleName
                                pendingRequestResult = "检查 Agent 文件请求失败：$errorMessage"
                                Toast.makeText(context, "检查 Agent 文件请求失败：$errorMessage", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    modifier = Modifier.padding(top = 12.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = primaryButtonColors()
                ) {
                    Text("检查 Agent 文件请求")
                }

                ResultText(pendingRequestResult)

                Button(
                    onClick = {
                        appFilesResult = "正在刷新文件列表..."

                        scope.launch {
                            try {
                                val response = withContext(Dispatchers.IO) {
                                    getAppFilesIndex(getServerBaseUrl(context))
                                }

                                appFiles = response.files
                                appFilesResult = "已刷新：${response.files.size} 个文件"
                                Toast.makeText(context, "文件列表已刷新", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                val errorMessage = e.message ?: e.javaClass.simpleName
                                appFilesResult = "刷新文件列表失败：$errorMessage"
                                Toast.makeText(context, "刷新文件列表失败：$errorMessage", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    modifier = Modifier.padding(top = 12.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = primaryButtonColors()
                ) {
                    Text("刷新文件列表")
                }

                ResultText(appFilesResult)

                Column(
                    modifier = Modifier.padding(top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (appFiles.isEmpty()) {
                        Text(
                            text = "暂无同步文件",
                            fontSize = 13.sp,
                            color = Color(0xFF6E6E73)
                        )
                    } else {
                        appFiles.forEach { file ->
                            AppFileIndexCard(file = file)
                        }
                    }
                }
            }
        }

        item {
            SettingsCard(
                title = "Agent 生成结果",
                description = "查看 OpenClaw Agent 写入 today/ai_generated 的 Markdown / JSON 文件。"
            ) {
                Button(
                    onClick = {
                        agentGeneratedResult = "正在刷新 Agent 生成结果..."

                        scope.launch {
                            try {
                                val files = withContext(Dispatchers.IO) {
                                    getAgentGeneratedFiles(getServerBaseUrl(context))
                                }

                                agentGeneratedFiles = files
                                agentGeneratedResult = "已刷新：${files.size} 个文件"
                                Toast.makeText(context, "Agent 生成结果已刷新", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                val errorMessage = e.message ?: e.javaClass.simpleName
                                agentGeneratedResult = "刷新 Agent 生成结果失败：$errorMessage"
                                Toast.makeText(context, "刷新 Agent 生成结果失败：$errorMessage", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    modifier = Modifier.padding(top = 12.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = primaryButtonColors()
                ) {
                    Text("刷新 Agent 生成结果")
                }

                ResultText(agentGeneratedResult)

                Column(
                    modifier = Modifier.padding(top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (agentGeneratedFiles.isEmpty()) {
                        Text(
                            text = "暂无 Agent 生成文件",
                            fontSize = 13.sp,
                            color = Color(0xFF6E6E73)
                        )
                    } else {
                        agentGeneratedFiles.forEach { file ->
                            AgentGeneratedFileCard(
                                file = file,
                                onClick = {
                                    selectedAgentGeneratedName = file.name
                                    selectedAgentGeneratedContent = "正在读取 ${file.name}..."

                                    scope.launch {
                                        try {
                                            selectedAgentGeneratedContent = withContext(Dispatchers.IO) {
                                                getAgentGeneratedFileContent(
                                                    baseUrl = getServerBaseUrl(context),
                                                    relativePath = file.relativePath
                                                )
                                            }
                                            Toast.makeText(context, "已读取 ${file.name}", Toast.LENGTH_SHORT).show()
                                        } catch (e: Exception) {
                                            selectedAgentGeneratedContent = "读取失败：${e.message ?: e.javaClass.simpleName}"
                                            Toast.makeText(context, selectedAgentGeneratedContent, Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            )
                        }
                    }
                }

                if (selectedAgentGeneratedContent.isNotBlank()) {
                    AgentGeneratedContentCard(
                        name = selectedAgentGeneratedName,
                        content = selectedAgentGeneratedContent
                    )
                }
            }
        }

        item {
            SettingsCard(
                title = "上传当前笔记",
                description = "把当前正在编辑的笔记发送到后端。"
            ) {
                Button(
                    onClick = {
                        uploadResult = "正在上传..."

                        scope.launch {
                            try {
                                val result = withContext(Dispatchers.IO) {
                                    uploadCurrentNote(
                                        baseUrl = serverUrl,
                                        title = currentTitle,
                                        content = currentContent
                                    )
                                }

                                uploadResult = result
                            } catch (e: Exception) {
                                uploadResult = "上传失败：${e.message}"
                            }
                        }
                    },
                    shape = RoundedCornerShape(18.dp),
                    colors = primaryButtonColors()
                ) {
                    Text("上传当前笔记")
                }

                ResultText(uploadResult)
            }
        }

        item {
            SettingsCard(
                title = "读取今日总结",
                description = "从后端读取今天的 AI 总结。"
            ) {
                Button(
                    onClick = {
                        summaryText = "正在读取总结..."

                        scope.launch {
                            try {
                                val result = withContext(Dispatchers.IO) {
                                    readTodaySummary(serverUrl)
                                }

                                summaryText = result
                            } catch (e: Exception) {
                                summaryText = "读取失败：${e.message}"
                            }
                        }
                    },
                    shape = RoundedCornerShape(18.dp),
                    colors = primaryButtonColors()
                ) {
                    Text("读取今日总结")
                }

                ResultText(summaryText)
            }
        }
    }
}

@Composable
private fun AppFileIndexCard(file: AppFileItem) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFF2F2F7)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = file.name.ifBlank { "未命名文件" },
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1C1C1E)
            )
            Text(
                text = "${if (file.availableLocally) "已缓存" else "仅索引"} · ${formatFileSize(file.size)}",
                fontSize = 12.sp,
                color = Color(0xFF3A3A3C),
                modifier = Modifier.padding(top = 3.dp)
            )
            Text(
                text = "fileId：${file.fileId}",
                fontSize = 12.sp,
                color = Color(0xFF6E6E73),
                modifier = Modifier.padding(top = 3.dp)
            )
            if (!file.cachedPath.isNullOrBlank()) {
                Text(
                    text = "缓存路径：${file.cachedPath}",
                    fontSize = 12.sp,
                    color = Color(0xFF6E6E73),
                    modifier = Modifier.padding(top = 3.dp)
                )
            }
        }
    }
}

@Composable
private fun AgentGeneratedFileCard(
    file: AgentGeneratedFileItem,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFF2F2F7)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = file.name.ifBlank { file.relativePath.substringAfterLast('/') },
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1C1C1E)
            )
            if (file.preview.isNotBlank()) {
                Text(
                    text = file.preview,
                    fontSize = 12.sp,
                    color = Color(0xFF3A3A3C),
                    modifier = Modifier.padding(top = 5.dp)
                )
            }
            Text(
                text = "路径：${file.relativePath}",
                fontSize = 12.sp,
                color = Color(0xFF6E6E73),
                modifier = Modifier.padding(top = 5.dp)
            )
            Text(
                text = "大小：${formatFileSize(file.size)}",
                fontSize = 12.sp,
                color = Color(0xFF6E6E73),
                modifier = Modifier.padding(top = 3.dp)
            )
            Text(
                text = "创建：${formatAgentTime(file.createdTime)} · 更新：${formatAgentTime(file.updatedTime)}",
                fontSize = 12.sp,
                color = Color(0xFF8E8E93),
                modifier = Modifier.padding(top = 3.dp)
            )
        }
    }
}

@Composable
private fun AgentGeneratedContentCard(
    name: String,
    content: String
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFF8F8F8)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = name.ifBlank { "Agent 生成内容" },
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1C1C1E)
            )
            Text(
                text = content,
                fontSize = 13.sp,
                color = Color(0xFF3A3A3C),
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    description: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color.White
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1C1C1E)
            )
            Text(
                text = description,
                fontSize = 13.sp,
                color = Color(0xFF6E6E73),
                modifier = Modifier.padding(top = 5.dp)
            )

            content()
        }
    }
}

@Composable
private fun StatusRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = Color(0xFF6E6E73)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = when (value) {
                "连接正常" -> Color(0xFF2E7D32)
                "连接失败" -> Color(0xFFC62828)
                else -> Color(0xFF1C1C1E)
            }
        )
    }
}

@Composable
private fun ResultText(text: String) {
    Text(
        text = text,
        fontSize = 13.sp,
        color = Color(0xFF6E6E73),
        modifier = Modifier.padding(top = 10.dp)
    )
}

@Composable
private fun primaryButtonColors() = ButtonDefaults.buttonColors(
    containerColor = Color(0xFF1C1C1E),
    contentColor = Color.White
)

private fun formatFileSize(size: Long): String {
    if (size < 1024L) {
        return "$size B"
    }

    val kb = size / 1024.0
    if (kb < 1024.0) {
        return String.format("%.1f KB", kb)
    }

    val mb = kb / 1024.0
    if (mb < 1024.0) {
        return String.format("%.1f MB", mb)
    }

    return String.format("%.1f GB", mb / 1024.0)
}

private fun formatAgentTime(timeMillis: Long): String {
    if (timeMillis <= 0L) {
        return "未知"
    }

    return SimpleDateFormat("yyyy/M/d HH:mm", Locale.getDefault()).format(Date(timeMillis))
}
