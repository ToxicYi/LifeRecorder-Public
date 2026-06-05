package com.jiacun.liferecorder.feature.file.storage

/**
 * FilesScreen
 *
 * 负责：
 * - 显示“手机存储/存储”页面。
 * - 浏览用户授权或公共目录下的第一层文件和文件夹。
 * - 支持进入文件夹、返回上一级、用第三方 App 打开文件。
 *
 * 不负责：
 * - 不作为 LifeRecorder App 自己的文件库。
 * - 不保存 App 虚拟文件索引。
 * - 不上传、删除、重命名或移动系统文件。
 *
 * 数据来源：
 * - 文件读取和打开能力来自 data/FileStorage.kt。
 * - 主文件库入口由 FileLibraryScreen 负责。
 */

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
@Composable
fun FilesScreen(
    onNestedFolderChange: (Boolean) -> Unit = {},
    onInternalBackHandlerChange: (((() -> Unit)?) -> Unit)? = null
) {
    val context = LocalContext.current
    val rootPath = remember {
        Environment.getExternalStorageDirectory().absolutePath
    }

    var hasAllFilesAccess by remember {
        mutableStateOf(checkAllFilesAccess())
    }
    var currentDirectoryPath by remember {
        mutableStateOf(rootPath)
    }
    var currentDirectoryTitle by remember {
        mutableStateOf("内部存储")
    }
    var currentItems by remember {
        mutableStateOf<List<LifeFileItem>>(emptyList())
    }
    var currentStatus by remember {
        mutableStateOf("")
    }
    var pathStack by remember {
        mutableStateOf<List<DirectoryState>>(emptyList())
    }
    var savedTreeUri by remember {
        mutableStateOf(getAuthorizedTreeUri(context))
    }
    var safItems by remember {
        mutableStateOf<List<LifeFileItem>>(emptyList())
    }
    var safStatusText by remember {
        mutableStateOf("")
    }

    fun loadDirectory(
        path: String,
        title: String,
        pushCurrent: Boolean
    ) {
        hasAllFilesAccess = checkAllFilesAccess()

        if (!hasAllFilesAccess) {
            currentItems = emptyList()
            currentStatus = "系统限制访问。可以打开全部文件访问权限，或选择一个授权文件夹。"
            onNestedFolderChange(false)
            return
        }

        if (pushCurrent) {
            pathStack = pathStack + DirectoryState(
                title = currentDirectoryTitle,
                path = currentDirectoryPath,
                items = currentItems,
                status = currentStatus
            )
        }

        val result = if (path == rootPath) {
            loadSharedStorageFiles()
        } else {
            loadPathDirectoryFiles(path)
        }

        currentDirectoryPath = path
        currentDirectoryTitle = title
        currentItems = result.items
        currentStatus = result.errorMessage ?: "当前目录 ${result.items.size} 项"
        onNestedFolderChange(path != rootPath)
    }

    fun refreshCurrentDirectory() {
        loadDirectory(
            path = currentDirectoryPath,
            title = currentDirectoryTitle,
            pushCurrent = false
        )
    }

    fun openFolder(item: LifeFileItem) {
        if (!item.isDirectory || item.accessError != null) {
            return
        }

        loadDirectory(
            path = item.uriOrPath,
            title = item.name,
            pushCurrent = true
        )
    }

    fun navigateBack() {
        val previous = pathStack.lastOrNull()

        if (previous == null) {
            currentStatus = "已经在根目录"
            onNestedFolderChange(false)
            return
        }

        currentDirectoryPath = previous.path
        currentDirectoryTitle = previous.title
        currentItems = previous.items
        currentStatus = previous.status
        pathStack = pathStack.dropLast(1)
        onNestedFolderChange(previous.path != rootPath)
    }

    BackHandler(enabled = pathStack.isNotEmpty()) {
        navigateBack()
    }

    fun refreshSafFiles(uri: Uri?) {
        if (uri == null) {
            safItems = emptyList()
            safStatusText = "还没有选择授权文件夹"
            return
        }

        val result = loadAuthorizedTreeFiles(context, uri)
        safItems = result.items
        safStatusText = result.errorMessage ?: "授权文件夹 ${result.items.size} 项"
    }

    val settingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = {
            refreshCurrentDirectory()
        }
    )

    val folderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
        onResult = { uri ->
            if (uri == null) {
                safStatusText = "没有选择文件夹"
                return@rememberLauncherForActivityResult
            }

            try {
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, flags)
                saveAuthorizedTreeUri(context, uri)
                savedTreeUri = uri
                refreshSafFiles(uri)
            } catch (e: Exception) {
                safStatusText = "保存文件夹授权失败：${e.message}"
            }
        }
    )

    LaunchedEffect(Unit) {
        onNestedFolderChange(false)
        refreshCurrentDirectory()
        refreshSafFiles(savedTreeUri)
    }

    LaunchedEffect(pathStack) {
        onNestedFolderChange(pathStack.isNotEmpty())
        onInternalBackHandlerChange?.invoke(
            if (pathStack.isNotEmpty()) {
                { navigateBack() }
            } else {
                null
            }
        )
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
            Text(
                text = "存储",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1C1C1E),
                modifier = Modifier.padding(top = 8.dp)
            )

            Text(
                text = "浏览手机公共目录，验证可访问的系统存储",
                fontSize = 12.sp,
                color = Color(0xFF8E8E93),
                modifier = Modifier.padding(top = 4.dp)
            )

            Text(
                text = currentDirectoryPath,
                fontSize = 12.sp,
                color = Color(0xFF8E8E93),
                modifier = Modifier.padding(top = 3.dp)
            )
        }

        item {
            FileControlCard(
                hasAllFilesAccess = hasAllFilesAccess,
                currentStatus = currentStatus,
                canGoBack = pathStack.isNotEmpty(),
                onRefreshOrRequestAccess = {
                    if (hasAllFilesAccess) {
                        refreshCurrentDirectory()
                    } else {
                        try {
                            settingsLauncher.launch(allFilesAccessSettingsIntent(context.packageName))
                        } catch (e: Exception) {
                            try {
                                settingsLauncher.launch(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
                            } catch (fallbackError: Exception) {
                                currentStatus = "无法打开全部文件访问设置：${fallbackError.message}"
                            }
                        }
                    }
                },
                onBack = {
                    navigateBack()
                },
                onChooseFolder = {
                    folderLauncher.launch(savedTreeUri)
                }
            )
        }

        item {
            FileListCard(
                title = currentDirectoryTitle,
                subtitle = "${currentItems.size} 项",
                items = currentItems,
                emptyText = if (hasAllFilesAccess) {
                    "这个目录里没有可显示的文件"
                } else {
                    "系统限制访问。开启权限或选择授权文件夹后可以浏览文件。"
                },
                onOpenFile = { item ->
                    openLifeFileItem(context, item)
                },
                onOpenFolder = { item ->
                    openFolder(item)
                }
            )
        }

        if (savedTreeUri != null || !hasAllFilesAccess) {
            item {
                FileListCard(
                    title = "授权文件夹",
                    subtitle = safStatusText,
                    items = safItems,
                    emptyText = "还没有选择授权文件夹",
                    onOpenFile = { item ->
                        openLifeFileItem(context, item)
                    },
                    onOpenFolder = null
                )
            }
        }
    }
}

