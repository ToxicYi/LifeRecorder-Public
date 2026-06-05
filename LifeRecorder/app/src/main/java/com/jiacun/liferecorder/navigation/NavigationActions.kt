package com.jiacun.liferecorder.navigation

import androidx.navigation.NavController
import com.jiacun.liferecorder.feature.note.NoteState

fun openNoteAndNavigate(
    noteState: NoteState,
    navController: NavController,
    id: Int
) {
    noteState.openNote(id)
    navController.navigate(LifeRoute.NoteEdit)
}

fun createNoteAndNavigate(
    noteState: NoteState,
    navController: NavController
) {
    noteState.createNewNote()
    navController.navigate(LifeRoute.NoteEdit)
}

fun navigateRoot(
    noteState: NoteState,
    navController: NavController,
    route: String
) {
    navController.navigate(route) {
        popUpTo(LifeRoute.Recent) {
            inclusive = false
        }
        launchSingleTop = true
    }
    noteState.clearSelection()
}
