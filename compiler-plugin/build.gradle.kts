import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinJvm
import org.gradle.kotlin.dsl.kotlin

plugins {
	kotlin("jvm")
	alias(libs.plugins.maven.publish)
}

kotlin { jvmToolchain(21) }

kotlin.sourceSets.main { kotlin.srcDir("src/main") }

dependencies {
    compileOnly("org.jetbrains.kotlin:kotlin-compiler-embeddable:${libs.versions.kotlin.get()}")
    testImplementation("org.jetbrains.kotlin:kotlin-compiler-embeddable:${libs.versions.kotlin.get()}")
    testImplementation(libs.kotlin.testJunit)
    testImplementation(projects.formdoc)
}

tasks.test { useJUnit() }

// Generate service registration without another version-sensitive compiler plugin.
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
    dependsOn(tasks.jar)
    systemProperty("formdoc.test.plugin", tasks.jar.get().archiveFile.get().asFile.absolutePath)
    systemProperty("formdoc.test.classpath", sourceSets.test.get().runtimeClasspath.asPath)
}

// Maven Central 公開設定（POM の共通値はルート、モジュール別値は本モジュールの gradle.properties）。
// TestKit フィクスチャの local-repo 経路（docs/test/フィクスチャ構成.md §4）も
// この publication の publishToMavenLocal を使う。javadoc jar は空で Central の要件を満たす
mavenPublishing {
	coordinates("io.github.deference3", "formdoc-compiler","0.0.7")
	publishToMavenCentral()
	signAllPublications()
	configure(KotlinJvm(javadocJar = JavadocJar.Empty()))
	pom {
		name = "Form Doc compiler plugin"
		description = "A Compose Multiplatform form validating library."
		inceptionYear = "2026"
		url = "https://github.com/DeFerence3/form-doctor"
		licenses {
			license {
				name = "The Apache License, Version 2.0"
				url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
				distribution = "https://www.apache.org/licenses/LICENSE-2.0.txt"
			}
		}
		developers {
			developer {
				id = "abhishek-deference"
				name = "Abhishek Krishnan"
				url = "https://github.com/DeFerence3"
			}
		}
		scm {
			url = "https://github.com/DeFerence3/form-doctor"
			connection = "scm:git:git://github.com/DeFerence3/form-doctor.git"
			developerConnection = "scm:git:ssh://git@github.com/DeFerence3/form-doctor.git"
		}
	}
}
