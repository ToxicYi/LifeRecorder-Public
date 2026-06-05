package com.jiacun.liferecorder.component.bottombar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jiacun.liferecorder.navigation.LifeRoute

@Composable
fun LifeNavigationBar(
    currentPage: String,
    onPageChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val resourceSelected = currentPage == LifeRoute.Resources ||
            currentPage == LifeRoute.NoteList ||
            currentPage == LifeRoute.NoteEdit ||
            currentPage == LifeRoute.Photos ||
            currentPage == LifeRoute.FileLibrary ||
            currentPage == LifeRoute.Storage ||
            currentPage == LifeRoute.Chat

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
                icon = Icons.Outlined.Schedule,
                text = "最近",
                selected = currentPage == LifeRoute.Recent,
                onClick = {
                    onPageChange(LifeRoute.Recent)
                }
            )

            GlassNavButton(
                icon = Icons.Outlined.Folder,
                text = "资源",
                selected = resourceSelected,
                onClick = {
                    onPageChange(LifeRoute.Resources)
                }
            )

            GlassNavButton(
                icon = Icons.Outlined.Sync,
                text = "同步",
                selected = currentPage == LifeRoute.Sync,
                onClick = {
                    onPageChange(LifeRoute.Sync)
                }
            )

            GlassNavButton(
                icon = Icons.Outlined.Person,
                text = "我的",
                selected = currentPage == LifeRoute.Mine,
                onClick = {
                    onPageChange(LifeRoute.Mine)
                }
            )
        }
    }
}