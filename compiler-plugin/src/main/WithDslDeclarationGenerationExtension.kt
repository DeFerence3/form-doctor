package me.deference.formdoc.compiler

import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.DirectDeclarationsAccess
import org.jetbrains.kotlin.fir.extensions.*
import org.jetbrains.kotlin.fir.extensions.predicate.LookupPredicate
import org.jetbrains.kotlin.fir.plugin.createCompanionObject
import org.jetbrains.kotlin.fir.plugin.createDefaultPrivateConstructor
import org.jetbrains.kotlin.fir.plugin.createMemberFunction
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.constructClassLikeType
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames

internal object WithDslKey : GeneratedDeclarationKey()

@OptIn(SymbolInternals::class, DirectDeclarationsAccess::class)
internal class WithDslDeclarationGenerationExtension(session: FirSession) : FirDeclarationGenerationExtension(session) {
    private val predicate = LookupPredicate.create { annotated(FqName("me.deference.formdoc.WithDsl")) }
    private val dslName = Name.identifier("dsl")
    private val builderId = ClassId.topLevel(FqName("me.deference.formdoc.FormMetadataBuilder"))

    override fun FirDeclarationPredicateRegistrar.registerPredicates() {
        register(predicate)
    }

    override fun getNestedClassifiersNames(classSymbol: FirClassSymbol<*>, context: NestedClassGenerationContext): Set<Name> =
        if (session.predicateBasedProvider.matches(predicate, classSymbol) &&
            (classSymbol as? FirRegularClassSymbol)?.companionObjectSymbol == null
        ) setOf(SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT) else emptySet()

    override fun generateNestedClassLikeDeclaration(owner: FirClassSymbol<*>, name: Name, context: NestedClassGenerationContext): FirClassLikeSymbol<*>? =
        if (name == SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT && session.predicateBasedProvider.matches(predicate, owner))
            createCompanionObject(owner, WithDslKey).symbol else null

    private fun modelFor(companion: FirClassSymbol<*>): FirRegularClassSymbol? {
        if ((companion as? FirRegularClassSymbol)?.fir?.status?.isCompanion != true) return null
        val outerId = companion.classId.outerClassId ?: return null
        val model = session.symbolProvider.getClassLikeSymbolByClassId(outerId) as? FirRegularClassSymbol ?: return null
        return model.takeIf { session.predicateBasedProvider.matches(predicate, it) }
    }

    private fun hasUserDsl(owner: FirClassSymbol<*>): Boolean = owner.declarationSymbols
        .filterIsInstance<FirNamedFunctionSymbol>()
        .any { it.name == dslName && (it.origin as? FirDeclarationOrigin.Plugin)?.key != WithDslKey }

    override fun getCallableNamesForClass(classSymbol: FirClassSymbol<*>, context: MemberGenerationContext): Set<Name> {
        modelFor(classSymbol) ?: return emptySet()
        return buildSet {
            if (!hasUserDsl(classSymbol)) add(dslName)
            if ((classSymbol.origin as? FirDeclarationOrigin.Plugin)?.key == WithDslKey) add(SpecialNames.INIT)
        }
    }

    override fun generateFunctions(callableId: CallableId, context: MemberGenerationContext?): List<FirNamedFunctionSymbol> {
        val owner = context?.owner ?: return emptyList()
        val model = modelFor(owner) ?: return emptyList()
        if (callableId.callableName != dslName || hasUserDsl(owner)) return emptyList()
        val modelType = model.classId.constructClassLikeType(emptyArray(), false)
        return listOf(createMemberFunction(owner, WithDslKey, dslName, session.builtinTypes.unitType.coneType) {
            valueParameter(Name.identifier("builder"), builderId.constructClassLikeType(arrayOf(modelType), false))
        }.symbol)
    }

    override fun generateConstructors(context: MemberGenerationContext): List<FirConstructorSymbol> =
        if ((context.owner.origin as? FirDeclarationOrigin.Plugin)?.key == WithDslKey)
            listOf(createDefaultPrivateConstructor(context.owner, WithDslKey).symbol) else emptyList()
}
