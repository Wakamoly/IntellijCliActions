package io.github.vacxe.cliactions.configurations

import com.intellij.openapi.project.Project
import kotlinx.coroutines.*
import org.apache.commons.io.FileUtils
import java.io.File
import kotlin.time.Duration.Companion.seconds

class CliActionsConfigurationProvider(private val project: Project) : ConfigurationProvider {
    private var subscription: ((List<File>) -> Unit)? = null
    private var currentConfigurations = listOf<File>()

    /**
     * Holds a string representation of the current configuration names and their sizes.
     * Used internally to track and compare configuration changes.
     * Useful because reloading of files can occur with updates when comparing the previous sizes, but referencing the
     * files themselves for the `.length()` will always match.
     */
    private var currentConfigurationNamesAndSizes: List<Pair<String, Long>>? = null

    private var findConfigsJob: Job? = null

    private fun findConfigs() = CoroutineScope(Dispatchers.IO).launch {
        // Invoke this when appropriate to update the UI
        val invokeUpdate: (newConfigs: List<File>) -> Unit = { newConfigs ->
            val newConfigsList = newConfigs.toList()
            currentConfigurationNamesAndSizes = newConfigsList.mapToAbsolutePathFileNamesAndSizes()
            currentConfigurations = newConfigsList
            subscription?.invoke(newConfigs)
        }

        while (isActive) {
            val projectConfigs = File(project.basePath ?: throw Exception("Project basePath cannot be found"))
                .walk()
                .maxDepth(2)
                .filter { it.name.endsWith(ConfigurationFileExtension) }

            val userConfigs = FileUtils.getUserDirectory()
                .walk()
                .maxDepth(1)
                .filter { it.name.endsWith(ConfigurationFileExtension) }

            val newConfigs = (projectConfigs + userConfigs).toList()
            val configFileNamesAndSizesMatching = currentConfigurationNamesAndSizes?.let { currentNamesAndSizes ->
                val newNamesAndSizes = newConfigs.mapToAbsolutePathFileNamesAndSizes()
                currentNamesAndSizes.containsAll(newNamesAndSizes)
            } ?: false

            if (!configFileNamesAndSizesMatching) {
                invokeUpdate(newConfigs)
            }
            delay(3.seconds)
        }
    }

    override fun subscribe(updateSubscription: (List<File>) -> Unit) {
        subscription = updateSubscription
        findConfigsJob = findConfigs()
    }

    // TODO: Find a way to utilize this when the tool window is hidden, resubscribing when necessary
    override fun unsubscribe() {
        subscription = null
        findConfigsJob?.cancel()
    }

    private fun List<File>.mapToAbsolutePathFileNamesAndSizes(): List<Pair<String, Long>> =
        map { file -> file.absolutePath to file.length() }
}
