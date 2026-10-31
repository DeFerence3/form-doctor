plugins {
    alias(libs.plugins.kotlinJvm)
    `java-gradle-plugin`
    `maven-publish`
}

kotlin { jvmToolchain(21) }
java { withSourcesJar() }

dependencies {
    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin-api:${libs.versions.kotlin.get()}")
    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin:${libs.versions.kotlin.get()}")
}

gradlePlugin {
    plugins {
        create("formdoc") {
            id = "io.github.deference3.formdoc"
            implementationClass = "me.deference.formdoc.gradle.FormDocGradlePlugin"
            displayName = "FormDoc"
            description = "Adds the FormDoc validation compiler plugin to Kotlin compilations."
        }
    }
}

val generatePluginProperties by tasks.registering {
    val output = layout.buildDirectory.dir("generated/pluginProperties")
    val pluginVersion = project.version.toString()
    inputs.property("pluginVersion", pluginVersion)
    outputs.dir(output)
    doLast {
        output.get().file("formdoc-plugin.properties").asFile.apply {
            parentFile.mkdirs()
            writeText("version=$pluginVersion\n")
        }
    }
}
tasks.processResources { from(generatePluginProperties) }
