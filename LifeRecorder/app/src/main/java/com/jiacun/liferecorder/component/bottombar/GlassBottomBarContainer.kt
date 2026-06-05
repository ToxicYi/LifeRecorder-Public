package com.jiacun.liferecorder.component.bottombar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jiacun.liferecorder.ui.theme.GlassBarBorderColor
import com.jiacun.liferecorder.ui.theme.GlassBarColor
import com.jiacun.liferecorder.ui.theme.GlassBarCornerRadius
import com.jiacun.liferecorder.ui.theme.GlassBarHeight
import com.jiacun.liferecorder.ui.theme.GlassBarHorizontalPadding
import com.jiacun.liferecorder.ui.theme.GlassBarShadowElevation
import com.jiacun.liferecorder.ui.theme.GlassBarVerticalPadding
//负责统一的玻璃外壳
@Composable
fun GlassBottomBarContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = GlassBarHorizontalPadding,
                vertical = GlassBarVerticalPadding
            ),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(GlassBarHeight),
            shape = RoundedCornerShape(GlassBarCornerRadius),
            color = GlassBarColor,
            shadowElevation = GlassBarShadowElevation,
            border = BorderStroke(
                width = 1.dp,
                color = GlassBarBorderColor
            )
        ) {
            content()
        }
    }
}