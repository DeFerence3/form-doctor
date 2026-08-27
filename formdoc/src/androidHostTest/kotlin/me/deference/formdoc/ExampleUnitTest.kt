package me.deference.formdoc

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun dslValidation_isCorrect() {
        val metadata = formConfig<TestFormModel> {
            field(TestFormModel::name) {
                label = "Name"
                required = true
                notBlank("Name required")
                minLength(3, "Name too short")
            }
            field(TestFormModel::age) {
                label = "Age"
                min(18, "Must be adult")
            }
            field(TestFormModel::email) {
                label = "Email"
                email("Invalid email")
            }
            field(TestFormModel::taxNumber) {
                label = "Tax Number"
                enabledIf = "requiresTax"
                requiredIf = "requiresTax"
                validateWithForm { value, formState ->
                    val requires = formState.getState(TestFormModel::requiresTax).value
                    if (requires && value.length < 5) "Tax number too short" else null
                }
            }
        }

        // Test basic validations
        val initial = TestFormModel(name = "Ab", age = 17, email = "wrong-email")
        val formState = FormState(initial, metadata)

        // Validate all
        val isValid = formState.validateAll()
        assertEquals(false, isValid)

        val errors = formState.errors()
        assertEquals("Name too short", errors[TestFormModel::name])
        assertEquals("Must be adult", errors[TestFormModel::age])
        assertEquals("Not a valid email", errors[TestFormModel::email])

        // Test conditional enabledIf/requiredIf
        val taxState = formState.getState(TestFormModel::taxNumber)
        assertEquals(false, taxState.enabled) // initially requiresTax is false

        // Turn requiresTax to true
        val requiresTaxState = formState.getState(TestFormModel::requiresTax)
        requiresTaxState.value = true
        formState.refreshDynamicStates()

        assertEquals(true, taxState.enabled)
        assertEquals(true, taxState.required)

        // Run validateAll again with tax enabled
        formState.validateAll()
        assertEquals("Tax number too short", formState.errors()[TestFormModel::taxNumber])

        // Test addValidator dynamically
        formState.addValidator(TestFormModel::name, FieldValidator { value ->
            if (value == "forbidden") "Forbidden name" else null
        })
        formState.getState(TestFormModel::name).value = "forbidden"
        formState.validateAll()
        assertEquals("Forbidden name", formState.errors()[TestFormModel::name])
    }
}

data class TestFormModel(
    val name: String = "",
    val age: Int = 0,
    val email: String? = null,
    val requiresTax: Boolean = false,
    val taxNumber: String = ""
)