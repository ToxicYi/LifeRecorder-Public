package com.jiacun.liferecorder.screen

/**
 * LifeRecorderScreen
 *
 * 负责：
 * - 保留旧版 currentPage 字符串导航时期的页面分发容器。
 * - 记录各主页面和笔记编辑页面之间的历史组织方式。
 *
 * 不负责：
 * - 当前不应继续新增业务逻辑。
 * - 不应继续承担 Navigation Compose 的新路由分发。
 * - 不应直接修改笔记保存、文件同步或网络协议。
 *
 * 数据来源：
 * - 新导航主流程已经迁移到 MainActivity 的 NavHost。
 * - 后续如清理旧代码，应先确认没有调用方再移除。
 */

import com.jiacun.liferecorder.feature.note.getActiveNoteIds

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.jiacun.liferecorder.feature.note.createNote
import com.jiacun.liferecorder.feature.note.getNote
import com.jiacun.liferecorder.feature.note.getNoteCount
import com.jiacun.liferecorder.feature.note.getNotePrefs
import com.jiacun.liferecorder.feature.note.saveNoteContent
import com.jiacun.liferecorder.feature.note.saveNoteTitle
import com.jiacun.liferecorder.feature.file.library.FileLibraryScreen
import com.jiacun.liferecorder.feature.mine.MineScreen
import com.jiacun.liferecorder.feature.note.NoteEditScreen
import com.jiacun.liferecorder.feature.note.NoteListScreen
import com.jiacun.liferecorder.feature.file.storage.FilesScreen
import com.jiacun.liferecorder.feature.photo.PhotosScreen
import com.jiacun.liferecorder.feature.recent.RecentScreen

@Composable
fun LifeRecorderScreen(
    modifier: Modifier = Modifier,
    currentPage: String,
    selectedNoteId: Int,
    onSelectedNoteChange: (Int) -> Unit,
    onPageChange: (String) -> Unit,
    onBottomBarHiddenChange: (Boolean) -> Unit = {},
    onStorageBackHandlerChange: (((() -> Unit)?) -> Unit)? = null
) {
    val context = LocalContext.current
    val prefs = getNotePrefs(context)

    var noteCount by remember {
        mutableIntStateOf(getNoteCount(prefs))
    }

    var currentNoteId by remember {
        mutableIntStateOf(1)
    }

    var listVersion by remember {
        mutableIntStateOf(0)
    }

    val firstNote = remember {
        getNote(prefs, 1)
    }

    var inputText by remember {
        mutableStateOf(firstNote.content)
    }

    var titleText by remember {
        mutableStateOf(firstNote.title)
    }

    var updatedTime by remember {
        mutableStateOf(firstNote.updatedTime)
    }

    val activeNoteCount = getActiveNoteIds(prefs, noteCount).size

    fun openNote(id: Int, noteText: String, noteTitle: String, noteUpdatedTime: String) {
        currentNoteId = id
        inputText = noteText
        titleText = noteTitle
        updatedTime = noteUpdatedTime
        onPageChange("edit")
    }

    fun createNewNote() {
        val newId = noteCount + 1
        val newNote = createNote(prefs, newId)

        noteCount = newId
        currentNoteId = newNote.id
        inputText = newNote.content
        titleText = newNote.title
        updatedTime = newNote.updatedTime

        // 新增：新建后强制刷新列表
        listVersion++

        onPageChange("edit")
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        if (currentPage == "recent") {
            RecentScreen(
                onOpenNote = { id ->
                    val note = getNote(prefs, id)
                    openNote(
                        id = note.id,
                        noteText = note.content,
                        noteTitle = note.title,
                        noteUpdatedTime = note.updatedTime
                    )
                }
            )
        } else if (currentPage == "folders") {
            FoldersScreen(
                noteCount = activeNoteCount,
                onOpenNotes = {
                    onPageChange("list")
                },
                onOpenPhotos = {
                    onPageChange("photos")
                },
                onOpenFiles = {
                    onPageChange("fileLibrary")
                },
                onOpenStorage = {
                    onPageChange("storage")
                },
                onOpenChat = {
                    onPageChange("chat")
                },
                onOpenMine = {
                    onPageChange("mine")
                }
            )
        } else if (currentPage == "photos") {
            PhotosScreen()
        } else if (currentPage == "fileLibrary") {
            FileLibraryScreen()
        } else if (currentPage == "storage") {
            FilesScreen(
                onNestedFolderChange = onBottomBarHiddenChange,
                onInternalBackHandlerChange = onStorageBackHandlerChange
            )
        } else if (currentPage == "chat") {
            ChatScreen()
        } else if (currentPage == "edit") {
            NoteEditScreen(
                titleText = titleText,
                inputText = inputText,
                updatedTime = updatedTime,
                onTitleChange = { newTitle ->
                    titleText = newTitle

                    // 保存标题并返回最新修改时间
                    updatedTime = saveNoteTitle(
                        prefs = prefs,
                        id = currentNoteId,
                        title = newTitle
                    )
                },
                onInputChange = { newText ->
                    inputText = newText

                    // 保存正文并返回最新修改时间
                    updatedTime = saveNoteContent(
                        prefs = prefs,
                        id = currentNoteId,
                        content = newText
                    )
                }
            )
        } else if (currentPage == "list") {
            NoteListScreen(
                noteCount = noteCount,
                prefs = prefs,
                refreshKey = listVersion,
                selectedNoteId = selectedNoteId,
                onSelectNote = { onSelectedNoteChange(it) },
                onOpenNote = { id, noteText, noteTitle, noteUpdatedTime ->
                    openNote(id, noteText, noteTitle, noteUpdatedTime)
                },
                onCreateNote = ::createNewNote
            )
        } else if (currentPage == "sync") {
            SyncScreen(
                currentTitle = titleText,
                currentContent = inputText
            )
        } else if (currentPage == "mine") {
            MineScreen()
        }
    }
}
