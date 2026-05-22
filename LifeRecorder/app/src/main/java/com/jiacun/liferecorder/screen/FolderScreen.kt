package com.jiacun.liferecorder.screen

/**
 * FoldersScreen
 *
 * 负责：
 * - 显示“资源”入口页。
 * - 提供笔记、相册、文件、AI、手机存储、同步与设置等入口。
 * - 只负责入口列表 UI 和点击回调。
 *
 * 不负责：
 * - 不实现各资源页面内部功能。
 * - 不读取或保存业务数据。
 * - 不直接发起网络请求。
 *
 * 数据来源：
 * - 笔记数量由上层传入。
 * - 页面跳转由 MainActivity/NavController 通过回调处理。
 */

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val PageHorizontalPadding = 24.dp
private val CardCornerRadius = 20.dp
private val PageBackground = Color(0xFFFAFAFA)

@Composable
fun FoldersScreen(
    noteCount: Int,
    onOpenNotes: () -> Unit,
    onOpenPhotos: () -> Unit,
    onOpenFiles: () -> Unit,
    onOpenStorage: () -> Unit,
    onOpenChat: () -> Unit,
    onOpenMine: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PageBackground)
            .padding(horizontal = PageHorizontalPadding)
            .padding(top = 12.dp)
    ) {
        GroupTitle(text = "资源")

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(CardCornerRadius),
            color = Color.White
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                FolderRow(
                    icon = Icons.Outlined.Description,
                    title = "全部笔记",
                    description = "${noteCount} 条笔记",
                    onClick = onOpenNotes
                )

                ResourceDivider()

                FolderRow(
                    icon = Icons.Outlined.PhotoLibrary,
                    title = "相册",
                    description = "浏览系统相册图片",
                    onClick = onOpenPhotos
                )

                ResourceDivider()

                FolderRow(
                    icon = Icons.Outlined.Folder,
                    title = "文件",
                    description = "导入到 LifeRecorder 的文件",
                    onClick = onOpenFiles
                )

                ResourceDivider()

                FolderRow(
                    icon = Icons.Outlined.SmartToy,
                    title = "AI 助手",
                    description = "连接后端智能助手",
                    onClick = onOpenChat
                )
            }
        }

        GroupTitle(
            text = "存储",
            modifier = Modifier.padding(top = 18.dp)
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(CardCornerRadius),
            color = Color.White
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                FolderRow(
                    icon = Icons.Outlined.Storage,
                    title = "手机存储",
                    description = "浏览手机公共目录，验证系统存储访问",
                    onClick = onOpenStorage
                )

                ResourceDivider()

                FolderRow(
                    icon = Icons.Outlined.Settings,
                    title = "同步与设置",
                    description = "服务器地址与个人设置",
                    onClick = onOpenMine
                )
            }
        }
    }
}

@Composable
private fun GroupTitle(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color(0xFF8E8E93),
        modifier = modifier.padding(start = 4.dp, bottom = 6.dp)
    )
}

@Composable
fun FolderRow(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onClick()
            }
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = Color(0xFF3A3A3C),
            modifier = Modifier.width(32.dp)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF1C1C1E)
            )

            Text(
                text = description,
                fontSize = 12.sp,
                color = Color(0xFF8E8E93),
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        Text(
            text = "›",
            fontSize = 23.sp,
            color = Color(0xFFC7C7CC)
        )
    }
}

@Composable
private fun ResourceDivider() {
    HorizontalDivider(
        color = Color(0xFFE5E5EA),
        thickness = 1.dp,
        modifier = Modifier.padding(start = 56.dp)
    )
}
