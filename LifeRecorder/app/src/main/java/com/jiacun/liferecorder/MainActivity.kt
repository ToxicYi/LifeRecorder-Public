package com.jiacun.liferecorder

/**
 * MainActivity.kt
 *
 * App 主入口文件：负责窗口初始化、全局 Scaffold、顶部栏/底部栏、Navigation Compose 路由和页面切换动画。
 */

import android.graphics.Color as AndroidColor
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.jiacun.liferecorder.component.LifeBottomBar
import com.jiacun.liferecorder.component.LifeTopBar
import com.jiacun.liferecorder.navigation.LifeRoute
import com.jiacun.liferecorder.screen.ChatScreen
import com.jiacun.liferecorder.screen.FileLibraryScreen
import com.jiacun.liferecorder.screen.FilesScreen
import com.jiacun.liferecorder.screen.FoldersScreen
import com.jiacun.liferecorder.screen.MineScreen
import com.jiacun.liferecorder.screen.NoteEditScreen
import com.jiacun.liferecorder.screen.NoteListScreen
import com.jiacun.liferecorder.screen.PhotosScreen
import com.jiacun.liferecorder.screen.RecentScreen
import com.jiacun.liferecorder.screen.SyncScreen
import com.jiacun.liferecorder.state.rememberNoteState
import com.jiacun.liferecorder.ui.theme.LifeRecorderTheme

