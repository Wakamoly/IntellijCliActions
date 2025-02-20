package io.github.vacxe.cliactions.ui

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlException
import com.intellij.icons.AllIcons
import com.intellij.ide.plugins.PluginManagerCore.logger
import com.intellij.ui.components.JBTabbedPane
import io.github.vacxe.cliactions.configurations.ConfigurationProvider
import io.github.vacxe.cliactions.model.Command
import io.github.vacxe.cliactions.model.Config
import io.github.vacxe.cliactions.ui.toolwindow.ToolWindowState
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.io.File
import javax.swing.*

class CliActionsTablePanel(
    private val configurationFinder: ConfigurationProvider, private val runTerminalCommand: (String, String, Boolean) -> Unit
) : JPanel() {

    private val configsUpdate: (Sequence<File>) -> Unit = { configFiles ->
        if (configFiles.toList().isNotEmpty()) {
            val errorMessages = mutableListOf<String>()
            val groups = configFiles.map { file ->
                try {
                    Yaml.default.decodeFromString(
                        Config.serializer(), file.readText()
                    ).groups
                } catch (e: YamlException) {
                    errorMessages.add(e.message)
                    logger.error("Error parsing config file: ${file.absolutePath}", e)
                    emptyList()
                }
            }.flatten().toList()
            updateState(ToolWindowState.Content(errorMessages.toList(), groups))
        } else {
            updateState(
                ToolWindowState.Error(
                    "Can't find any config files. Please define `<name>.cliactions.yaml` in the project root " +
                            "or user home directory."
                )
            )
        }
    }

    private fun updateState(viewState: ToolWindowState) {
        removeAll()
        when (viewState) {
            is ToolWindowState.Content -> {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)

                val jbTabbedPane = JBTabbedPane()

                viewState.messages.forEach { message ->
                    add(InformationView(message))
                }

                viewState.groups.forEach { group ->
                    val commandsLayout = JPanel()
                    commandsLayout.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
                    commandsLayout.layout = BoxLayout(commandsLayout, BoxLayout.Y_AXIS)
                    group.commands.forEach { command ->
                        commandsLayout.add(CmdShortcutItem(command))
                        commandsLayout.add(JSeparator().apply {
                            maximumSize = Dimension(this.maximumSize.width, 5)
                        })
                    }
                    jbTabbedPane.add(group.name, JScrollPane(commandsLayout))
                }
                add(jbTabbedPane)
            }

            is ToolWindowState.Error -> {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
                add(InformationView(viewState.message))
            }

            is ToolWindowState.Loading -> {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
                add(InformationView(viewState.message))
            }
        }
    }

    private fun CmdShortcutItem(iCmdCommand: Command): JComponent {
        val panel = JPanel()
        panel.alignmentX = Component.LEFT_ALIGNMENT
        val button = JButton("Run", AllIcons.Actions.Execute)
        button.addActionListener {
            if (iCmdCommand.prompt) {
                if (JOptionPane.showConfirmDialog(
                        null, "Run: ${iCmdCommand.name} ?", "Confirm Action", JOptionPane.YES_NO_OPTION
                    ) == 0
                ) {
                    runTerminalCommand.invoke(
                        iCmdCommand.name,
                        iCmdCommand.command,
                        iCmdCommand.forceNewTab
                    )
                }
            } else {
                runTerminalCommand.invoke(
                    iCmdCommand.name,
                    iCmdCommand.command,
                    iCmdCommand.forceNewTab
                )
            }
        }
        panel.add(button)
        val stripLabel = JLabel(iCmdCommand.name)
        panel.add(stripLabel, BorderLayout.WEST)
        panel.maximumSize = panel.preferredSize
        return panel
    }

    private fun InformationView(message: String): JComponent {
        val panel = JPanel()
        panel.alignmentX = Component.CENTER_ALIGNMENT
        panel.alignmentY = Component.CENTER_ALIGNMENT
        val stripLabel = JLabel(message)
        panel.add(stripLabel)
        return panel
    }

    fun dispose() {
        configurationFinder.unsubscribe()
    }

    fun initialise() {
        updateState(ToolWindowState.Loading("Searching for configuration files (*.cliactions.yaml)..."))
        configurationFinder.subscribe(configsUpdate)
    }
}
