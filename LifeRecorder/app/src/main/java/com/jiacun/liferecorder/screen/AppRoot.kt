package com.jiacun.liferecorder.screen

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
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
import com.jiacun.liferecorder.state.rememberNoteState
import com.jiacun.liferecorder.navigation.openNoteAndNavigate
import com.jiacun.liferecorder.navigation.createNoteAndNavigate
import com.jiacun.liferecorder.navigation.navigateRoot

//是所有Screen的框架
@Composable
fun AppRoot() {
    // Navigation Compose 的页面控制器。
    val navController = rememberNavController()

    // 当前导航栈顶部条目，用于获取当前页面 route。
    val backStackEntry by navController.currentBackStackEntryAsState()

    // 当前导航栈顶部条目的route；为空时默认最近页。
    val currentRoute = backStackEntry?.destination?.route ?: LifeRoute.Recent

    // 当前 Android 上下文，供状态对象读取本地存储并且可以使用其他系统能力。
    val context = LocalContext.current

    // 打包操作笔记和笔记外挂内容的方法。
    val noteState = rememberNoteState(context)

    // 离开笔记列表时自动退出选择模式。
    LaunchedEffect(currentRoute) {
        if (currentRoute != LifeRoute.NoteList) {
            noteState.clearSelection()
        }
    }

    // 全局页面框架：顶部栏、底部栏和中间 NavHost 内容区。
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
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
            }
        ) { innerPanding ->//传入算好的
            // 页面内容区使用 innerPadding，避免被顶部栏和底部栏遮挡。
            NavHost(
                navController = navController,
                startDestination = LifeRoute.Recent,//默认界面
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = innerPanding.calculateTopPadding()),
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
            // 点击最近笔记进入编辑页。
            composable(LifeRoute.Recent) {
                RecentScreen(
                    onOpenNote = { id ->  openNoteAndNavigate(noteState, navController, id) }
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
                    onOpenNote = { id, _, _, _ ->  openNoteAndNavigate(noteState, navController, id) },
                    onDeleteNote = { id ->
                        noteState.removeNote(id)
                    },
                    onCreateNote = {
                        createNoteAndNavigate(noteState, navController)
                    }
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

        if (shouldShowBottomBar(currentRoute)) {
            LifeBottomBar(
                modifier = Modifier.align(Alignment.BottomCenter),
                selectedNoteId = noteState.selectedNoteId,
                currentPage = currentRoute,
                onPageChange = { route ->
                    when (route) {
                        LifeRoute.Recent -> navigateRoot(noteState, navController, LifeRoute.Recent)
                        LifeRoute.Resources -> navigateRoot(noteState, navController, LifeRoute.Resources)
                        LifeRoute.Sync -> navigateRoot(noteState, navController, LifeRoute.Sync)
                        LifeRoute.Mine -> navigateRoot(noteState, navController, LifeRoute.Mine)
                        else -> navController.navigate(route)
                    }
                }
            )
        }
    }
}


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
