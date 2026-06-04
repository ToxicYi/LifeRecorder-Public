package com.jiacun.liferecorder.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun LifeNavigationBar(
    currentPage: String,
    onPageChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val resourceSelected = currentPage == "folders" ||
            currentPage == "list" ||
            currentPage == "edit" ||
            currentPage == "photos" ||
            currentPage == "files" ||
            currentPage == "fileLibrary" ||
            currentPage == "storage" ||
            currentPage == "chat"

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(40.dp),
            color = Color.White.copy(alpha = 0.72f),
            shadowElevation = 12.dp,
            border = BorderStroke(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.75f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.18f))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                GlassNavButton(
                    icon = Icons.Outlined.Schedule,
                    text = "最近",
                    selected = currentPage == "recent",
                    onClick = {
                        onPageChange("recent")
                    }
                )

                GlassNavButton(
                    icon = Icons.Outlined.Folder,
                    text = "资源",
                    selected = resourceSelected,
                    onClick = {
                        onPageChange("folders")
                    }
                )

                GlassNavButton(
                    icon = Icons.Outlined.Sync,
                    text = "同步",
                    selected = currentPage == "sync",
                    onClick = {
                        onPageChange("sync")
                    }
                )

                GlassNavButton(
                    icon = Icons.Outlined.Person,
                    text = "我的",
                    selected = currentPage == "mine",
                    onClick = {
                        onPageChange("mine")
                    }
                )
            }
        }
    }
}

@Composable
private fun GlassNavButton(
    icon: ImageVector,
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val itemColor = if (selected) {
                Color(0xFF111111)
            } else {
                Color(0xFF777777)
            }

            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = itemColor,
                modifier = Modifier.size(22.dp)
            )

            Text(
                text = text,
                color = itemColor,
                fontWeight = if (selected) {
                    FontWeight.Bold
                } else {
                    FontWeight.Normal
                }
            )
        }
    }
}