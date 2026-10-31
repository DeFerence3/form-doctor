# FormDoc setup

This guide covers installing FormDoc in a Kotlin Multiplatform module, configuring
generation, and testing a local release. For models, validation rules, and UI
integration, see [USAGE.MD](USAGE.MD).

## Contents

- [Components and versions](#components-and-versions)
- [Toolchain and platforms](#toolchain-and-platforms)
- [Configure repositories](#configure-repositories)
- [Configure the consuming module](#configure-the-consuming-module)
- [Add the metadata registry](#add-the-metadata-registry)
- [Verify your installation](#verify-your-installation)
- [Additional targets](#additional-targets)
- [Use only the standalone DSL](#use-only-the-standalone-dsl)
- [Test a local release](#test-a-local-release)
- [Develop in this repository](#develop-in-this-repository)
- [Preparing a public release](#preparing-a-public-release)
- [Troubleshooting](#troubleshooting)

## Components and versions

Examples use **0.0.7**, the version configured in this checkout. This does not
assert that all the new artifacts are already on Maven Central or the Gradle
Plugin Portal. Until they are released together, use the
[local publication workflow](#test-a-local-release).

| Component | Identifier | Purpose |
| --- | --- | --- |
| Runtime | `io.github.deference3:formdoc:0.0.7` | Annotations, validators, form state, and Compose helpers. |
| KSP processor | `io.github.deference3:formdoc-processor:0.0.7` | Generates field metadata and platform registry implementations. |
| Gradle plugin | `id("io.github.deference3.formdoc") version "0.0.7"` | Configures the Kotlin compiler plugin for the module's compilations. |
| Compiler plugin for Kotlin 2.3.0 | `io.github.deference3:formdoc-compiler:0.0.7` | Generates companion `dsl` hooks for `@WithDsl`. Selected automatically. |
| Compiler plugin for Kotlin 2.4.0 | `io.github.deference3:formdoc-compiler-kotlin24:0.0.7` | The same implementation compiled against Kotlin 2.4.0 APIs. Selected automatically. |

The Gradle plugin's implementation artifact is
`io.github.deference3:formdoc-gradle-plugin:0.0.7`. Consumers normally apply its
plugin ID instead of depending on that JAR directly.

| Approach | Runtime | KSP processor | FormDoc Gradle plugin |
| --- | --- | --- | --- |
| `@Validatable` field annotations | Required | Required | Optional |
| `@Validatable` + `@WithDsl` | Required | Required | Required |
| Standalone `formConfig` | Required | Not needed | Not needed |

The Gradle plugin configures compiler loading. It does **not** apply KSP, add the
runtime dependency, create your registry source file, or replace field metadata
generation. Do not put the compiler plugin on `implementation` or `ksp`.

## Toolchain and platforms

Use this baseline for the code in the repository:

| Tool or platform | Baseline |
| --- | --- |
| Kotlin and Kotlin Compose compiler plugin | **2.3.0** |
| Gradle JVM | **JDK 21**; plugin artifacts require Java 21 or newer |
| Gradle wrapper | **8.14.3** |
| KSP | **2.3.4** |
| Compose Multiplatform | **1.10.0** in the sample and runtime build |
| Android Gradle Plugin | **8.11.2** in the sample |
| Android | Minimum SDK **24**, compile SDK **36** in the runtime build |

The default examples use Kotlin **2.3.0**. Kotlin **2.4.0** is also supported by
a separately compiled artifact; use a matching 2.4.0 Compose compiler plugin in
that project. The FormDoc wrapper selects the artifact from the project's Kotlin
Gradle plugin version. Keep that version aligned with the actual compiler.

Compiler APIs differ between these releases, so the Kotlin 2.3.0 compiler JAR
must not be loaded into Kotlin 2.4.0. Both variants share source and run the same
compiler tests. Other Kotlin versions produce a configuration error rather than
loading an unverified binary. Supporting another version requires rebuilding and
testing a corresponding compiler variant. The Gradle plugin version follows
FormDoc's version, not Kotlin's.

The runtime declares Android, JVM, JavaScript, Wasm/JS, and iOS targets
(`iosX64`, `iosArm64`, `iosSimulatorArm64`). The companion feature has been checked
with JVM tests and JavaScript/Wasm compilation. Android and iOS need their own
release verification; a declared target does not mean every target has been
tested. Apple builds and tests require a suitable macOS/Xcode environment.

Configure Android Studio's **Gradle JDK** or command-line `JAVA_HOME` to use
JDK 21. Setting only the application's Kotlin `jvmTarget` does not change the
JVM that loads Gradle plugins.

## Configure repositories

For artifacts published to Maven Central, use this in the root
`settings.gradle.kts`. Merge repositories into existing blocks if necessary:

```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "FormDocExample"
include(":shared")
```

For local testing, add `mavenLocal()` **first in both repository blocks**.
Plugin repositories resolve the Gradle plugin and its marker; dependency
repositories resolve the runtime, processor, and compiler artifact. Adding
Maven Local to only one block is insufficient.

`pluginManagement` belongs in settings, not the module build file. Remove
`mavenLocal()` when you want a consumer to resolve only published artifacts and
avoid a local build shadowing the same version.

## Configure the consuming module

Apply the plugins to the **Kotlin module containing the form models**. Applying
FormDoc only to a root project with `apply false` does not enable generation in
its subprojects.

This minimal `shared/build.gradle.kts` uses a JVM target through Kotlin
Multiplatform. It also provides the Compose dependencies used by the
[UI example](USAGE.MD#compose-integration):

```kotlin
plugins {
    kotlin("multiplatform") version "2.3.0"
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.0"
    id("org.jetbrains.compose") version "1.10.0"
    id("com.google.devtools.ksp") version "2.3.4"
    id("io.github.deference3.formdoc") version "0.0.7"
}

kotlin {
    jvmToolchain(21)
    jvm()

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    sourceSets {
        commonMain.dependencies {
            implementation("io.github.deference3:formdoc:0.0.7")
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

dependencies {
    add("kspJvm", "io.github.deference3:formdoc-processor:0.0.7")
}
```

This creates a shared library module. Add your application's platform entry point
to display the UI. In an existing Compose project, retain its platform settings
and add the FormDoc runtime, processor, and plugin. If versions are declared at
the root or in a version catalog, reuse those declarations without introducing
conflicting versions.

Normal consumers do not need a custom `formdocCompilerPlugin` configuration,
compiler-task classpath changes, or `-Xplugin` arguments.

### Target-specific KSP dependencies

Add the processor for every target that compiles annotated models, following the
[KSP multiplatform configuration model](https://kotlinlang.org/docs/ksp-multiplatform.html).
Use configurations for targets actually declared in your module:

| Target declaration | Processor configuration |
| --- | --- |
| `jvm()` | `kspJvm` |
| `js { browser() }` | `kspJs` |
| `wasmJs { browser() }` | `kspWasmJs` |
| `androidTarget()` in the sample's Android application setup | `kspAndroid` |
| `iosArm64()` | `kspIosArm64` |
| `iosSimulatorArm64()` | `kspIosSimulatorArm64` |
| `iosX64()` | `kspIosX64` |

Names depend on the target and Android plugin configuration. For renamed targets
or another Android KMP plugin, inspect the module's Gradle configurations rather
than assuming the sample names apply unchanged.

The sample currently uses the older catch-all `ksp(projects.processor)` setup.
New consumers should use target-specific dependencies. Do not add both styles
for the same targets.

The processor writes platform `actual` registries. Do **not** replace target
processing with only `kspCommonMainMetadata`, or add platform-generated source
directories to `commonMain`: this implementation uses the registry bridge below.

## Add the metadata registry

Create this file in the module containing your models:

`shared/src/commonMain/kotlin/formdoc/registry/FormMetadataRegistry.kt`

```kotlin
package formdoc.registry

import me.deference.formdoc.FormMetadata

expect object FormMetadataRegistry {
    inline fun <reified T : Any> get(): FormMetadata<T>?
}
```

Use this package, object name, and signature exactly. KSP generates matching
`actual` implementations for configured targets. Do not handwrite another
`actual` registry or edit generated files.

Keep the models and bridge in the same KMP module. The current registry does not
automatically merge models from several library modules or discover annotated
classes in binary dependencies. Keep model class names unique within a module,
even across packages: the registry imports models by simple name. Use a named
Kotlin package such as `example`; the generator does not support the default
(empty) package. Prefer public, top-level, non-generic data classes with publicly
readable properties.

The generated registry is designed for Kotlin Multiplatform, even when you only
need JVM. A plain `kotlin("jvm")` or Android-only module lacks this `expect`/`actual`
arrangement by default. Use a KMP module for the annotation workflow, or use the
standalone DSL without KSP.

## Verify your installation

Create `shared/src/commonMain/kotlin/example/Registration.kt`:

```kotlin
package example

import me.deference.formdoc.NotBlank
import me.deference.formdoc.Validatable
import me.deference.formdoc.WithDsl

@Validatable
@WithDsl
data class Registration(
    @NotBlank(message = "Name is required")
    val name: String = ""
)
```

This model intentionally has no companion: the compiler should create its empty
`dsl` hook. Add
`shared/src/commonTest/kotlin/example/RegistrationTest.kt`:

```kotlin
package example

import formdoc.registry.FormMetadataRegistry
import me.deference.formdoc.FormState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RegistrationTest {
    @Test
    fun validatesGeneratedMetadata() {
        val metadata = assertNotNull(FormMetadataRegistry.get<Registration>())
        val form = FormState(Registration(), metadata)

        assertFalse(form.validateAll())
        assertEquals("Name is required", form.errors()[Registration::name])

        form.getState(Registration::name).value = "Sam"
        assertTrue(form.validateAll())
    }
}
```

Run:

```shell
./gradlew :shared:jvmTest
```

On Windows PowerShell, use `.\gradlew.bat :shared:jvmTest`. This checks the
runtime, processor, registry, and compiler plugin together. Resolving only the
Gradle plugin does not verify KSP configuration.

Generated Kotlin files appear under the module's `build/generated/ksp/` directory.
`RegistrationMetadata` should exist in the model's package. The generated companion
hook is a compiled declaration, not an editable generated `.kt` file.

## Additional targets

For browser builds, extend the module's existing `kotlin` block:

```kotlin
// Add inside kotlin { ... }.
js { browser() }
@OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
wasmJs { browser() }
```

Add the corresponding processors:

```kotlin
dependencies {
    add("kspJs", "io.github.deference3:formdoc-processor:0.0.7")
    add("kspWasmJs", "io.github.deference3:formdoc-processor:0.0.7")
}
```

For iOS, add the device/simulator targets your app uses and their matching
processor configurations. Run Apple compilation and tests on macOS.

For Android, retain your Android plugin, SDK configuration, and target declaration.
The sample uses AGP 8.11.2 with `androidTarget()` and `kspAndroid`; the runtime
uses the Android KMP library plugin. These two Android setups are not interchangeable
verbatim.

FormDoc delegates compiler attachment to Kotlin's Gradle integration for each
target, including Native. The runtime must also have a published variant for
every consumer target. A JVM JAR alone does not supply Android, JS, Wasm, or iOS
artifacts.

## Use only the standalone DSL

If every rule uses `formConfig`, add only the runtime to the relevant source set.
You do not need the FormDoc Gradle plugin, KSP processor, or registry. Retain
Compose compiler configuration for modules defining composable functions.

See [standalone DSL usage](USAGE.MD#standalone-dsl-and-migration). Annotations are
not processed in a runtime-only setup.

## Test a local release

Run this from the **FormDoc repository**, not the consuming app:

```shell
./gradlew :formdoc-gradle-plugin:publishToMavenLocal :compiler-plugin:publishToMavenLocal :compiler-plugin-kotlin24:publishToMavenLocal :processor:publishToMavenLocal :formdoc:publishToMavenLocal
```

Windows PowerShell:

```powershell
.\gradlew.bat :formdoc-gradle-plugin:publishToMavenLocal :compiler-plugin:publishToMavenLocal :compiler-plugin-kotlin24:publishToMavenLocal :processor:publishToMavenLocal :formdoc:publishToMavenLocal
```

Publish the updated runtime and processor as well as the wrapper and compiler
variants needed by consumers. An older
runtime at the same version can lack `@WithDsl`, and an older processor will not
invoke the hook. Maven Local is normally under `~/.m2/repository`, unless your
Maven configuration overrides it.

Use a host capable of building the runtime targets you need. Full Apple artifact
publication needs macOS/Xcode. If aggregate runtime publication cannot build a
target on your host, inspect `./gradlew :formdoc:tasks --all` and publish the root
KMP metadata plus the supported variants needed by your consumer. A partial local
publication is not a complete multiplatform release.

The Gradle plugin publishes both an implementation and a marker POM:

| Publication | Maven coordinates |
| --- | --- |
| Implementation | `io.github.deference3:formdoc-gradle-plugin:0.0.7` |
| Plugin marker | `io.github.deference3.formdoc:io.github.deference3.formdoc.gradle.plugin:0.0.7` |

The marker lets Gradle's `plugins` block find the implementation. Publishing only
the compiler JAR does not create this marker.

In the consumer, add `mavenLocal()` to both repository blocks and use matching
plugin, runtime, and processor versions. Republish after implementation changes.
Use `--refresh-dependencies` when diagnosing a consumer resolving an older
artifact at the same version; prefer a new version for public releases.

## Develop in this repository

The repository already provides:

- A `gradle-plugin` included build registered in `pluginManagement`.
- `id("io.github.deference3.formdoc")` applied to `composeApp`.
- Dependency substitution from the compiler's Maven coordinates to the local
  `:compiler-plugin` project.
- Runtime and processor project dependencies and the registry bridge.

The sample therefore uses checkout sources without publishing first. Consumers
of released artifacts should not copy included-build or substitution settings.

Useful verification commands:

```shell
./gradlew :compiler-plugin:test :compiler-plugin-kotlin24:test :formdoc:jvmTest :composeApp:jvmTest
./gradlew :composeApp:compileKotlinJs :composeApp:compileKotlinWasmJs
./gradlew :formdoc-gradle-plugin:validatePlugins
```

## Preparing a public release

Local publication does not upload to Maven Central or the Gradle Plugin Portal.
Before distributing a release:

1. Align versions in `formdoc/build.gradle.kts`, `processor/build.gradle.kts`,
   `compiler-plugin/build.gradle.kts`, `compiler-plugin-kotlin24/build.gradle.kts`,
   and `gradle-plugin/gradle.properties`.
   The wrapper uses its version to select the matching compiler artifact.
2. Update these guides and verify the plugin/Kotlin compatibility combination.
3. Complete publishing metadata, signing, and repository credentials for all
   artifacts. Keep the POM license consistent with [LICENSE.md](LICENSE.md).
4. Configure remote publication for the Gradle wrapper **and its marker**.
   It currently applies Gradle's `maven-publish` plugin but has no remote
   repository or Plugin Portal publishing setup.
5. Publish and test runtime variants for the advertised platforms. Verify the
   intended remote repository from a separate consuming project.

The runtime, processor, and compiler modules have Maven Central configuration.
Review publishing metadata for every compiler variant before public release.
A successful local publish does not validate Central's publication requirements.
See the official [Kotlin Multiplatform publishing guide](https://kotlinlang.org/docs/multiplatform/multiplatform-publish-libraries-to-maven.html)
for the remote publication process.

## Troubleshooting

| Symptom | Check or action |
| --- | --- |
| Plugin ID cannot be found | Add the repository under `pluginManagement`; for local builds publish the wrapper and marker, not only the compiler JAR. |
| Compiler artifact cannot be resolved | Add its repository to dependency repositories and publish the matching compiler version. |
| `WithDsl` cannot be imported | Use the runtime built from the same checkout/version as the plugins. |
| `dsl` is unresolved | Apply FormDoc in the module compiling the annotated class; use supported Kotlin 2.3.0 or 2.4.0 and publish the matching compiler artifact. |
| Registry has no `actual` implementation | Configure KSP for that target, keep the bridge in `commonMain`, and ensure a source model has `@Validatable`. |
| Duplicate registry declarations | Remove handwritten/generated-source copies or duplicate processing setup; keep one bridge and target-generated implementations. |
| Registry lookup returns `null` | Use `get<YourModel>()`, not `get<YourModelMetadata>()`; process the model in the same module. |
| Form accepts values without checking annotations | Require non-null generated metadata. Passing `null` does not discover validators automatically. |
| Unsupported class-file version | Run Gradle with JDK 21 or newer. |
| Compiler linkage errors after a Kotlin upgrade | Use the supported Kotlin version, or rebuild and verify the compiler plugin for the new version. |
| `FirExtensionRegistrarAdapter` / `ProjectExtensionDescriptor` class-cast failure | A Kotlin 2.3.0 compiler binary was loaded by Kotlin 2.4.0. Republish the updated wrapper and `:compiler-plugin-kotlin24`; remove manually configured compiler JARs. |
| `@WithDsl expects public fun dsl(...)` | Check the exact [companion signature](USAGE.MD#companion-dsl-with-withdsl). |
| Generated source fails for a generic property or escaped annotation value | See [current constraints](USAGE.MD#current-constraints); use the typed DSL or a custom validator for that rule. |
| Native artifact cannot be built locally | Build and publish it on a supported host with the platform toolchain. |

For failures, inspect the first Kotlin/KSP diagnostic and generated sources under
`build/generated/ksp/`. Report the Kotlin, Gradle, KSP, FormDoc, and target versions
with a minimal model that reproduces the problem.
