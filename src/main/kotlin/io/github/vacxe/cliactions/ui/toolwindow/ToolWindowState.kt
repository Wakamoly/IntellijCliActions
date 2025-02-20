package io.github.vacxe.cliactions.ui.toolwindow

import  io.github.vacxe.cliactions.model.Group
sealed class ToolWindowState {
    data class Loading(val message: String) : ToolWindowState()
    data class Error(val message: String): ToolWindowState()
    data class Content(val contentTabItems: List<ContentTabItem>) : ToolWindowState()
}

data class ContentTabItem(
    val name: String,
    val messages: List<String>,
    val groups: List<Group>,
)