private fun checkAllFilesAccess(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Environment.isExternalStorageManager()
    } else {
        true
    }
}

private fun allFilesAccessSettingsIntent(packageName: String): Intent {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Intent(
            Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
            Uri.parse("package:$packageName")
        )
    } else {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:$packageName")
        }
    }
}

@Composable
private fun FileControlCard(
    hasAllFilesAccess: Boolean,
    currentStatus: String,
    canGoBack: Boolean,
    onRefreshOrRequestAccess: () -> Unit,
    onBack: () -> Unit,
    onChooseFolder: () -> Unit
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
                text = if (hasAllFilesAccess) "文件访问已开启" else "系统限制访问",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1C1C1E)
            )
            Text(
                text = currentStatus,
                fontSize = 13.sp,
                color = Color(0xFF6E6E73),
                modifier = Modifier.padding(top = 6.dp)
            )

            Row(
                modifier = Modifier.padding(top = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onRefreshOrRequestAccess,
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1C1C1E),
                        contentColor = Color.White
                    )
                ) {
                    Text(if (hasAllFilesAccess) "刷新" else "打开权限")
                }

                Button(
                    onClick = onChooseFolder,
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF2F2F7),
                        contentColor = Color(0xFF1C1C1E)
                    )
                ) {
                    Text("选择文件夹")
                }

                if (canGoBack) {
                    Button(
                        onClick = onBack,
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF2F2F7),
                            contentColor = Color(0xFF1C1C1E)
                        )
                    ) {
                        Text("上一级")
                    }
                }
            }
        }
    }
}

@Composable
private fun FileListCard(
    title: String,
    subtitle: String,
    items: List<LifeFileItem>,
    emptyText: String,
    onOpenFile: (LifeFileItem) -> Unit,
    onOpenFolder: ((LifeFileItem) -> Unit)?
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color.White
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Text(
                    text = title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1C1C1E)
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = Color(0xFF8E8E93),
                    modifier = Modifier.padding(top = 3.dp)
                )
            }

            if (items.isEmpty()) {
                HorizontalDivider(
                    color = Color(0xFFE5E5EA),
                    thickness = 1.dp
                )
                Text(
                    text = emptyText,
                    fontSize = 14.sp,
                    color = Color(0xFF8E8E93),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
                )
            } else {
                items.forEachIndexed { index, item ->
                    HorizontalDivider(
                        color = Color(0xFFE5E5EA),
                        thickness = 1.dp,
                        modifier = Modifier.padding(start = if (index == 0) 0.dp else 54.dp)
                    )

                    FileItemRow(
                        name = item.name,
                        path = itemSubtitle(item),
                        isDirectory = item.isDirectory,
                        isRestricted = item.accessError != null,
                        onClick = when {
                            item.accessError != null -> null
                            item.isDirectory && onOpenFolder != null -> {
                                { onOpenFolder(item) }
                            }
                            !item.isDirectory -> {
                                { onOpenFile(item) }
                            }
                            else -> null
                        }
                    )
                }
            }
        }
    }
}

private fun itemSubtitle(item: LifeFileItem): String {
    return when {
        item.accessError != null -> "系统限制访问"
        item.isDirectory -> "文件夹"
        item.type.isNullOrBlank() -> "文件"
        else -> item.type
    }
}

private data class DirectoryState(
    val title: String,
    val path: String,
    val items: List<LifeFileItem>,
    val status: String
)
