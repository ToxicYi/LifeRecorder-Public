package com.jiacun.liferecorder.component

/**
 * NoteItem
 *
 * 笔记列表中的单条笔记行，负责展示标题、内容预览、更新时间，
 * 并处理点击打开和长按选择。
 */

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteItem(
    // 笔记 id。
    id: Int,
    // 笔记标题。
    title: String,
    // 笔记正文。
    content: String,
    // 笔记更新时间文本。
    updatedTime: String,
    // 当前选中的笔记 id；0 表示非选择模式。
    selectedNoteId: Int,
    // 点击普通笔记时打开编辑页。
    onOpenNote: (Int, String, String, String) -> Unit,
    // 选择或取消选择笔记。
    onSelectNote: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    if (selectedNoteId == 0) {
                        onOpenNote(id, content, title, updatedTime)
                    } else {
                        onSelectNote(id)
                    }
                },
                onLongClick = {
                    onSelectNote(id)
                }
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.Top
    ) {
        if (selectedNoteId != 0) {
            Icon(
                imageVector = if (selectedNoteId == id) {
                    Icons.Outlined.CheckCircle
                } else {
                    Icons.Outlined.RadioButtonUnchecked
                },
                contentDescription = if (selectedNoteId == id) "已选择" else "未选择",
                tint = Color(0xFF3A3A3C),
                modifier = Modifier
                    .width(30.dp)
                    .size(22.dp)
                    .padding(top = 1.dp)
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (title.isBlank()) "无标题笔记" else title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = Color(0xFF1C1C1E)
            )

            Text(
                text = if (content.isBlank()) "暂无内容" else content.take(72),
                fontSize = 13.sp,
                color = Color(0xFF6E6E73),
                modifier = Modifier.padding(top = 6.dp)
            )

            Text(
                text = if (updatedTime.isBlank()) "未记录时间" else updatedTime,
                fontSize = 12.sp,
                color = Color(0xFF8E8E93),
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}
