package me.deference.formsample

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

fun main() = application {
    val state = rememberWindowState(placement = WindowPlacement.Maximized)
    Window(
        state = state,
        onCloseRequest = ::exitApplication,
        title = "Formdoctorcmp",
    ) {
        App()
    }
}