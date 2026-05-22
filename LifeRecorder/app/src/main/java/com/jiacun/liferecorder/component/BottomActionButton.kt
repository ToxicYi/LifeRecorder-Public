package com.jiacun.liferecorder.component

/**
 * BottomActionButton
 *
 * 底部操作栏使用的图标+文字按钮组件，当前用于选择模式或底部工具操作。
 */

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BottomActionButton(
    // 按钮上方显示的线性图标。
    icon: ImageVector,
    // 按钮下方显示的文字。
    text: String,
    // 点击按钮时执行的操作。
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = Color(0xFF3A3A3C),
                modifier = androidx.compose.ui.Modifier.size(24.dp)
            )

            Text(
                text = text,
                color = Color(0xFF222222),
                fontWeight = FontWeight.Bold
            )
        }
    }
}
