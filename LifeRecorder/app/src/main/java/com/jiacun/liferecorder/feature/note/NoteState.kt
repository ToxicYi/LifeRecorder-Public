package com.jiacun.liferecorder.feature.note

/**
 * NoteState.kt
 * 笔记状态文件：集中保存当前笔记、选择模式、列表刷新标记，并封装新建/打开/删除/保存笔记操作。
 */

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
/**
 * NoteState
 * 集中保存笔记相关状态，并封装新建、打开、删除、保存等笔记操作。
 * MainActivity 只负责调用这些函数并执行页面导航。
 */
@Stable
class NoteState(
    // 笔记底层存储使用的 SharedPreferences，由 rememberNoteState 创建并传入。
    val prefs: SharedPreferences
) {
    // 当前被长按选中的笔记 id；0 表示没有进入选择模式。
    var selectedNoteId by mutableIntStateOf(0)

    // 当前已创建过的笔记数量，用于生成下一条新笔记 id。
    var noteCount by mutableIntStateOf(getNoteCount(prefs))

    // 当前正在编辑的笔记 id；0 表示当前没有打开任何笔记。
    var currentNoteId by mutableIntStateOf(0)

    // 笔记列表刷新标记；新建或删除笔记后递增，触发列表重新读取。
    var listVersion by mutableIntStateOf(0)

    // 当前编辑页正文内容；未打开笔记时为空。
    var inputText by mutableStateOf("")

    // 当前编辑页标题内容；未打开笔记时为空。
    var titleText by mutableStateOf("")

    // 当前编辑页展示的更新时间；未打开笔记时为空。
    var updatedTime by mutableStateOf("")

    // 清空笔记列表的选择模式。
    fun clearSelection() {
        selectedNoteId = 0
    }

    // 选中指定笔记。
    fun selectNote(id: Int) {
        selectedNoteId = id
    }

    // 打开笔记时读取目标笔记，并更新编辑页状态。
    fun openNote(id: Int) {
        val note = getNote(prefs, id)

        currentNoteId = note.id
        inputText = note.content
        titleText = note.title
        updatedTime = note.updatedTime
    }

    // 新建笔记，并将新笔记设置为当前编辑对象。
    fun createNewNote() {
        val newId = noteCount + 1
        val newNote = createNote(prefs, newId)

        noteCount = newId
        currentNoteId = newNote.id
        inputText = newNote.content
        titleText = newNote.title
        updatedTime = newNote.updatedTime
        listVersion++
    }

    // 删除笔记后刷新列表并退出选择模式。
    fun removeNote(id: Int) {
        deleteNote(prefs, id)
        selectedNoteId = 0
        listVersion++
    }

    // 保存标题并同步更新时间。
    fun saveTitle(newTitle: String) {
        // 没有打开笔记时，不允许保存到不存在的 note_0。
        if (currentNoteId == 0) return

        titleText = newTitle
        updatedTime = saveNoteTitle(
            prefs = prefs,
            id = currentNoteId,
            title = newTitle
        )
    }

    // 保存正文并同步更新时间。
    fun saveContent(newText: String) {
        // 没有打开笔记时，不允许保存到不存在的 note_0。
        if (currentNoteId == 0) return

        inputText = newText
        updatedTime = saveNoteContent(
            prefs = prefs,
            id = currentNoteId,
            content = newText
        )
    }

    // 返回未删除笔记数量，用于资源页展示。
    fun activeNoteCount(): Int {
        return getActiveNoteIds(prefs, noteCount).size
    }
}

@Composable
fun rememberNoteState(context: Context): NoteState {
    // 获取笔记使用的 SharedPreferences。
    val prefs = remember { getNotePrefs(context) }

    // 让笔记状态在重组时保持稳定。
    return remember(prefs) { NoteState(prefs) }
}
