package com.jiacun.liferecorder

import android.graphics.Color as AndroidColor
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.jiacun.liferecorder.screen.AppRoot
import com.jiacun.liferecorder.ui.theme.LifeRecorderTheme

class MainActivity : ComponentActivity() {
    // Android Activity 启动入口。
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 启用边缘到边缘布局，让状态栏和导航栏透明。
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                lightScrim = AndroidColor.TRANSPARENT,
                darkScrim = AndroidColor.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.auto(
                lightScrim = AndroidColor.TRANSPARENT,
                darkScrim = AndroidColor.TRANSPARENT
            )
        )

        // Android 10 及以上关闭系统导航栏强制对比背景。
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        // 创建 Compose UI 根节点。
        setContent {
            LifeRecorderTheme {
                AppRoot()
            }
        }
    }
}
