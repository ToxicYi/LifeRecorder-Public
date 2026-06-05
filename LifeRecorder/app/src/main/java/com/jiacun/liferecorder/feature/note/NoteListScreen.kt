package com.jiacun.liferecorder.feature.note

import android.content.SharedPreferences
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * NoteListScreen
 *
 * 负责：
 * - 显示全部笔记列表。
 * - 展示标题、内容预览、更新时间。
 * - 支持打开笔记、创建笔记、长按选择和删除操作入口。
 *
 * 不负责：
 * - 不直接实现笔记正文编辑。
 * - 不直接上传笔记到后端。
 * - 不修改 NoteStorage 的保存规则。
 *
 * 数据来源：
 * - 笔记数据从上层传入的 SharedPreferences 读取。
 * - 删除、新建、打开通过回调交给上层协调。
 */

private val PageHorizontalPadding = 24.dp
private val CardCornerRadius = 20.dp
private val CardInnerPadding = 16.dp
private val PageBackground = Color(0xFFFAFAFA)

@Composable
fun NoteListScreen(
    noteCount: Int,
    prefs: SharedPreferences,
    refreshKey: Int,
    selectedNoteId: Int,
    onSelectNote: (Int) -> Unit,
    onOpenNote: (Int, String, String, String) -> Unit,
    onCreateNote: () -> Unit
) {
    val notes = loadDisplayNotes(
        prefs = prefs,
        noteCount = noteCount,
        refreshKey = refreshKey
    )

    Box(
        modifier = Modifier.Companion
            .fillMaxSize()
            .background(PageBackground)
    ) {
        LazyColumn(
            modifier = Modifier.Companion
                .fillMaxSize()
                .padding(horizontal = PageHorizontalPadding),
            contentPadding = PaddingValues(top = 12.dp, bottom = 16.dp)
        ) {
            item {
                Text(
                    text = "全部笔记",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Companion.Bold,
                    color = Color(0xFF1C1C1E)
                )

                Text(
                    text = "${notes.size} 篇笔记",
                    fontSize = 14.sp,
                    color = Color(0xFF8E8E93),
                    modifier = Modifier.Companion.padding(top = 4.dp, bottom = 14.dp)
                )
            }

            if (notes.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.Companion.fillMaxWidth(),
                        shape = RoundedCornerShape(CardCornerRadius),
                        color = Color.Companion.White
                    ) {
                        Text(
                            text = "还没有笔记，点击右下角新建一条",
                            fontSize = 15.sp,
                            color = Color(0xFF6E6E73),
                            modifier = Modifier.Companion.padding(CardInnerPadding)
                        )
                    }
                }
            } else {
                item {
                    Surface(
                        modifier = Modifier.Companion.fillMaxWidth(),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(
                            CardCornerRadius
                        ),
                        color = Color.Companion.White
                    ) {
                        Column(
                            modifier = Modifier.Companion.fillMaxWidth()
                        ) {
                            notes.forEachIndexed { index, note ->
                                NoteItem(
                                    id = note.id,
                                    title = note.title,
                                    content = note.content,
                                    updatedTime = note.updatedTime,
                                    selectedNoteId = selectedNoteId,
                                    onOpenNote = onOpenNote,
                                    onSelectNote = onSelectNote
                                )

                                if (index < notes.lastIndex) {
                                    HorizontalDivider(
                                        color = Color(0xFFE5E5EA),
                                        thickness = 1.dp,
                                        modifier = Modifier.Companion.padding(start = if (selectedNoteId == 0) 16.dp else 54.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (selectedNoteId == 0) {
            FloatingActionButton(
                onClick = onCreateNote,
                modifier = Modifier.Companion
                    .align(Alignment.Companion.BottomEnd)
                    .padding(18.dp),
                containerColor = Color.Companion.White,
                contentColor = Color(0xFF1C1C1E),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(22.dp)
            ) {
                Text(
                    text = "+",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Companion.Light
                )
            }
        }

    }
}

private fun loadDisplayNotes(
    prefs: SharedPreferences,
    noteCount: Int,
    refreshKey: Int
): List<Note> {
    val timeFormat = SimpleDateFormat("yyyy/M/d HH:mm", Locale.getDefault())

    return getActiveNoteIds(prefs, noteCount)
        .map { id -> getNote(prefs, id) }
        .filter { note ->
            note.title.isNotBlank() || note.content.isNotBlank() || note.updatedTime.isNotBlank()
        }
        .sortedWith(
            compareByDescending { note ->
                runCatching {
                    timeFormat.parse(note.updatedTime)?.time
                }.getOrNull() ?: note.id.toLong()
            }
        )
        .also {
            refreshKey.hashCode()
        }
}