/**
 * MainActivity
 *
 * App 主入口，负责初始化窗口、创建 Compose 根布局、配置全局导航、
 * 顶部栏、底部栏和页面切换动画。
 */
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
                // Navigation Compose 的页面控制器。
                val navController = rememberNavController()

                // 当前导航栈条目，用于获取当前页面 route。
                val backStackEntry by navController.currentBackStackEntryAsState()

                // 当前页面 route；为空时默认最近页。
                val currentRoute = backStackEntry?.destination?.route ?: LifeRoute.Recent

                // 当前 Android 上下文，供状态对象读取本地存储。
                val context = LocalContext.current

                // 笔记状态统一交给 NoteState，MainActivity 只负责导航协调。
                val noteState = rememberNoteState(context)

                // 离开笔记列表时自动退出选择模式。
                LaunchedEffect(currentRoute) {
                    if (currentRoute != LifeRoute.NoteList) {
                        noteState.clearSelection()
                    }
                }

                fun openNote(id: Int) {
                    // 打开笔记后进入编辑页。
                    noteState.openNote(id)
                    navController.navigate(LifeRoute.NoteEdit)
                }

                fun createNewNote() {
                    // 新建笔记后进入编辑页。
                    noteState.createNewNote()
                    navController.navigate(LifeRoute.NoteEdit)
                }

                // 切换底部一级页面，并避免重复压入同一个根页面。
                fun navigateRoot(route: String) {
                    navController.navigate(route) {
                        popUpTo(LifeRoute.Recent) {
                            inclusive = false
                        }
                        launchSingleTop = true
                    }
                    noteState.clearSelection()
                }

                // 全局页面框架：顶部栏、底部栏和中间 NavHost 内容区。
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize(),
                    containerColor = Color(0xFFF2F2F7),
                    topBar = {
                        // 全局顶部栏，标题和返回按钮根据当前 route 改变。
                        LifeTopBar(
                            currentPage = currentRoute,
                            selectedNoteId = noteState.selectedNoteId,
                            onCancelSelection = {
                                noteState.clearSelection()
                            },
                            onBackToList = {
                                navController.popBackStack()
                            },
                            onBackToFolders = {
                                navController.popBackStack()
                            }
                        )
                    },
                    bottomBar = {
                        // 底部栏只在一级页面显示，二级页面隐藏。
                        if (shouldShowBottomBar(currentRoute)) {
                            LifeBottomBar(
                                selectedNoteId = noteState.selectedNoteId,
                                currentPage = currentRoute,
                                onPageChange = { route ->
                                    // 底部栏点击根页面时走 navigateRoot，保持返回栈干净。
                                    when (route) {
                                        LifeRoute.Recent -> navigateRoot(LifeRoute.Recent)
                                        LifeRoute.Resources -> navigateRoot(LifeRoute.Resources)
                                        LifeRoute.Sync -> navigateRoot(LifeRoute.Sync)
                                        LifeRoute.Mine -> navigateRoot(LifeRoute.Mine)
                                        else -> navController.navigate(route)
                                    }
                                }
                            )
                        }
                    }
                ) { innerPadding ->
                    // 页面内容区使用 innerPadding，避免被顶部栏和底部栏遮挡。
                    NavHost(
                        navController = navController,
                        startDestination = LifeRoute.Recent,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        enterTransition = {
                            // 页面进入动画：根 Tab 间淡入，进入更深页面时从右侧滑入。
                            val duration = 260
                            val initialRoute = initialState.destination.route
                            val targetRoute = targetState.destination.route

                            if (isRootTab(initialRoute) && isRootTab(targetRoute)) {
                                fadeIn(tween(80))
                            } else if (pageDepth(targetRoute) > pageDepth(initialRoute)) {
                                slideInHorizontally(tween(duration)) { fullWidth -> fullWidth } + fadeIn(tween(duration))
                            } else {
                                slideInHorizontally(tween(duration)) { fullWidth -> -fullWidth / 3 } + fadeIn(tween(duration))
                            }
                        },
                        exitTransition = {
                            // 页面退出动画：前进时旧页面向左退，返回时旧页面向右退出。
                            val duration = 260
                            val initialRoute = initialState.destination.route
                            val targetRoute = targetState.destination.route

                            if (isRootTab(initialRoute) && isRootTab(targetRoute)) {
                                fadeOut(tween(80))
                            } else if (pageDepth(targetRoute) > pageDepth(initialRoute)) {
                                slideOutHorizontally(tween(duration)) { fullWidth -> -fullWidth / 3 } + fadeOut(tween(duration))
                            } else {
                                slideOutHorizontally(tween(duration)) { fullWidth -> fullWidth } + fadeOut(tween(duration))
                            }
                        },
                        popEnterTransition = {
                            // 系统返回或 popBackStack 时，新露出的页面进入动画。
                            val initialRoute = initialState.destination.route
                            val targetRoute = targetState.destination.route

                            if (isRootTab(initialRoute) && isRootTab(targetRoute)) {
                                fadeIn(tween(80))
                            } else {
                                slideInHorizontally(tween(260)) { fullWidth -> -fullWidth / 3 } + fadeIn(tween(260))
                            }
                        },
                        popExitTransition = {
                            // 系统返回或 popBackStack 时，当前页面退出动画。
                            val initialRoute = initialState.destination.route
                            val targetRoute = targetState.destination.route

                            if (isRootTab(initialRoute) && isRootTab(targetRoute)) {
                                fadeOut(tween(80))
                            } else {
                                slideOutHorizontally(tween(260)) { fullWidth -> fullWidth } + fadeOut(tween(260))
                            }
                        }
                    ) {
                        // 最近页，允许点击最近笔记进入编辑页。
                        composable(LifeRoute.Recent) {
                            RecentScreen(
                                onOpenNote = { id -> openNote(id) }
                            )
                        }

                        // 资源页，集中跳转到笔记、相册、文件、存储、AI 和我的页面。
                        composable(LifeRoute.Resources) {
                            FoldersScreen(
                                noteCount = noteState.activeNoteCount(),
                                onOpenNotes = {
                                    navController.navigate(LifeRoute.NoteList)
                                },
                                onOpenPhotos = {
                                    navController.navigate(LifeRoute.Photos)
                                },
                                onOpenFiles = {
                                    navController.navigate(LifeRoute.FileLibrary)
                                },
                                onOpenStorage = {
                                    navController.navigate(LifeRoute.Storage)
                                },
                                onOpenChat = {
                                    navController.navigate(LifeRoute.Chat)
                                },
                                onOpenMine = {
                                    navController.navigate(LifeRoute.Mine)
                                }
                            )
                        }

                        // 全部笔记列表页，笔记数据和选择状态来自 NoteState。
                        composable(LifeRoute.NoteList) {
                            NoteListScreen(
                                noteCount = noteState.noteCount,
                                prefs = noteState.prefs,
                                refreshKey = noteState.listVersion,
                                selectedNoteId = noteState.selectedNoteId,
                                onSelectNote = { noteState.selectNote(it) },
                                onClearSelection = { noteState.clearSelection() },
                                onOpenNote = { id, _, _, _ -> openNote(id) },
                                onDeleteNote = { id ->
                                    noteState.removeNote(id)
                                },
                                onCreateNote = ::createNewNote
                            )
                        }

                        // 笔记编辑页，输入变化通过 NoteState 保存到 NoteStorage。
                        composable(LifeRoute.NoteEdit) {
                            NoteEditScreen(
                                titleText = noteState.titleText,
                                inputText = noteState.inputText,
                                updatedTime = noteState.updatedTime,
                                onTitleChange = { newTitle ->
                                    noteState.saveTitle(newTitle)
                                },
                                onInputChange = { newText ->
                                    noteState.saveContent(newText)
                                }
                            )
                        }

                        // 系统相册浏览页。
                        composable(LifeRoute.Photos) {
                            PhotosScreen()
                        }

                        // LifeRecorder 自己的文件库页面。
                        composable(LifeRoute.FileLibrary) {
                            FileLibraryScreen()
                        }

                        // 手机公共存储浏览页面。
                        composable(LifeRoute.Storage) {
                            FilesScreen()
                        }

                        // AI/Agent 聊天页面。
                        composable(LifeRoute.Chat) {
                            ChatScreen()
                        }

                        // 同步与设置页面，接收当前笔记内容用于上传当前笔记。
                        composable(LifeRoute.Sync) {
                            SyncScreen(
                                currentTitle = noteState.titleText,
                                currentContent = noteState.inputText
                            )
                        }

                        // 我的页面。
                        composable(LifeRoute.Mine) {
                            MineScreen()
                        }
                    }
                }
            }
        }
    }
}

// 返回页面层级，用于判断页面切换动画方向。
private fun pageDepth(route: String?): Int {
    return when (route) {
        LifeRoute.Recent -> 0
        LifeRoute.Resources -> 1
        LifeRoute.NoteList,
        LifeRoute.Photos,
        LifeRoute.FileLibrary,
        LifeRoute.Storage,
        LifeRoute.Chat,
        LifeRoute.Sync,
        LifeRoute.Mine -> 2
        LifeRoute.NoteEdit -> 3
        else -> 0
    }
}

// 判断 route 是否属于底部一级 Tab 页面。
private fun isRootTab(route: String?): Boolean {
    return route == LifeRoute.Recent ||
        route == LifeRoute.Resources ||
        route == LifeRoute.Sync ||
        route == LifeRoute.Mine
}

// 判断当前页面是否显示底部栏。
private fun shouldShowBottomBar(route: String?): Boolean {
    return route == LifeRoute.Recent ||
        route == LifeRoute.Resources ||
        route == LifeRoute.Sync ||
        route == LifeRoute.Mine
}
