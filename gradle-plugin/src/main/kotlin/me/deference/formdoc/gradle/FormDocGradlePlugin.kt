package me.deference.formdoc.gradle

import java.util.Properties
import org.gradle.api.Project
import org.gradle.api.GradleException
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion

/** Lets Kotlin's Gradle plugin attach FormDoc to each target's compiler. */
class FormDocGradlePlugin : KotlinCompilerPluginSupportPlugin {
    private lateinit var project: Project

    override fun apply(target: Project) {
        project = target
    }

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean = true

    override fun getCompilerPluginId(): String = "me.deference.formdoc"

    override fun getPluginArtifact(): SubpluginArtifact = SubpluginArtifact(
        groupId = "io.github.deference3",
        artifactId = when (val kotlinVersion = project.getKotlinPluginVersion()) {
            "2.3.0" -> "formdoc-compiler"
            "2.4.0" -> "formdoc-compiler-kotlin24"
            else -> throw GradleException(
                "FormDoc supports Kotlin 2.3.0 and 2.4.0; this project uses $kotlinVersion. " +
                    "Use a supported Kotlin version or a FormDoc build targeting your compiler."
            )
        },
        version = Properties().apply {
            checkNotNull(FormDocGradlePlugin::class.java.getResourceAsStream("/formdoc-plugin.properties")) {
                "Missing FormDoc plugin version resource"
            }.use { load(it) }
        }.getProperty("version")
    )

    @Suppress("OVERRIDE_DEPRECATION")
    override fun getPluginArtifactForNative(): SubpluginArtifact = getPluginArtifact()

    override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> =
        kotlinCompilation.target.project.provider { emptyList() }
}
