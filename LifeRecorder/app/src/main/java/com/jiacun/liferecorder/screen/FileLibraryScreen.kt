package com.jiacun.liferecorder.screen

/**
 * FileLibraryScreen
 *
 * 负责：
 * - 显示 LifeRecorder 自己的文件库页面。
 * - 让用户手动导入文件，并展示已导入文件列表。
 * - 点击文件时交给系统或第三方 App 打开本地保存的文件。
 *
 * 不负责：
 * - 不扫描手机全盘文件。
 * - 不处理 phone_sync 后端索引协议。
 * - 不删除、移动、重命名或上传文件。
 *
 * 数据来源：
 * - 文件导入和列表读取通过 data/FileLibraryStorage.kt 完成。
 * - 与系统公共目录浏览页面 FilesScreen 分工独立。
 */

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jiacun.liferecorder.data.ImportedFileItem
import com.jiacun.liferecorder.data.LifeFileItem
import com.jiacun.liferecorder.data.formatImportedFileSize
import com.jiacun.liferecorder.data.getImportedFiles
import com.jiacun.liferecorder.data.importSharedUriToFileLibrary
import com.jiacun.liferecorder.data.openLifeFileItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FileLibraryScreen() {
    val context = LocalContext.current

    var importedFiles by remember {
        mutableStateOf(getImportedFiles(context))
    }
    var statusText by remember {
        mutableStateOf("")
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            if (uri == null) {
                statusText = "没有选择文件"
                return@rememberLauncherForActivityResult
            }

            val item = importSharedUriToFileLibrary(
                context = context,
                uri = uri,
                mimeType = context.contentResolver.getType(uri)
            )
            importedFiles = getImportedFiles(context)
            statusText = "已导入：${item.name}"
        }
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF2F2F7))
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "文件",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1C1C1E),
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = Color.White
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "这里用于管理导入到 LifeRecorder 的文件",
                        fontSize = 14.sp,
                        color = Color(0xFF6E6E73)
                    )

                    Button(
                        onClick = {
                            importLauncher.launch(arrayOf("*/*"))
                        },
                        modifier = Modifier.padding(top = 14.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1C1C1E),
                            contentColor = Color.White
                        )
                    ) {
                        Text("导入文件")
                    }

                    if (statusText.isNotBlank()) {
                        Text(
                            text = statusText,
                            fontSize = 13.sp,
                            color = Color(0xFF8E8E93),
                            modifier = Modifier.padding(top = 10.dp)
                        )
                    }
                }
            }
        }

        item {
            FileLibraryList(
                files = importedFiles,
                onOpenFile = { file ->
                    openLifeFileItem(
                        context = context,
                        item = LifeFileItem(
                            name = file.name,
                            uriOrPath = file.uri,
                            type = file.mimeType,
                            isDirectory = false
                        )
                    )
                }
            )
        }
    }
}

@Composable
private fun FileLibraryList(
    files: List<ImportedFileItem>,
    onOpenFile: (ImportedFileItem) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color.White
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "已导入文件",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1C1C1E),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
            )

            HorizontalDivider(color = Color(0xFFE5E5EA), thickness = 1.dp)

            if (files.isEmpty()) {
                Text(
                    text = "还没有导入文件",
                    fontSize = 14.sp,
                    color = Color(0xFF8E8E93),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
                )
            } else {
                files.forEachIndexed { index, file ->
                    if (index > 0) {
                        HorizontalDivider(
                            color = Color(0xFFE5E5EA),
                            thickness = 1.dp,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }

                    ImportedFileRow(
                        file = file,
                        onClick = {
                            onOpenFile(file)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ImportedFileRow(
    file: ImportedFileItem,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = file.name.ifBlank { "未命名文件" },
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF1C1C1E)
            )
            Text(
                text = "${file.mimeType ?: "未知类型"} · ${formatImportedFileSize(file.sizeBytes)}",
                fontSize = 12.sp,
                color = Color(0xFF8E8E93),
                modifier = Modifier.padding(top = 3.dp)
            )
        }

        Text(
            text = formatAddedTime(file.addedTime),
            fontSize = 12.sp,
            color = Color(0xFF8E8E93),
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}

private fun formatAddedTime(time: Long): String {
    if (time <= 0L) {
        return "未知时间"
    }

    return SimpleDateFormat("yyyy/M/d HH:mm", Locale.getDefault()).format(Date(time))
}
