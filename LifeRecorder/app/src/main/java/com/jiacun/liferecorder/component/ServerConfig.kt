package com.jiacun.liferecorder.data

/**
 * ServerConfig
 *
 * 管理 Android App 保存的 Spring Boot 后端地址，提供默认地址读取和用户手动保存能力。
 */

import android.content.Context

// SharedPreferences 文件名，专门保存服务器配置。
private const val CONFIG_PREFS_NAME = "server_config"

// 服务器地址在 SharedPreferences 中使用的 key。
private const val KEY_BASE_URL = "base_url"

// 旧版本内置的 Cloudflare 默认地址，用于迁移到新的模拟器默认地址。
private const val LEGACY_DEFAULT_BASE_URL = "https://homeless-asking-dryer-regions.trycloudflare.com"

// 默认服务器地址：Android 模拟器访问电脑本机 localhost 使用 10.0.2.2
const val DEFAULT_BASE_URL = "http://10.0.2.2:8080"

fun getServerBaseUrl(context: Context): String {
    // 读取用户保存过的服务器地址。
    val prefs = context.getSharedPreferences(CONFIG_PREFS_NAME, Context.MODE_PRIVATE)
    val savedUrl = prefs.getString(KEY_BASE_URL, null)?.trim()

    return when {
        savedUrl.isNullOrBlank() -> DEFAULT_BASE_URL
        savedUrl == LEGACY_DEFAULT_BASE_URL -> DEFAULT_BASE_URL
        else -> savedUrl
    }
}

fun saveServerBaseUrl(context: Context, baseUrl: String) {
    // 保存前去掉首尾空格和末尾斜杠，避免拼接接口路径时出现双斜杠。
    val cleanUrl = baseUrl.trim().removeSuffix("/")

    context.getSharedPreferences(CONFIG_PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString(KEY_BASE_URL, cleanUrl)
        .apply()
}
