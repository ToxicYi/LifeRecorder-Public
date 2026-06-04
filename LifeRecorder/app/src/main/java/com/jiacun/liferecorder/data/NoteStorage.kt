package com.jiacun.liferecorder.data

/**
 * NoteStorage
 *
 * 使用 SharedPreferences 保存 LifeRecorder 的本地笔记数据，包括标题、正文、
 * 更新时间、删除标记和笔记计数。
 */

import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// 笔记数据结构
data class Note(
    // 笔记 id。
    val id: Int,
    // 笔记标题。
    val title: String,
    // 笔记正文。
    val content: String,
    // 笔记更新时间文本。
    val updatedTime: String
)

// 获取本地笔记存储
fun getNotePrefs(context: Context): SharedPreferences {
    return context.getSharedPreferences("life_data", Context.MODE_PRIVATE)
}

// 获取当前时间
fun nowTime(): String {
    return SimpleDateFormat("yyyy/M/d HH:mm", Locale.getDefault()).format(Date())
}

// 读取笔记总数
fun getNoteCount(prefs: SharedPreferences): Int {
    return prefs.getInt("note_count", 0)
}

// 读取单篇笔记
fun getNote(prefs: SharedPreferences, id: Int): Note {
    return Note(
        id = id,
        title = prefs.getString("note_${id}_title", "") ?: "",
        content = prefs.getString("note_$id", "") ?: "",
        updatedTime = prefs.getString("note_${id}_updated_time", "") ?: ""
    )
}


// 获取未删除笔记 id
fun getActiveNoteIds(prefs: SharedPreferences, noteCount: Int): List<Int> {
    return (1..noteCount).filter { id ->
        !prefs.getBoolean("note_${id}_deleted", false)
    }
}

// 保存标题
fun saveNoteTitle(prefs: SharedPreferences, id: Int, title: String): String {
    // 保存标题时生成新的更新时间。
    val time = nowTime()

    prefs.edit()
        .putString("note_${id}_title", title)
        .putString("note_${id}_updated_time", time)
        .apply()

    return time
}

// 保存正文
fun saveNoteContent(prefs: SharedPreferences, id: Int, content: String): String {
    // 保存正文时生成新的更新时间。
    val time = nowTime()

    prefs.edit()
        .putString("note_$id", content)
        .putString("note_${id}_updated_time", time)
        .apply()

    return time
}

// 新建笔记
fun createNote(prefs: SharedPreferences, newId: Int): Note {
    // 新建笔记的创建时间和更新时间相同。
    val time = nowTime()

    prefs.edit()
        .putInt("note_count", newId)
        .putString("note_$newId", "")
        .putString("note_${newId}_title", "")
        .putString("note_${newId}_created_time", time)
        .putString("note_${newId}_updated_time", time)
        .commit() // 修改：立刻保存，避免新建后列表读不到

    return Note(
        id = newId,
        title = "",
        content = "",
        updatedTime = time
    )
}

// 隐藏式删除笔记
fun deleteNote(prefs: SharedPreferences, id: Int) {
    prefs.edit()
        .putBoolean("note_${id}_deleted", true)
        .apply()
}
