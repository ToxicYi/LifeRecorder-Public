package com.jiacun.liferecorder.feature.photo

/**
 * PhotosScreen
 *
 * 负责：
 * - 显示系统相册图片网格。
 * - 处理图片读取权限状态、空状态和简单图片预览。
 * - 调用本地相册读取能力加载图片 Uri。
 *
 * 不负责：
 * - 不上传图片。
 * - 不删除、编辑或移动系统照片。
 * - 不处理 LifeRecorder 文件库或 phone_sync 文件索引。
 *
 * 数据来源：
 * - 图片 Uri 来自 data/PhotoStorage.kt。
 * - 权限请求只服务于相册展示，不影响其他页面。
 */

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.rememberAsyncImagePainter
@Composable
fun PhotosScreen() {
    val context = LocalContext.current
    val imagePermission = rememberImagePermission()

    var imageUris by remember {
        mutableStateOf<List<Uri>>(emptyList())
    }
    var permissionGranted by remember {
        mutableStateOf(hasImagePermission(context, imagePermission))
    }
    var errorMessage by remember {
        mutableStateOf("")
    }

    fun loadImages() {
        runCatching {
            loadAllImageUris(context)
        }.onSuccess { uris ->
            imageUris = uris
            errorMessage = ""
        }.onFailure { error ->
            imageUris = emptyList()
            errorMessage = "读取相册失败：${error.message ?: error.javaClass.simpleName}"
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            permissionGranted = granted
            if (granted) {
                loadImages()
            }
        }
    )

    LaunchedEffect(permissionGranted) {
        if (permissionGranted) {
            loadImages()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF2F2F7))
            .padding(horizontal = 20.dp)
            .padding(top = 12.dp)
    ) {
        Text(
            text = "相册",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1C1C1E),
            modifier = Modifier.padding(top = 8.dp)
        )

        Text(
            text = "共 ${imageUris.size} 张图片",
            fontSize = 14.sp,
            color = Color(0xFF8E8E93),
            modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
        )

        when {
            !permissionGranted -> {
                PermissionStateCard(
                    onRequestPermission = {
                        permissionLauncher.launch(imagePermission)
                    }
                )
            }

            errorMessage.isNotBlank() -> {
                StateCard(text = errorMessage)
            }

            imageUris.isEmpty() -> {
                StateCard(text = "相册里还没有可显示的图片")
            }

            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(imageUris) { uri ->
                        Image(
                            painter = rememberAsyncImagePainter(uri),
                            contentDescription = "相册图片",
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionStateCard(
    onRequestPermission: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color.White
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Text(
                text = "需要相册访问权限",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1C1C1E)
            )
            Text(
                text = "授权后可以在这里浏览系统相册图片。拒绝权限不会影响其他功能。",
                fontSize = 14.sp,
                color = Color(0xFF6E6E73),
                modifier = Modifier.padding(top = 8.dp, bottom = 14.dp)
            )
            Button(
                onClick = onRequestPermission
            ) {
                Text("允许访问相册")
            }
        }
    }
}

@Composable
private fun StateCard(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color.White
    ) {
        Text(
            text = text,
            fontSize = 15.sp,
            color = Color(0xFF6E6E73),
            modifier = Modifier.padding(18.dp)
        )
    }
}

@Composable
private fun rememberImagePermission(): String {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
}

private fun hasImagePermission(context: Context, permission: String): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        permission
    ) == PackageManager.PERMISSION_GRANTED
}
