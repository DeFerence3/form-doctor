package me.deference.formsample

import formdoc.registry.FormMetadataRegistry
import me.deference.formdoc.FormState
import me.deference.formsample.customer.RegistrationFormModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class WithDslIntegrationTest {
    @Test fun annotationAndCompanionRulesAreAppliedThroughTheRegistry() {
        val metadata = assertNotNull(FormMetadataRegistry.get<RegistrationFormModel>())
        val state = FormState(RegistrationFormModel("secret", "different"), metadata)
        assertFalse(state.validateAll())
        assertEquals("Passwords do not match", state.errors()[RegistrationFormModel::confirmPassword])
        state.getState(RegistrationFormModel::confirmPassword).value = "secret"
        assertTrue(state.validateAll())
        state.getState(RegistrationFormModel::confirmPassword).value = ""
        assertFalse(state.validateAll())
        assertEquals("This field is required", state.errors()[RegistrationFormModel::confirmPassword])
    }
}
