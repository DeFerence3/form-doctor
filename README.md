# FormDoc

FormDoc is a Kotlin Multiplatform form-validation library for Compose. Define
field rules with annotations, add cross-field validation with a companion DSL,
and bind form state to your UI.

- [Setup](SETUP.MD): dependencies, Gradle plugin, KSP, targets, and local publishing.
- [Usage](USAGE.MD): annotations, companion DSL, Compose integration, and API reference.

## Simple usage

After completing [setup](SETUP.MD), define a model and validate its current values:

```kotlin
package example

import formdoc.registry.FormMetadataRegistry
import me.deference.formdoc.*

@Validatable
data class Login(
    @NotBlank(message = "Email is required")
    @Email
    val email: String = ""
)

fun validateLogin(): String? {
    val form = FormState(
        initialData = Login(),
        metadata = requireNotNull(FormMetadataRegistry.get<Login>())
    )

    form.getState(Login::email).value = "person@example.com"
    form.validateAll()
    return form.errors()[Login::email] // null when valid
}
```

Use `rememberFormState` in Compose. Add `@WithDsl` when a rule needs other fields;
see the [companion DSL example](USAGE.MD#companion-dsl-with-withdsl).

Licensed under the [Apache License 2.0](LICENSE.md).
