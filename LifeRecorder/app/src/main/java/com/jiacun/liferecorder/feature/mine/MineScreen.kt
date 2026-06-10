package com.jiacun.liferecorder.feature.mine

/**
 * MineScreen
 *
 * 负责：
 * - 显示“我的”页面的个人资料和设置占位内容。
 * - 承载未来账号、偏好、个人信息等入口。
 *
 * 不负责：
 * - 不处理服务器地址、同步测试或文件同步。
 * - 不保存笔记、文件或 AI 会话数据。
 * - 不直接访问后端接口。
 *
 * 数据来源：
 * - 当前主要是静态 UI。
 * - 同步相关设置由 SyncScreen 负责。
 */

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MineScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF2F2F7))
            .padding(horizontal = 16.dp)
            .padding(top = 12.dp, bottom = 16.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier.padding(18.dp)
            ) {
                Text(
                    text = "这里以后放个人资料、偏好设置和账号相关内容。",
                    fontSize = 14.sp,
                    color = Color(0xFF6E6E73),
                )
            }
        }
    }
}
