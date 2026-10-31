package me.deference.formdoc

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CompanionDslMetadataTest {
    private data class Model(val name: String = "", val active: Boolean = false)

    @Test fun extendingFieldsPreservesAnnotationSettingsAndAppendsRules() {
        val base = formConfig<Model> {
            field(Model::name, label = "Name", required = true, enabled = false,
                requiredIf = "active", enabledIf = "active") { notBlank("Required") }
            field(Model::active)
        }
        val extended = formConfig(base) {
            field(Model::name) { minLength(3, "Too short") }
            field(Model::name) { maxLength(20, "Too long") }
        }
        assertEquals(base.fields, extended.fields)
        assertEquals(base.fieldMetadata, extended.fieldMetadata)
        assertEquals(3, extended.validators[Model::name]?.size)
        assertEquals(1, base.validators[Model::name]?.size)
        val state = FormState(Model("a", active = true), extended)
        assertFalse(state.validateAll())
        assertEquals("Too short", state.errors()[Model::name])
        state.getState(Model::active).value = false
        assertTrue(state.validateAll())
    }

    @Test fun explicitDslOverridesAndLaterBuilderEditsDoNotChangeBuiltMetadata() {
        val base = formConfig<Model> { field(Model::name, label = "Name", required = true, enabledIf = "active") }
        val builder = FormMetadataBuilder(base)
        builder.field(Model::name, label = "Display name", required = false, enabledIf = "")
        val built = builder.build()
        builder.field(Model::active)
        builder.field(Model::name) { notBlank() }
        assertEquals(listOf(Model::name), built.fields)
        assertEquals("Display name", built.fieldMetadata[Model::name]?.label)
        assertEquals(false, built.fieldMetadata[Model::name]?.required)
        assertEquals("", built.fieldMetadata[Model::name]?.enabledIf)
        assertTrue(built.validators[Model::name].orEmpty().isEmpty())
    }
}
