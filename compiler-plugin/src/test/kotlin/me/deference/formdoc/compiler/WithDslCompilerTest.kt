package me.deference.formdoc.compiler

import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.net.URLClassLoader
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WithDslCompilerTest {
    @get:Rule val temporaryFolder = TemporaryFolder()

    private fun compile(source: String, expected: ExitCode = ExitCode.OK): Pair<java.io.File, String> {
        val root = temporaryFolder.newFolder()
        val input = root.resolve("Fixture.kt").apply { writeText("import me.deference.formdoc.*\n" + source) }
        val output = root.resolve("classes")
        val messages = ByteArrayOutputStream()
        val exit = K2JVMCompiler().exec(PrintStream(messages),
            "-no-stdlib", "-no-reflect", "-jvm-target", "21",
            "-classpath", System.getProperty("formdoc.test.classpath"),
            "-Xplugin=${System.getProperty("formdoc.test.plugin")}",
            "-d", output.absolutePath, input.absolutePath)
        assertEquals(expected, exit, messages.toString())
        return output to messages.toString()
    }

    private fun run(source: String) {
        val (output, _) = compile(source)
        URLClassLoader(arrayOf(output.toURI().toURL()), javaClass.classLoader).use { loader ->
            loader.loadClass("FixtureKt").getMethod("verify").invoke(null)
        }
    }

    @Test fun `generates companion and callable empty hook`() = run("""
        @Validatable @WithDsl data class Form(val name: String = "")
        fun verify() {
            val builder = FormMetadataBuilder<Form>()
            Form.dsl(builder)
            check(builder.build().fields.isEmpty())
            val base = formConfig<Form> { field(Form::name, label = "Name") { notBlank("Required") } }
            val metadata = formConfig(base) { Form.dsl(this) }
            check(metadata.fields == base.fields)
            check(metadata.fieldMetadata == base.fieldMetadata)
            val state = FormState(Form(), metadata)
            check(!state.validateAll())
            check(state.errors()[Form::name] == "Required")
        }
    """.trimIndent())

    @Test fun `adds hook to existing and named companions without changing members`() = run("""
        @Validatable @WithDsl data class Form(val name: String = "") {
            companion object Rules { val sentinel = "preserved" }
        }
        fun verify() {
            Form.dsl(FormMetadataBuilder<Form>())
            check(Form.sentinel == "preserved")
        }
    """.trimIndent())

    @Test fun `uses existing hook and reads live values independently for shared metadata`() = run("""
        @Validatable @WithDsl data class Form(val password: String = "", val confirmation: String = "") {
            companion object {
                var calls = 0
                fun dsl(builder: FormMetadataBuilder<Form>) {
                    calls++
                    builder.field(Form::confirmation) {
                        validateWithForm { value, state ->
                            if (value != state.getState(Form::password).value) "Mismatch" else null
                        }
                    }
                }
            }
        }
        fun verify() {
            val base = formConfig<Form> {
                field(Form::password)
                field(Form::confirmation, label = "Confirm", required = true) { notBlank("Required") }
            }
            val metadata = formConfig(base) { Form.dsl(this) }
            check(Form.calls == 1)
            check(metadata.fields.size == 2)
            check(metadata.fieldMetadata[Form::confirmation]?.label == "Confirm")
            val first = FormState(Form("one", "one"), metadata)
            val second = FormState(Form("two", "two"), metadata)
            check(first.validateAll())
            check(second.validateAll())
            first.getState(Form::password).value = "changed"
            check(!first.validateAll())
            check(first.errors()[Form::confirmation] == "Mismatch")
            check(second.validateAll())
            first.getState(Form::confirmation).value = ""
            check(!first.validateAll())
            check(first.errors()[Form::confirmation] == "Required")
        }
    """.trimIndent())

    @Test fun `rejects incompatible user hook`() {
        val (_, messages) = compile("""
            @Validatable @WithDsl data class Form(val name: String) {
                companion object { fun dsl() {} }
            }
        """.trimIndent(), ExitCode.COMPILATION_ERROR)
        assertTrue(messages.contains("@WithDsl expects public fun dsl"), messages)
    }

    @Test fun `requires Validatable data class`() {
        val (_, messages) = compile("@WithDsl class Form", ExitCode.COMPILATION_ERROR)
        assertTrue(messages.contains("@WithDsl requires a non-generic @Validatable data class"), messages)
    }

    @Test fun `rejects generic models with a diagnostic`() {
        val (_, messages) = compile("@Validatable @WithDsl data class Form<T>(val value: T)", ExitCode.COMPILATION_ERROR)
        assertTrue(messages.contains("@WithDsl requires a non-generic @Validatable data class"), messages)
    }

    @Test fun `rejects a hook for the wrong model`() {
        val (_, messages) = compile("""
            data class Other(val name: String)
            @Validatable @WithDsl data class Form(val name: String) {
                companion object { fun dsl(builder: FormMetadataBuilder<Other>) {} }
            }
        """.trimIndent(), ExitCode.COMPILATION_ERROR)
        assertTrue(messages.contains("@WithDsl expects public fun dsl"), messages)
    }

    @Test fun `does not add a hook without WithDsl`() {
        val (_, messages) = compile("""
            @Validatable data class Form(val name: String)
            fun verify() { Form.dsl(FormMetadataBuilder<Form>()) }
        """.trimIndent(), ExitCode.COMPILATION_ERROR)
        assertTrue(messages.contains("unresolved reference", ignoreCase = true), messages)
    }
}
