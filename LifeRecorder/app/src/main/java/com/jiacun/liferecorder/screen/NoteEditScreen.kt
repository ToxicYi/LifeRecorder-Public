package com.jiacun.liferecorder.screen

/**
 * NoteEditScreen
 *
 * 负责：
 * - 显示笔记编辑页面。
 * - 展示标题输入、正文输入和更新时间。
 * - 将用户编辑事件通过回调交给上层处理。
 *
 * 不负责：
 * - 不直接决定笔记 id。
 * - 不直接管理笔记列表、删除或选择模式。
 * - 不发起后端同步或 AI 总结。
 *
 * 数据来源：
 * - 当前笔记内容由 MainActivity 传入。
 * - 保存逻辑通过上层回调进入 NoteStorage。
 */

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val PageHorizontalPadding = 24.dp
private val CardCornerRadius = 20.dp
private val CardInnerPadding = 18.dp
private val PageBackground = Color(0xFFFAFAFA)

//编辑页面
@Composable
fun NoteEditScreen(
    titleText: String,
    inputText: String,
    updatedTime: String,
    onTitleChange: (String) -> Unit,
    onInputChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PageBackground)
            .padding(horizontal = PageHorizontalPadding)
            .padding(top = 12.dp, bottom = 16.dp)
    ) {
        BasicTextField(
            value = titleText,
            onValueChange = onTitleChange,
            textStyle = TextStyle(
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp)
        )

        Text(
            text = "${if (updatedTime.isBlank()) "未保存" else updatedTime} | ${inputText.length}字 | 未分类⌄",
            fontSize = 14.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(CardCornerRadius),
            color = Color.White
        ) {
            BasicTextField(
                value = inputText,
                onValueChange = onInputChange,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(CardInnerPadding)
            )
        }
    }
}
