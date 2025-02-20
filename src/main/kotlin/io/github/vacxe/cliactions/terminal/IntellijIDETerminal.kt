package io.github.vacxe.cliactions.terminal

import com.intellij.openapi.project.Project
import com.intellij.terminal.ui.TerminalWidget
import org.jetbrains.plugins.terminal.TerminalToolWindowManager
import java.io.IOException

class IntellijIDETerminal(private val project: Project) : Terminal {
    override fun run(
        name: String,
        command: String,
        forceNewTab: Boolean
    ) {
        try {
            val terminalView = TerminalToolWindowManager.getInstance(project)
            val contentManager = terminalView.toolWindow?.contentManager

            val terminalWidget = if (forceNewTab) {
                terminalView.createShellWidget(project.basePath, name, true, true)
            } else {
                when (val content = contentManager?.findContent(name)) {
                    null -> terminalView.createShellWidget(project.basePath, name, true, true)
                    else -> TerminalToolWindowManager.getWidgetByContent(content) as TerminalWidget
                }
            }

            terminalWidget.sendCommandToExecute(command)
        } catch (err: IOException) {
            err.printStackTrace()
        }
    }
}