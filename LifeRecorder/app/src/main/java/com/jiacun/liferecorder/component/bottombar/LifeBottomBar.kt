package com.jiacun.liferecorder.component.bottombar

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

//直接用于显示底部栏，显示组件
@Composable
fun LifeBottomBar(
    selectedNoteId: Int,
    currentPage: String,
    onPageChange: (String) -> Unit,
    onDeleteNote: (Int) -> Unit,
    onClearSelection: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (selectedNoteId == 0) {
        LifeNavigationBar(
            currentPage = currentPage,
            onPageChange = onPageChange,
            modifier = modifier
        )
    } else {
        SelectionBottomBar(
            selectedNoteId = selectedNoteId,
            onDeleteNote = onDeleteNote,
            onClearSelection = onClearSelection,
            modifier = modifier
        )
    }
}

