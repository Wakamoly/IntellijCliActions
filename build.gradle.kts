plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.1.0"
    id("org.jetbrains.intellij.platform") version "2.2.1"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.1.0"
}

group = "io.github.vacxe"
version = "1.1.0"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
        localPlatformArtifacts()
    }
}

intellijPlatform {
    pluginConfiguration {
        name.set("CLI Actions")
        version.set("1.1.0")
        changeNotes.set(
            """
            - Update Kotlin
        """
        )
    }

    signing {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishing {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    intellijPlatform {
        bundledPlugin("org.jetbrains.plugins.terminal")
        jetbrainsRuntime()
        intellijIdeaCommunity("2024.3.3", false)
        pluginVerifier()
        zipSigner()
    }

    implementation("com.charleskorn.kaml:kaml:0.72.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
}
