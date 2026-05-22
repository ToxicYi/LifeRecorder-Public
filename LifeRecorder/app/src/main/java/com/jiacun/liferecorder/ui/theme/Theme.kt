package com.jiacun.liferecorder.ui.theme

/**
 * Theme
 *
 * LifeRecorder 的 Compose MaterialTheme 入口，负责选择浅色/深色/动态色方案，
 * 并把统一的颜色和字体配置传给所有页面。
 */

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

// 深色模式下使用的 Material 颜色方案。
private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

// 浅色模式下使用的 Material 颜色方案。
private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)
@Composable
fun LifeRecorderTheme(
    // 是否使用深色主题，默认跟随系统。
    darkTheme: Boolean = isSystemInDarkTheme(),
    // 是否启用 Android 12+ 动态取色。
    dynamicColor: Boolean = true,
    // 被主题包裹的 App 内容。
    content: @Composable () -> Unit
) {
    // 最终传给 MaterialTheme 的颜色方案。
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            // Android 12+ 时可从系统壁纸提取动态色。
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
