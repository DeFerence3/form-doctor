package me.deference.formdoc.compiler

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.isNullable as isNullableType
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.name.FqName

@OptIn(UnsafeDuringIrConstructionAPI::class)
internal class FormDocIrGenerationExtension(configuration: CompilerConfiguration) : IrGenerationExtension {
    private val messages = configuration.get(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY, MessageCollector.NONE)

    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        moduleFragment.acceptChildrenVoid(object : IrVisitorVoid() {
            override fun visitElement(element: IrElement) { element.acceptChildrenVoid(this) }

            override fun visitClass(declaration: IrClass) {
                if (declaration.hasAnnotation(FqName("me.deference.formdoc.WithDsl"))) {
                    if (!declaration.isData || declaration.typeParameters.isNotEmpty() ||
                        !declaration.hasAnnotation(FqName("me.deference.formdoc.Validatable"))) {
                        messages.report(CompilerMessageSeverity.ERROR,
                            "@WithDsl requires a non-generic @Validatable data class: ${declaration.name}")
                    }
                    val companion = declaration.declarations.filterIsInstance<IrClass>().firstOrNull { it.isCompanion }
                    val hooks = companion?.functions?.filter { it.name.asString() == "dsl" }?.toList().orEmpty()
                    val valid = hooks.any { function ->
                        val parameterType = function.parameters.singleOrNull { it.kind == IrParameterKind.Regular }?.type as? IrSimpleType
                        function.visibility == org.jetbrains.kotlin.descriptors.DescriptorVisibilities.PUBLIC &&
                            !function.isSuspend && function.typeParameters.isEmpty() &&
                            function.parameters.none { it.kind == IrParameterKind.ExtensionReceiver || it.kind == IrParameterKind.Context } &&
                            function.returnType.isUnit() && parameterType != null && !parameterType.isNullableType() &&
                            parameterType.classFqName?.asString() == "me.deference.formdoc.FormMetadataBuilder" &&
                            parameterType.arguments.singleOrNull()?.typeOrNull == declaration.defaultType
                    }
                    if (!valid) messages.report(CompilerMessageSeverity.ERROR,
                        "@WithDsl expects public fun dsl(builder: FormMetadataBuilder<${declaration.name}>): Unit in its companion object")
                }
                declaration.acceptChildrenVoid(this)
            }

            override fun visitSimpleFunction(declaration: IrSimpleFunction) {
                if ((declaration.origin as? IrDeclarationOrigin.GeneratedByPlugin)?.pluginKey == WithDslKey) {
                    declaration.body = DeclarationIrBuilder(pluginContext, declaration.symbol).irBlockBody { }
                }
                declaration.acceptChildrenVoid(this)
            }

            override fun visitConstructor(declaration: IrConstructor) {
                if ((declaration.origin as? IrDeclarationOrigin.GeneratedByPlugin)?.pluginKey == WithDslKey) {
                    declaration.body = DeclarationIrBuilder(pluginContext, declaration.symbol).irBlockBody {
                        +irDelegatingConstructorCall(pluginContext.irBuiltIns.anyClass.owner.constructors.single())
                    }
                }
                declaration.acceptChildrenVoid(this)
            }
        })
    }
}
