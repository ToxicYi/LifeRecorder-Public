package com.jiacun.liferecorder.state

/**
 * NoteState.kt
 *
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
import com.jiacun.liferecorder.data.createNote
import com.jiacun.liferecorder.data.deleteNote
import com.jiacun.liferecorder.data.getActiveNoteIds
import com.jiacun.liferecorder.data.getNote
import com.jiacun.liferecorder.data.getNoteCount
import com.jiacun.liferecorder.data.getNotePrefs
import com.jiacun.liferecorder.data.saveNoteContent
import com.jiacun.liferecorder.data.saveNoteTitle

/**
 * NoteState
 *
 * 集中保存笔记相关状态，并封装新建、打开、删除、保存等笔记操作。
 * MainActivity 只负责调用这些函数并执行页面导航。
 */
@Stable
class NoteState(
    // 笔记底层存储使用的 SharedPreferences，由 rememberNoteState 创建并传入。
    val prefs: SharedPreferences
) {
    // 默认读取第 1 条笔记，用来初始化编辑页的标题、正文和更新时间。
    private val firstNote = getNote(prefs, 1)

    // 当前被长按选中的笔记 id；0 表示没有进入选择模式。
    var selectedNoteId by mutableIntStateOf(0)

    // 当前已创建过的笔记数量，用于生成下一条新笔记 id。
    var noteCount by mutableIntStateOf(getNoteCount(prefs))

    // 当前正在编辑的笔记 id，保存标题和正文时会用到。
    var currentNoteId by mutableIntStateOf(1)

    // 笔记列表刷新标记；新建或删除笔记后递增，触发列表重新读取。
    var listVersion by mutableIntStateOf(0)

    // 当前编辑页正文内容。
    var inputText by mutableStateOf(firstNote.content)

    // 当前编辑页标题内容。
    var titleText by mutableStateOf(firstNote.title)

    // 当前编辑页展示的更新时间。
    var updatedTime by mutableStateOf(firstNote.updatedTime)

    // 清空笔记列表的选择模式。
    fun clearSelection() {
        selectedNoteId = 0
    }

    // 选中指定笔记。
    fun selectNote(id: Int) {
        selectedNoteId = id
    }

    // 打开笔记时只更新状态，导航由 MainActivity 处理。
    fun openNote(id: Int) {
        // 从 SharedPreferences 读取目标笔记。
        val note = getNote(prefs, id)
        currentNoteId = note.id
        inputText = note.content
        titleText = note.title
        updatedTime = note.updatedTime
    }

    // 新建笔记时只创建和更新状态，导航由 MainActivity 处理。
    fun createNewNote() {
        // 下一条笔记 id 基于当前计数递增。
        val newId = noteCount + 1

        // 调用 NoteStorage 创建新笔记记录。
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
        titleText = newTitle
        updatedTime = saveNoteTitle(
            prefs = prefs,
            id = currentNoteId,
            title = newTitle
        )
    }

    // 保存正文并同步更新时间。
    fun saveContent(newText: String) {
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
