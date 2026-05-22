package com.jiacun.liferecorder.component

/**
 * FileItemRow
 *
 * 文件/文件夹列表行组件，用于存储浏览页面展示文件夹、普通文件和受限目录。
 */

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun FileItemRow(
    // 文件或文件夹显示名称。
    name: String,
    // 文件路径或用于辅助展示的路径文本。
    path: String,
    // 是否为文件夹。
    isDirectory: Boolean,
    // 是否为系统受限目录。
    isRestricted: Boolean,
    // 可点击行的点击回调；为空时该行不可点击。
    onClick: (() -> Unit)? = null,
    // 外部传入的布局修饰符。
    modifier: Modifier = Modifier
) {
    // 根据文件状态选择左侧图标。
    val icon: ImageVector = when {
        isRestricted -> Icons.Outlined.Lock
        isDirectory -> Icons.Outlined.Folder
        else -> Icons.Outlined.InsertDriveFile
    }

    // 受限目录使用灰色标题，普通文件/文件夹使用主文字色。
    val titleColor = if (isRestricted) {
        Color(0xFF8E8E93)
    } else {
        Color(0xFF1C1C1E)
    }

    // 第二行说明文本：受限目录显示限制说明，普通条目显示路径。
    val subtitle = if (isRestricted) {
        "系统限制访问"
    } else {
        path
    }

    // 右侧操作提示文本。
    val trailingText = when {
        isRestricted -> "受限"
        isDirectory -> "›"
        else -> "打开"
    }

    // 有点击回调时才给整行添加 clickable。
    val rowModifier = if (onClick != null) {
        modifier.clickable(onClick = onClick)
    } else {
        modifier
    }

    Row(
        modifier = rowModifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = name,
            tint = Color(0xFF3A3A3C),
            modifier = Modifier
                .width(34.dp)
                .size(22.dp)
        )

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = name,
                color = titleColor,
                fontSize = 16.sp,
                fontWeight = if (isDirectory && !isRestricted) {
                    FontWeight.Bold
                } else {
                    FontWeight.Normal
                }
            )

            Text(
                text = subtitle,
                color = Color(0xFF8E8E93),
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Text(
            text = trailingText,
            color = if (isRestricted) {
                Color(0xFFAEAEB2)
            } else {
                Color(0xFF8E8E93)
            },
            fontSize = 14.sp,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}
