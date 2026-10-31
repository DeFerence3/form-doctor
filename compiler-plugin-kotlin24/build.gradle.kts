import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinJvm

plugins {
    kotlin("jvm")
    alias(libs.plugins.maven.publish)
}

kotlin {
    jvmToolchain(21)
    sourceSets.main { kotlin.srcDir("../compiler-plugin/src/main") }
    sourceSets.test { kotlin.srcDir("../compiler-plugin/src/test/kotlin") }
}

// Compiler APIs are not binary compatible across Kotlin releases. Compile the
// same implementation against 2.4.0 and publish it separately from the 2.3 build.
dependencies {
    compileOnly("org.jetbrains.kotlin:kotlin-compiler-embeddable:2.4.0")
    testImplementation("org.jetbrains.kotlin:kotlin-compiler-embeddable:2.4.0")
    testImplementation(libs.kotlin.testJunit)
    testImplementation(projects.formdoc)
}

val generateRegistrarService by tasks.registering {
    val output = layout.buildDirectory.dir("generated/registrarService")
    outputs.dir(output)
    doLast {
        output.get().file("META-INF/services/org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar").asFile.apply {
            parentFile.mkdirs()
            writeText("me.deference.formdoc.compiler.FormDocCompilerPluginRegistrar\n")
        }
    }
}
tasks.processResources { from(generateRegistrarService) }
tasks.test {
    useJUnit()
    dependsOn(tasks.jar)
    systemProperty("formdoc.test.plugin", tasks.jar.get().archiveFile.get().asFile.absolutePath)
    systemProperty("formdoc.test.classpath", sourceSets.test.get().runtimeClasspath.asPath)
}

mavenPublishing {
    coordinates("io.github.deference3", "formdoc-compiler-kotlin24", "0.0.7")
    publishToMavenCentral()
    signAllPublications()
    configure(KotlinJvm(javadocJar = JavadocJar.Empty()))
    pom {
        name = "FormDoc compiler plugin for Kotlin 2.4.0"
        description = "FormDoc companion DSL generation for Kotlin 2.4.0."
        url = "https://github.com/DeFerence3/form-doctor"
        licenses {
            license {
                name = "The Apache License, Version 2.0"
                url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
            }
        }
    }
}
