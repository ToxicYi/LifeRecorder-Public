package com.jiacun.liferecorder.feature.recent

/**
 * RecentScreen
 *
 * 负责：
 * - 显示“最近”首页内容。
 * - 展示搜索框占位和最近 30 天内的笔记、图片、文件记录。
 * - 按日期分组展示最近资源。
 *
 * 不负责：
 * - 不直接处理后端文件同步。
 * - 不执行 Agent 请求。
 * - 不保存文件索引或笔记内容。
 *
 * 数据来源：
 * - 最近记录由 data/RecentStorage.kt 聚合。
 * - 打开笔记通过 onOpenNote 回调交给上层导航。
 */

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val PageHorizontalPadding = 24.dp
private val CardCornerRadius = 22.dp
private val CardInnerPadding = 16.dp
private val PageBackground = Color(0xFFFAFAFA)

@Composable
fun RecentScreen(
    onOpenNote: (Int) -> Unit
) {
    val context = LocalContext.current

    var recentItems by remember {
        mutableStateOf<List<RecentItem>>(emptyList())
    }
    var isLoaded by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(Unit) {
        recentItems = withContext(Dispatchers.IO) {
            loadRecentItems(context, 30)
        }
        isLoaded = true
    }

    val groups = remember(recentItems) {
        recentItems
            .groupBy { item -> startOfDayMillis(item.timeMillis) }
            .toSortedMap(compareByDescending { it })
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(PageBackground)
            .padding(horizontal = PageHorizontalPadding),
        contentPadding = PaddingValues(top = 12.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SearchPlaceholder()
        }

        if (groups.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(CardCornerRadius),
                    color = Color.White
                ) {
                    Text(
                        text = if (isLoaded) "最近 30 天还没有记录" else "正在读取最近记录...",
                        fontSize = 15.sp,
                        color = Color(0xFF6E6E73),
                        modifier = Modifier.padding(CardInnerPadding)
                    )
                }
            }
        } else {
            groups.forEach { (dayMillis, items) ->
                item {
                    DateSection(
                        title = formatDateTitle(dayMillis),
                        items = items.sortedByDescending { it.timeMillis },
                        onOpenNote = onOpenNote
                    )
                }
            }
        }

        item {
            Text(
                text = "仅显示最近 30 天的文件",
                fontSize = 12.sp,
                color = Color(0xFF8E8E93),
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
    }
}

@Composable
private fun SearchPlaceholder() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color.White
    ) {
        Text(
            text = "搜索",
            fontSize = 15.sp,
            color = Color(0xFF8E8E93),
            modifier = Modifier.padding(horizontal = CardInnerPadding, vertical = 11.dp)
        )
    }
}

@Composable
private fun DateSection(
    title: String,
    items: List<RecentItem>,
    onOpenNote: (Int) -> Unit
) {
    Column {
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1C1C1E),
            modifier = Modifier.padding(start = 2.dp, bottom = 8.dp)
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(CardCornerRadius),
            color = Color.White
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                val images = items.filter { it.type == "image" }
                val otherItems = items.filterNot { it.type == "image" }

                if (images.isNotEmpty()) {
                    ImageStrip(images = images)
                }

                otherItems.forEachIndexed { index, item ->
                    if (images.isNotEmpty() || index > 0) {
                        HorizontalDivider(
                            color = Color(0xFFE5E5EA),
                            thickness = 1.dp,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }

                    when (item.type) {
                        "note" -> NoteRecentRow(
                            item = item,
                            onClick = {
                                item.id.toIntOrNull()?.let(onOpenNote)
                            }
                        )
                        "file" -> FileRecentRow(item = item)
                    }
                }
            }
        }
    }
}

@Composable
private fun ImageStrip(images: List<RecentItem>) {
    val visibleImages = images.take(6)
    val moreCount = images.size - visibleImages.size

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(CardInnerPadding)
    ) {
        SourceLabel(
            text = if (images.any { it.source == "截图" }) "截屏" else "相册"
        )

        Row(
            modifier = Modifier.padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            visibleImages.forEach { item ->
                Image(
                    painter = rememberAsyncImagePainter(Uri.parse(item.uriOrPath)),
                    contentDescription = item.name,
                    modifier = Modifier
                        .width(56.dp)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            if (moreCount > 0) {
                Surface(
                    modifier = Modifier
                        .width(56.dp)
                        .aspectRatio(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF2F3F5)
                ) {
                    Text(
                        text = "更多\n+$moreCount",
                        fontSize = 12.sp,
                        color = Color(0xFF6E6E73),
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun NoteRecentRow(
    item: RecentItem,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            SourceLabel(
                text = sourceLabelForItem(item),
                modifier = Modifier.padding(bottom = 7.dp)
            )

            Text(
                text = item.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1C1C1E)
            )
            Text(
                text = item.previewText?.ifBlank { "暂无内容" } ?: "暂无内容",
                fontSize = 13.sp,
                color = Color(0xFF6E6E73),
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = formatTime(item.timeMillis),
                fontSize = 12.sp,
                color = Color(0xFF8E8E93),
                modifier = Modifier.padding(top = 5.dp)
            )
        }
    }
}

@Composable
private fun FileRecentRow(item: RecentItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.InsertDriveFile,
            contentDescription = item.name,
            tint = Color(0xFF3A3A3C),
            modifier = Modifier.width(32.dp)
        )

        Column(
            modifier = Modifier.weight(1f)
        ) {
            SourceLabel(
                text = sourceLabelForItem(item),
                modifier = Modifier.padding(bottom = 7.dp)
            )

            Text(
                text = item.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF1C1C1E)
            )
            Text(
                text = item.sizeText ?: item.source,
                fontSize = 12.sp,
                color = Color(0xFF8E8E93),
                modifier = Modifier.padding(top = 3.dp)
            )
        }
    }
}

@Composable
private fun SourceLabel(
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = Color(0xFFF2F3F5)
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF6E6E73),
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
        )
    }
}

private fun sourceLabelForItem(item: RecentItem): String {
    return when (item.type) {
        "note" -> "LifeRecorder"
        "image" -> if (item.source == "截图") "截屏" else item.source.ifBlank { "相册" }
        "file" -> item.source.ifBlank { "文件来源" }
        else -> item.source.ifBlank { "应用来源" }
    }
}

private fun startOfDayMillis(timeMillis: Long): Long {
    return Calendar.getInstance().apply {
        timeInMillis = timeMillis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

private fun formatDateTitle(dayMillis: Long): String {
    val today = startOfDayMillis(System.currentTimeMillis())
    val yesterday = today - 24L * 60L * 60L * 1000L

    return when (dayMillis) {
        today -> "今天"
        yesterday -> "昨天"
        else -> SimpleDateFormat("M月d日", Locale.getDefault()).format(Date(dayMillis))
    }
}

private fun formatTime(timeMillis: Long): String {
    return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timeMillis))
}
