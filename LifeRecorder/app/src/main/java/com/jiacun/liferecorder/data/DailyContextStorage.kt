package com.jiacun.liferecorder.data

/**
 * DailyContextStorage
 *
 * 采集和保存 Daily Context 所需的本地信息，包括城市/国家、日期、时区、电量、
 * 网络类型、设备名称和 App 版本。
 */

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class DailyContextRequest(
    // 当前日期，格式 yyyy-MM-dd。
    val date: String,
    // 当前系统时区，例如 Asia/Shanghai。
    val timezone: String,
    // 用户在同步页填写的城市。
    val city: String?,
    // 用户在同步页填写的国家。
    val country: String?,
    // 当前电量百分比。
    val batteryPercent: Int?,
    // 当前是否正在充电。
    val isCharging: Boolean?,
    // 当前网络类型，WiFi / Mobile / Unknown。
    val networkType: String?,
    // 设备品牌和型号。
    val deviceName: String?,
    // Android 系统版本。
    val androidVersion: String?,
    // 当前 App 版本。
    val appVersion: String?,
    // 首次创建时间戳。
    val createdTime: Long,
    // 本次更新时间戳。
    val updatedTime: Long
)

data class DeviceContextSnapshot(
    // 当前日期。
    val date: String,
    // 当前时区。
    val timezone: String,
    // 当前电量百分比。
    val batteryPercent: Int?,
    // 当前是否充电。
    val isCharging: Boolean?,
    // 当前网络类型。
    val networkType: String,
    // 当前设备名称。
    val deviceName: String,
    // 当前 Android 版本。
    val androidVersion: String,
    // 当前 App 版本。
    val appVersion: String?,
    // 采集时刻时间戳。
    val timeMillis: Long
)

// 保存 Daily Context 城市/国家配置的 SharedPreferences 文件名。
private const val DAILY_CONTEXT_PREFS = "daily_context_config"
// 城市字段 key。
private const val KEY_CITY = "city"
// 国家字段 key。
private const val KEY_COUNTRY = "country"

fun getDailyContextCity(context: Context): String {
    return context.getSharedPreferences(DAILY_CONTEXT_PREFS, Context.MODE_PRIVATE)
        .getString(KEY_CITY, "")
        .orEmpty()
}

fun getDailyContextCountry(context: Context): String {
    return context.getSharedPreferences(DAILY_CONTEXT_PREFS, Context.MODE_PRIVATE)
        .getString(KEY_COUNTRY, "")
        .orEmpty()
}

fun saveDailyContextLocation(context: Context, city: String, country: String) {
    context.getSharedPreferences(DAILY_CONTEXT_PREFS, Context.MODE_PRIVATE)
        .edit()
        .putString(KEY_CITY, city.trim())
        .putString(KEY_COUNTRY, country.trim())
        .apply()
}

fun collectDeviceContextSnapshot(context: Context): DeviceContextSnapshot {
    val now = System.currentTimeMillis()

    return DeviceContextSnapshot(
        date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(now)),
        timezone = TimeZone.getDefault().id,
        batteryPercent = readBatteryPercent(context),
        isCharging = readIsCharging(context),
        networkType = readNetworkType(context),
        deviceName = buildDeviceName(),
        androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
        appVersion = readAppVersion(context),
        timeMillis = now
    )
}

fun buildDailyContextRequest(
    context: Context,
    city: String,
    country: String
): DailyContextRequest {
    val snapshot = collectDeviceContextSnapshot(context)

    return DailyContextRequest(
        date = snapshot.date,
        timezone = snapshot.timezone,
        city = city.trim().ifBlank { null },
        country = country.trim().ifBlank { null },
        batteryPercent = snapshot.batteryPercent,
        isCharging = snapshot.isCharging,
        networkType = snapshot.networkType,
        deviceName = snapshot.deviceName,
        androidVersion = snapshot.androidVersion,
        appVersion = snapshot.appVersion,
        createdTime = snapshot.timeMillis,
        updatedTime = snapshot.timeMillis
    )
}

private fun readBatteryPercent(context: Context): Int? {
    val batteryIntent = context.registerReceiver(
        null,
        IntentFilter(Intent.ACTION_BATTERY_CHANGED)
    ) ?: return null

    val level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
    val scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)

    return if (level >= 0 && scale > 0) {
        (level * 100 / scale.toFloat()).toInt()
    } else {
        null
    }
}

private fun readIsCharging(context: Context): Boolean? {
    val batteryIntent = context.registerReceiver(
        null,
        IntentFilter(Intent.ACTION_BATTERY_CHANGED)
    ) ?: return null

    return when (batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)) {
        BatteryManager.BATTERY_STATUS_CHARGING,
        BatteryManager.BATTERY_STATUS_FULL -> true
        BatteryManager.BATTERY_STATUS_DISCHARGING,
        BatteryManager.BATTERY_STATUS_NOT_CHARGING -> false
        else -> null
    }
}

private fun readNetworkType(context: Context): String {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        ?: return "Unknown"
    val activeNetwork = connectivityManager.activeNetwork ?: return "Unknown"
    val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return "Unknown"

    return when {
        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WiFi"
        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Mobile"
        else -> "Unknown"
    }
}

private fun buildDeviceName(): String {
    val manufacturer = Build.MANUFACTURER.orEmpty().trim()
    val model = Build.MODEL.orEmpty().trim()

    return when {
        manufacturer.isBlank() -> model.ifBlank { "Android Device" }
        model.startsWith(manufacturer, ignoreCase = true) -> model
        else -> "$manufacturer $model"
    }
}

private fun readAppVersion(context: Context): String? {
    return try {
        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(
                context.packageName,
                android.content.pm.PackageManager.PackageInfoFlags.of(0)
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0)
        }

        packageInfo.versionName
    } catch (e: Exception) {
        null
    }
}
