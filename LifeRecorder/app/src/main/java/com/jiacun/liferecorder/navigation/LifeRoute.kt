package com.jiacun.liferecorder.navigation

/**
 * LifeRoute
 *
 * 集中定义 Navigation Compose 使用的页面 route，避免页面跳转时到处写硬编码字符串。
 */
object LifeRoute {
    // 最近页，App 默认首页。
    const val Recent = "recent"
    // 资源入口页，对应原 folders 页面。
    const val Resources = "folders"
    // 全部笔记列表页。
    const val NoteList = "list"
    // 笔记编辑页。
    const val NoteEdit = "edit"
    // 系统相册浏览页。
    const val Photos = "photos"
    // LifeRecorder 自己的文件库页面。
    const val FileLibrary = "fileLibrary"
    // 手机公共存储浏览页面。
    const val Storage = "storage"
    // AI/Agent 聊天页面。
    const val Chat = "chat"
    // 同步与设置页面。
    const val Sync = "sync"
    // 我的页面。
    const val Mine = "mine"
}
