plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.1.0"
    id("org.jetbrains.intellij") version "1.17.4"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.1.0"
}

group = "io.github.vacxe"
version = "1.1.0"

repositories {
    mavenCentral()
}

intellij {
    version.set("2024.3.3")
    pluginName.set("CLI Actions")
    plugins.set(listOf("org.jetbrains.plugins.terminal"))
}

kotlin {
    jvmToolchain(17)
}

tasks {
    patchPluginXml {
        sinceBuild.set("232")
        untilBuild.set("")
        changeNotes.set("""
            - Update Kotlin
        """)
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }
}

dependencies {
    implementation("com.charleskorn.kaml:kaml:0.72.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
}
