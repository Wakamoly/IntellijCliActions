package io.github.vacxe.cliactions.ui

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlException
import com.intellij.icons.AllIcons
import com.intellij.ui.components.JBTabbedPane
import io.github.vacxe.cliactions.configurations.ConfigurationFileExtension
import io.github.vacxe.cliactions.configurations.ConfigurationProvider
import io.github.vacxe.cliactions.model.Command
import io.github.vacxe.cliactions.model.Config
import io.github.vacxe.cliactions.ui.toolwindow.ContentTabItem
import io.github.vacxe.cliactions.ui.toolwindow.ToolWindowState
import java.awt.BorderLayout
import java.awt.Dimension
import java.io.File
import javax.swing.*

class CliActionsTablePanel(
    private val projectBasePath: String?,
    private val configurationFinder: ConfigurationProvider,
    private val runTerminalCommand: (String, String, Boolean) -> Unit,
) : JPanel() {
    private val yaml = Yaml(configuration = Yaml.default.configuration.copy(strictMode = false))


    private val configsUpdate: (List<File>) -> Unit = { configFiles ->
        if (configFiles.toList().isNotEmpty()) {
            val tabItems = configFiles.map { file ->
                val errorMessages = mutableListOf<String>()
                val prepend = when {
                    projectBasePath == null -> ""
                    file.absolutePath.startsWith(projectBasePath) -> "Local: "
                    else -> "Global: "
                }
                val tabName = prepend + file.name.replace(ConfigurationFileExtension, "")
                val groups =
                    try {
                        yaml.decodeFromString(Config.serializer(), file.readText()).groups
                    } catch (e: YamlException) {
                        errorMessages.add("File `${file.name}` error:" + e.message)
                        emptyList()
                    }
                ContentTabItem(
                    name = tabName,
                    messages = errorMessages.toList(),
                    groups = groups,
                )
            }.toList()
            updateState(ToolWindowState.Content(tabItems))
        } else {
            updateState(
                ToolWindowState.Error(
                    "Can't find any config files. Please define `<name>.cliactions.yaml` in the project root (local) " +
                            "or user home directory (global)."
                )
            )
        }
    }

    private fun updateState(viewState: ToolWindowState) {
        removeAll()
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        when (viewState) {
            is ToolWindowState.Content -> {
                val parentTabbedPane = JBTabbedPane()
                viewState.contentTabItems.forEach { tabItem ->
                    val tabItemPane = JBTabbedPane()
                    if (tabItem.messages.isNotEmpty()) {
                        tabItemPane.addTab(
                            "Errors",
                            InformationView(tabItem.messages.map { InformationLabel(it) })
                        )
                    }

                    tabItem.groups.forEach { group ->
                        val commandsLayout = JPanel()
                        commandsLayout.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
                        commandsLayout.layout = BoxLayout(commandsLayout, BoxLayout.Y_AXIS)
                        group.commands.forEach { command ->
                            commandsLayout.add(CmdShortcutItem(command))
                            commandsLayout.add(JSeparator().apply {
                                maximumSize = Dimension(this.maximumSize.width, 5)
                            })
                        }
                        tabItemPane.add(group.name, JScrollPane(commandsLayout))
                    }
                    parentTabbedPane.addTab(tabItem.name, tabItemPane)
                }
                add(parentTabbedPane)
            }

            is ToolWindowState.Error -> {
                val informationLabel = InformationLabel(viewState.message)
                add(InformationView(listOf(informationLabel)))
            }

            is ToolWindowState.Loading -> {
                val informationLabel = InformationLabel(viewState.message, AllIcons.Actions.Refresh)
                add(InformationView(listOf(informationLabel)))
            }
        }
    }

    private fun CmdShortcutItem(iCmdCommand: Command): JComponent {
        val panel = JPanel()
        panel.alignmentX = LEFT_ALIGNMENT
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

    private fun InformationView(messageLabels: List<InformationLabel>): JComponent {
        val panel = JPanel().apply {
            alignmentX = CENTER_ALIGNMENT
            alignmentY = CENTER_ALIGNMENT
            messageLabels.forEach { jLabel -> add(jLabel.toLabel()) }
        }
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

data class InformationLabel(
    val message: String,
    val icon: Icon? = AllIcons.Ide.FatalErrorRead,
    val iconHorizontalAlignment: Int = SwingConstants.LEADING,
) {
    fun toLabel(): JLabel {
        val jLabel = JLabel(message, icon, iconHorizontalAlignment)
        jLabel.maximumSize = jLabel.preferredSize
        return jLabel
    }
}
