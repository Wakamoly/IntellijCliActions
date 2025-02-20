package io.github.vacxe.cliactions.terminal

import com.intellij.openapi.project.Project
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

            val terminalWidget = if (forceNewTab) {
                terminalView.createShellWidget(project.basePath, name, true, true)
            } else {
                terminalView.terminalWidgets.find { it.terminalTitle.defaultTitle == name }
                    ?: terminalView.createShellWidget(project.basePath, name, false, false)
            }

            terminalWidget.sendCommandToExecute(command)
        } catch (err: IOException) {
            err.printStackTrace()
        }
    }
}