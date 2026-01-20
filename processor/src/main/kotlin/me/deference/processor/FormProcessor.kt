package me.deference.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.google.devtools.ksp.validate
import java.io.OutputStream

class FormProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {

    private var validatableClasses = mutableListOf<KSClassDeclaration>()
    private var isRegistryGenerated = false

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver.getSymbolsWithAnnotation("me.deference.formdoc.Validatable")
        logger.info("Symbols Found: ${symbols.count()}")
        symbols
            .filterIsInstance<KSClassDeclaration>()
            .filter { it.validate() }
            .forEach {
                validatableClasses += it
                it.accept(FormVisitor(), Unit)
            }

        if (validatableClasses.isNotEmpty() && !isRegistryGenerated) {
            generateMetadataRegistry(validatableClasses)
            isRegistryGenerated = true
        }

        return symbols.filter { !it.validate() }.toList()
    }

    inner class FormVisitor : KSVisitorVoid() {
        override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
            val packageName = classDeclaration.packageName.asString()
            val className = classDeclaration.simpleName.asString()
            val generatedClassName = "${className}Metadata"
            val file = codeGenerator.createNewFile(
                Dependencies(true, classDeclaration.containingFile!!),
                packageName,
                generatedClassName
            )

            file.use { output ->
                output.appendLine("package $packageName")
                output.appendLine()
                output.appendLine("import me.deference.formdoc.*")
                output.appendLine("import me.deference.formdoc.Validators")
                output.appendLine()
                output.appendLine("class $generatedClassName : FormMetadata<$className> {")
                output.appendLine("    override val validators: Map<kotlin.reflect.KProperty1<$className, *>, List<FieldValidator<*>>> = mapOf(")

                val properties = classDeclaration.getAllProperties()
                properties.forEach { prop ->
                    val propName = prop.simpleName.asString()
                    val propValidators = mutableListOf<String>()

                    prop.annotations.forEach { annot ->
                        val annotationName = annot.shortName.asString()
                        when (annotationName) {
                            "NotBlank" -> {
                                val msg = annot.arguments.find { it.name?.asString() == "message" }?.value as? String ?: "This field is required"
                                propValidators.add("Validators.notBlank(\"$msg\")")
                            }
                            "Email" -> {
                                val msg = annot.arguments.find { it.name?.asString() == "message" }?.value as? String ?: "Invalid email address"
                                propValidators.add("Validators.email(\"$msg\")")
                            }
                            "Pattern" -> {
                                val regex = annot.arguments.find { it.name?.asString() == "regex" }?.value as? String ?: ""
                                val msg = annot.arguments.find { it.name?.asString() == "message" }?.value as? String ?: "Invalid format"
                                propValidators.add("Validators.pattern(\"$regex\", \"$msg\")")
                            }
                            "Min" -> {
                                val value = annot.arguments.find { it.name?.asString() == "value" }?.value as? Int ?: 0
                                val msg = annot.arguments.find { it.name?.asString() == "message" }?.value as? String ?: "Value too small"
                                propValidators.add("Validators.min($value, \"$msg\")")
                            }
                            "Max" -> {
                                val value = annot.arguments.find { it.name?.asString() == "value" }?.value as? Int ?: 0
                                val msg = annot.arguments.find { it.name?.asString() == "message" }?.value as? String ?: "Value too large"
                                propValidators.add("Validators.max($value, \"$msg\")")
                            }
                            "ValidatedBy" -> {
                                val kClass = annot.arguments.first().value.toString()
                                propValidators.add("$kClass()")
                            }
                        }
                    }

                    if (propValidators.isNotEmpty()) {
                        output.appendLine("        $className::$propName to listOf(")
                        propValidators.forEachIndexed { index, v ->
                            val comma = if (index < propValidators.size - 1) "," else ""
                            output.appendLine("            $v$comma")
                        }
                        output.appendLine("        ),")
                    }
                }
                output.appendLine("    )")
                output.appendLine("}")
            }
        }
    }

    private fun generateMetadataRegistry(
        classes: List<KSClassDeclaration>
    ) {
        logger.info("Generating metadata registry")
        val pkg = "me.deference.formdoc.registry"
        val file = codeGenerator.createNewFile(
            Dependencies(false),
            pkg,
            "FormMetadataRegistry"
        )

        file.use { output ->
            output.appendLine("package $pkg")
            output.appendLine()
            output.appendLine("import me.deference.formdoc.*")
            output.appendLine()

            classes.forEach {
                val qName = it.qualifiedName!!.asString()
                output.appendLine("import $qName")
                output.appendLine("import ${qName}Metadata")
            }

            output.appendLine()
            output.appendLine("actual object FormMetadataRegistry {")
            output.appendLine("    @Suppress(\"UNCHECKED_CAST\")")
            output.appendLine("    actual inline fun <reified T : Any> get(): FormMetadata<T>? =")
            output.appendLine("        when (T::class) {")

            classes.forEach {
                val className = it.simpleName.asString()
                output.appendLine(
                    "            $className::class -> ${className}Metadata() as FormMetadata<T>"
                )
            }

            output.appendLine("            else -> null")
            output.appendLine("        }")
            output.appendLine("}")
        }
    }

    private fun OutputStream.appendLine(str: String = "") {
        this.write((str + "\n").toByteArray())
    }
}