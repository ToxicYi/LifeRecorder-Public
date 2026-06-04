package com.jiacun.liferecorder.component

/**
 * LifeTopBar
 *
 * App 全局顶部栏组件，负责根据当前 route 显示标题、返回按钮或选择模式操作。
 */

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LifeTopBar(
    // 当前页面 route，用于决定显示标题还是返回按钮。
    currentPage: String,
    // 当前选中的笔记 id；非 0 时显示选择模式顶部栏。
    selectedNoteId: Int,
    // 取消笔记选择模式。
    onCancelSelection: () -> Unit,
    // 编辑页返回笔记列表。
    onBackToList: () -> Unit,
    // 资源二级页面返回资源页。
    onBackToFolders: () -> Unit,
    // 外部传入的布局修饰符。
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.58f))
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TopBarContent(
                currentPage = currentPage,
                selectedNoteId = selectedNoteId,
                onCancelSelection = onCancelSelection,
                onBackToList = onBackToList,
                onBackToFolders = onBackToFolders
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(0xFFE5E5EA).copy(alpha = 0.45f))
        )
    }
}

@Composable
private fun TopBarContent(
    // 当前页面 route。
    currentPage: String,
    // 当前选中的笔记 id。
    selectedNoteId: Int,
    // 取消选择模式回调。
    onCancelSelection: () -> Unit,
    // 返回笔记列表回调。
    onBackToList: () -> Unit,
    // 返回资源页回调。
    onBackToFolders: () -> Unit
) {
    if (selectedNoteId != 0 && currentPage == "list") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = {
                    // TODO：后面做多选时再实现全选
                }
            ) {
                Text(
                    text = "全选",
                    color = Color(0xFFFFC400),
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "已选择 1 项",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF222222)
            )

            TextButton(
                onClick = onCancelSelection
            ) {
                Text(
                    text = "取消",
                    color = Color(0xFFFFC400),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    } else if (currentPage == "edit") {
        TextButton(
            onClick = onBackToList
        ) {
            Text("< 返回列表")
        }
    } else if (currentPage == "list") {
        ResourceBackButton(
            text = "< 笔记",
            onClick = onBackToFolders
        )
    } else if (currentPage == "photos") {
        ResourceBackButton(
            text = "< 相册",
            onClick = onBackToFolders
        )
    } else if (currentPage == "files") {
        ResourceBackButton(
            text = "< 文件",
            onClick = onBackToFolders
        )
    } else if (currentPage == "fileLibrary") {
        ResourceBackButton(
            text = "< 文件",
            onClick = onBackToFolders
        )
    } else if (currentPage == "storage") {
        ResourceBackButton(
            text = "< 存储",
            onClick = onBackToFolders
        )
    } else if (currentPage == "chat") {
        ResourceBackButton(
            text = "< AI",
            onClick = onBackToFolders
        )
    } else if (currentPage == "sync") {
        MainTopTitle("同步")
    } else if (currentPage == "mine") {
        MainTopTitle("我的")
    } else if (currentPage == "recent") {
        MainTopTitle("最近")
    } else if (currentPage == "folders") {
        MainTopTitle("资源")
    } else {
        Text("LifeRecorder.v1.0")
    }
}

@Composable
fun ResourceBackButton


























































































































































            (
    // 返回按钮显示文字。
    text: String,
    // 点击返回按钮时执行的回调。
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick
    ) {
        Text(text)
    }
}

@Composable
fun MainTopTitle(text: String) {
    Text(
        text = text,
        fontSize = 32.sp,
        fontWeight = FontWeight.Bold,
        color = Color.Black
    )
}
