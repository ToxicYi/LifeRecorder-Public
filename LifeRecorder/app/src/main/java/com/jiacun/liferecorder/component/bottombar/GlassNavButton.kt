package com.jiacun.liferecorder.component.bottombar

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material3.TextButton
//负责统一的图标加文字按钮样式
@Composable
fun GlassNavButton(
    icon: ImageVector,
    text: String,
    selected: Boolean = false,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val itemColor = if (selected) {
                Color(0xFF111111)
            } else {
                Color(0xFF777777)
            }

            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = itemColor,
                modifier = Modifier.size(22.dp)
            )

            Text(
                text = text,
                color = itemColor,
                fontWeight = if (selected) {
                    FontWeight.Bold
                } else {
                    FontWeight.Normal
                }
            )
        }
    }
}