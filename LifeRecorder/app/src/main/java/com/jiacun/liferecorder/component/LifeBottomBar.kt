package com.jiacun.liferecorder.component

/**
 * LifeBottomBar
 *
 * App 底部一级导航栏组件，负责显示“最近 / 资源 / 同步 / 我的”四个入口，
 * 并根据当前页面显示选中态。
 */

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun LifeBottomBar(
    // 当前选中的笔记 id；非 0 时隐藏底部栏，避免和选择操作栏冲突。
    selectedNoteId: Int,
    // 当前页面 route，用于计算底部栏选中状态。
    currentPage: String,
    // 点击底部入口时交给 MainActivity 执行导航。
    onPageChange: (String) -> Unit,
    // 外部传入的布局修饰符。
    modifier: Modifier = Modifier
) {
    if (selectedNoteId == 0) {
        // 资源页及其二级页面都高亮“资源”入口。
        val resourceSelected = currentPage == "folders" ||
            currentPage == "list" ||
            currentPage == "edit" ||
            currentPage == "photos" ||
            currentPage == "files" ||
            currentPage == "fileLibrary" ||
            currentPage == "storage" ||
            currentPage == "chat"

        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(40.dp),
                color = Color.White.copy(alpha = 0.72f),
                shadowElevation = 12.dp,
                border = BorderStroke(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.75f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.18f))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    GlassNavButton(
                        icon = Icons.Outlined.Schedule,
                        text = "最近",
                        selected = currentPage == "recent",
                        onClick = {
                            onPageChange("recent")
                        }
                    )

                    GlassNavButton(
                        icon = Icons.Outlined.Folder,
                        text = "资源",
                        selected = resourceSelected,
                        onClick = {
                            onPageChange("folders")
                        }
                    )

                    GlassNavButton(
                        icon = Icons.Outlined.Sync,
                        text = "同步",
                        selected = currentPage == "sync",
                        onClick = {
                            onPageChange("sync")
                        }
                    )

                    GlassNavButton(
                        icon = Icons.Outlined.Person,
                        text = "我的",
                        selected = currentPage == "mine",
                        onClick = {
                            onPageChange("mine")
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun GlassNavButton(
    // 导航项图标。
    icon: ImageVector,
    // 导航项文字。
    text: String,
    // 是否处于选中态。
    selected: Boolean,
    // 点击导航项时执行的回调。
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 选中态使用深色，未选中使用灰色。
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
