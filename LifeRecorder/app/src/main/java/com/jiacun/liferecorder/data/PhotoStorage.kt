package com.jiacun.liferecorder.data

/**
 * PhotoStorage
 *
 * 通过 MediaStore 读取系统相册图片 Uri，供 PhotosScreen 显示图片网格。
 */

import android.content.Context
import android.net.Uri
import android.provider.MediaStore

fun loadAllImageUris(context: Context): List<Uri> {
    // 读取到的图片 Uri 列表。
    val imageUris = mutableListOf<Uri>()

    // 系统相册图片集合。
    val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

    // 只需要图片 id 和添加时间。
    val projection = arrayOf(
        MediaStore.Images.Media._ID,
        MediaStore.Images.Media.DATE_ADDED
    )

    // 最新图片排在前面。
    val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

    context.contentResolver.query(
        collection,
        projection,
        null,
        null,
        sortOrder
    )?.use { cursor ->
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)

        while (cursor.moveToNext()) {
            val id = cursor.getLong(idColumn)

            val uri = Uri.withAppendedPath(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                id.toString()
            )

            imageUris.add(uri)
        }
    }

    return imageUris
}
