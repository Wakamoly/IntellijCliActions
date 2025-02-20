package io.github.vacxe.cliactions.configurations

import java.io.File

interface ConfigurationProvider {
    fun subscribe(updateSubscription: (List<File>) -> Unit)
    fun unsubscribe()
}