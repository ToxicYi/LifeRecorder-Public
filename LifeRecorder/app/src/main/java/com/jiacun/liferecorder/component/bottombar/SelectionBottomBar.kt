package com.jiacun.liferecorder.component.bottombar

/**
 * SelectionBottomBar
 *
 * 笔记列表进入选择模式后显示的底部操作栏，目前提供删除和更多操作入口。
 */

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.ui.unit.dp

@Composable
fun SelectionBottomBar(
    // 当前被选中的笔记 id。
    selectedNoteId: Int,
    // 删除当前选中笔记。
    onDeleteNote: (Int) -> Unit,
    // 清空选择模式。
    onClearSelection: () -> Unit,
    // 外部传入的布局修饰符。
    modifier: Modifier = Modifier
) {
    GlassBottomBarContainer(
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassNavButton(
                icon = Icons.Outlined.Delete,
                text = "删除",
                selected = false,
                onClick = {
                    onDeleteNote(selectedNoteId)
                    onClearSelection()
                }
            )

            GlassNavButton(
                icon = Icons.Outlined.MoreHoriz,
                text = "更多",
                selected = false,
                onClick = {
                    // TODO：后面放置顶、标签、移动、导出等功能
                }
            )
        }
    }
}
