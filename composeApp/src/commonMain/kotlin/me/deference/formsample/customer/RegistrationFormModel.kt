package me.deference.formsample.customer

import me.deference.formdoc.FormMetadataBuilder
import me.deference.formdoc.NotBlank
import me.deference.formdoc.Validatable
import me.deference.formdoc.WithDsl

/** Example of annotation validation extended by companion cross-field rules. */
@Validatable
@WithDsl
data class RegistrationFormModel(
    @NotBlank val password: String = "",
    @NotBlank val confirmPassword: String = ""
) {
    companion object {
        fun dsl(builder: FormMetadataBuilder<RegistrationFormModel>) {
            builder.field(RegistrationFormModel::confirmPassword) {
                validateWithForm { value, form ->
                    if (value != form.getState(RegistrationFormModel::password).value) {
                        "Passwords do not match"
                    } else null
                }
            }
        }
    }
}
