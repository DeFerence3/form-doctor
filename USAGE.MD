# Using FormDoc

FormDoc keeps editable values and validation results in a `FormState`. Your data
class supplies the initial values; annotations and an optional companion DSL
supply the rules. Complete [SETUP.MD](SETUP.MD) before using generated metadata.

This guide describes the current **0.0.7 checkout**, including the `@WithDsl`
compiler feature. The [current constraints](#current-constraints) section records
behavior that matters when integrating this version.

## Contents

- [Create and validate a form](#create-and-validate-a-form)
- [Annotation reference](#annotation-reference)
- [Companion DSL with WithDsl](#companion-dsl-with-withdsl)
- [Conditional fields and business rules](#conditional-fields-and-business-rules)
- [Custom validators](#custom-validators)
- [Compose integration](#compose-integration)
- [Validation timing and errors](#validation-timing-and-errors)
- [Read values and submit](#read-values-and-submit)
- [Reset and manage field state](#reset-and-manage-field-state)
- [Standalone DSL and migration](#standalone-dsl-and-migration)
- [API reference](#api-reference)
- [Test your rules](#test-your-rules)
- [Current constraints](#current-constraints)

## Create and validate a form

Place a model in the configured KMP module's `commonMain` source set. Prefer
public, top-level, non-generic data classes with public properties. Defaults are
convenient for empty forms but are not required when you pass an initial instance.
Annotated models must use a named Kotlin package; the generator does not support
models in the default (empty) package.

```kotlin
package example

import me.deference.formdoc.Email
import me.deference.formdoc.FormElement
import me.deference.formdoc.NotBlank
import me.deference.formdoc.Validatable

@Validatable
data class Profile(
    @FormElement(label = "Name")
    @NotBlank(message = "Name is required")
    val name: String = "",

    @FormElement(label = "Email")
    @NotBlank(message = "Email is required")
    @Email
    val email: String = ""
)
```

Create state using the **model type** in the registry lookup:

```kotlin
import example.Profile
import formdoc.registry.FormMetadataRegistry
import me.deference.formdoc.FormState

val form = FormState(
    initialData = Profile(),
    metadata = requireNotNull(FormMetadataRegistry.get<Profile>()) {
        "Profile metadata was not generated; check the KSP setup"
    }
)

form.getState(Profile::name).value = "Sam"
form.getState(Profile::email).value = "sam@example.com"

val valid = form.validateAll() // true
val nameError = form.errors()[Profile::name] // null
```

Property references such as `Profile::email` identify fields. Editing a field
updates `FormState`; it does not mutate or replace `initialData`. Calling
`Profile().copy(...)` outside the form also does not update its fields.

`FormMetadataRegistry.get<T>()` returns `null` for an unregistered type. Require
non-null metadata when you expect annotation validation. A form constructed with
`metadata = null` has no generated fields or rules until you register them.

## Annotation reference

Import annotations from `me.deference.formdoc`. Put field annotations on Kotlin
properties, as shown above. They target `PROPERTY`; do not change them to Java
`@field:` annotations.

| Annotation | Use | Parameters and defaults |
| --- | --- | --- |
| `@Validatable` | Ask KSP to generate model metadata and a registry entry. | None. |
| `@WithDsl` | Add the companion DSL hook to a `@Validatable` data class. | None. Requires the FormDoc compiler plugin. |
| `@NotBlank` | Reject null, empty, or whitespace-only strings. | `message = "This field is required"`. |
| `@NotNull` | Reject a null value. | `message = "This field is required"`. |
| `@Email` | Check a string using the runtime's email expression. | `message = "Invalid email address"`; see the message caveat below. |
| `@Pattern` | Require a non-null string to match the entire expression. Null is accepted. | Required `regex`; `message = "Invalid format"`. |
| `@Min` | Inclusive numeric lower bound; use non-null `Int` properties for the annotation path. | Required `value: Int`; `message = "Value too small"`. |
| `@Max` | Inclusive numeric upper bound; use non-null `Int` properties for the annotation path. | Required `value: Int`; `message = "Value too large"`. |
| `@ValidatedBy` | Instantiate a custom `FieldValidator`. | Required validator class, for example `CodeValidator::class`. |
| `@FormElement` | Set presentation and dynamic-state metadata. | See below. |

For numeric types other than `Int`, use typed DSL validators. For nullable
numbers, use a null-aware custom rule rather than `@Min` or `@Max`.

Example combining constraints:

```kotlin
package example

import me.deference.formdoc.*

@Validatable
data class Booking(
    @Min(value = 1, message = "Choose at least one seat")
    @Max(value = 8, message = "Choose at most eight seats")
    val seats: Int = 1,

    @Pattern(regex = "[A-Z]{3}", message = "Use three uppercase letters")
    val code: String? = null
)
```

Here a null `code` passes `@Pattern`. Add `@NotNull` or `@NotBlank` if it must be
supplied. There are no `@MinLength`, `@MaxLength`, or `@NotEmpty` annotations in
this version; use the DSL helpers for those rules.

### Field presentation metadata

| `@FormElement` argument | Default | Meaning |
| --- | --- | --- |
| `label` | `""` | Label exposed to the UI. |
| `required` | `true` | Initial required state. |
| `enabled` | `true` | Initial enabled state; disabled fields skip validation. |
| `requiredIf` | `""` | Name of a Boolean property controlling required state. |
| `enabledIf` | `""` | Name of a Boolean property controlling enabled state. |

Without `@FormElement`, the runtime's default `FieldMetadata.required` is `false`.
The standalone DSL also defaults `required` to `false` for a newly added field.
Labels are not inferred from property names.

**Current behavior:** `required` and `requiredIf` affect field state and background
validity checks, but `validateAll()` and `submit()` currently run only registered
validators. Pair required inputs with `@NotBlank`, `@NotNull`, or an explicit
conditional DSL validator to enforce them at submission. Setting `required = false`
also does not remove a `@NotBlank` rule.

## Companion DSL with WithDsl

Use `@WithDsl` to extend annotation-generated metadata with ordinary Kotlin
logic. The hook has this shape:

```kotlin
companion object {
    fun dsl(builder: FormMetadataBuilder<YourModel>) {
        // Register fields or additional validators.
    }
}
```

The compiler supplies an empty implementation if you do not declare one. If
necessary it creates the companion too. A named companion is supported. Existing
companion members and a source-defined hook are preserved.

The hook must be public, non-suspending, non-generic, return `Unit`, and take
exactly one `FormMetadataBuilder<YourModel>` parameter. It must not be an
extension or use context parameters. If a companion declares `dsl` but no
compatible hook, compilation reports an error instead of silently ignoring it.
The parameter name itself need not be `builder`.

### Password confirmation example

The following model is also used by the Compose and testing examples below:

```kotlin
package example

import me.deference.formdoc.*

@Validatable
@WithDsl
data class Registration(
    @FormElement(label = "Email")
    @NotBlank(message = "Email is required")
    @Email
    val email: String = "",

    @FormElement(label = "Password")
    @NotBlank(message = "Password is required")
    val password: String = "",

    @FormElement(label = "Confirm password")
    @NotBlank(message = "Confirm your password")
    val confirmPassword: String = ""
) {
    companion object {
        fun dsl(builder: FormMetadataBuilder<Registration>) {
            builder.field(Registration::password) {
                minLength(8, "Use at least eight characters")
            }
            builder.field(Registration::confirmPassword) {
                validateWithForm { value, form ->
                    val password = form.getState(Registration::password).value
                    if (value != password) "Passwords do not match" else null
                }
            }
        }
    }
}
```

Use `FormMetadataRegistry.get<Registration>()` as usual. The generated metadata
invokes `Registration.dsl(builder)` automatically; your UI should not call it
again to register the same rules.

### Hook lifecycle and merging

The hook configures rules **when metadata is constructed**. Its validator lambdas
run later, each time validation reaches them. Read other fields through the
`FormState` passed to `validateWithForm`, so rules see edited values rather than
the original model snapshot.

When a DSL `field(...)` call refers to an annotation-generated field:

- Existing annotation validators remain and run before appended DSL validators.
- Label, required/enabled flags, and condition names are preserved unless changed.
- Repeated field calls append rules without duplicating the field registration.
- Explicit DSL metadata values override that field's previous metadata settings.

For example, inside the hook:

```kotlin
builder.field(Registration::email, label = "Contact email") {
    validate { value ->
        if (value.endsWith("@example.invalid")) "Use a deliverable email" else null
    }
}
```

Changing a label or `required` setting does not remove annotation validators.
There is no remove-validator API. If a rule needs to be conditional, express
that rule conditionally instead of trying to undo an unconditional annotation.

Metadata may be shared by multiple forms: DSL cross-field validators receive a
separate binding for each `FormState`. Custom mutable validators should still be
designed carefully; the runtime does not clone arbitrary `FieldValidator`
implementations.

The empty hook exists in compiled code. It is not inserted into your model's
source file, and editing generated KSP files will not preserve customizations.
Declare your hook in the model's source when you want to customize it.

## Conditional fields and business rules

### Enable a field from a Boolean property

```kotlin
package example

import me.deference.formdoc.*

@Validatable
data class Company(
    val isTaxRegistered: Boolean = false,

    @FormElement(
        label = "Tax number",
        required = false,
        requiredIf = "isTaxRegistered",
        enabledIf = "isTaxRegistered"
    )
    @NotBlank(message = "Tax number is required")
    val taxNumber: String = ""
)
```

When `isTaxRegistered` is false, `taxNumber` becomes disabled and its validators
are skipped. When true, the field is enabled and `@NotBlank` enforces the value.
Bind your input's `enabled` property to its `FieldState.enabled`.

`enabledIf` and `requiredIf` are exact property names, not Kotlin expressions.
They cannot contain `a && b`, negation, or a comparison. A present dependency
with a non-Boolean value falls back to the static flag; a missing property leaves
the current flag unchanged. Use valid Boolean property names to avoid ambiguity.

Call `form.refreshDynamicStates()` after programmatic changes when you need the
flags immediately. `getState()` refreshes the requested field, validation
refreshes dynamic flags, and `FormContent` observes value changes in Compose.

### Combine several fields

Keep a field enabled while making its rule conditional with `validateWithForm`:

```kotlin
package example

import me.deference.formdoc.*

@Validatable
@WithDsl
data class Delivery(
    val shippingRequired: Boolean = false,
    val country: String = "",
    @FormElement(label = "Postal code", required = false)
    val postalCode: String = ""
) {
    companion object {
        fun dsl(builder: FormMetadataBuilder<Delivery>) {
            builder.field(Delivery::postalCode) {
                validateWithForm { value, form ->
                    val shipping = form.getState(Delivery::shippingRequired).value
                    val country = form.getState(Delivery::country).value
                    when {
                        !shipping -> null
                        value.isBlank() -> "Postal code is required for shipping"
                        country == "IN" && !value.matches(Regex("[0-9]{6}")) ->
                            "Use a six-digit postal code"
                        else -> null
                    }
                }
            }
        }
    }
}
```

The rule is attached to `postalCode`, so its error appears on that field. There
is no separate form-level error collection. Attach a multi-field rule to the
field where the user should see the error.

Validators should be synchronous, deterministic checks that return an error
message or `null`. Do not modify field values from a validator. For remote or
suspending validation, perform the request in application code and handle its
result explicitly; validators do not await network requests or catch exceptions.

## Custom validators

Implement `FieldValidator<V>`, matching the property's type and nullability:

```kotlin
package example

import me.deference.formdoc.*

class CodeValidator : FieldValidator<String> {
    override fun validate(value: String): String? =
        if (value.matches(Regex("[A-Z]{3}-[0-9]{4}"))) null
        else "Use a code such as ABC-1234"
}

@Validatable
data class Product(
    @ValidatedBy(CodeValidator::class)
    val code: String = ""
)
```

The processor emits `CodeValidator()`. Use an accessible class with a callable
zero-argument constructor. Kotlin `object` validators and validators requiring
constructor arguments cannot be instantiated by this annotation path.

As an alternative to `@ValidatedBy`, register a configurable validator or lambda
in the companion DSL:

```kotlin
// Inside a Product companion hook when Product also has @WithDsl.
builder.field(Product::code) {
    validate(CodeValidator())
    validate { value -> if (value == "SYS-0000") "Reserved code" else null }
}
```

For a validator using current values from other fields, use `validateWithForm`.
For nullable properties, handle null in the validator itself or add an earlier
null check. Because the runtime stores heterogeneous validators using erased
types, a mismatched custom validator can fail at runtime.

### Register a rule at runtime

```kotlin
form.addValidator(Profile::name) { value ->
    if (value.equals("admin", ignoreCase = true)) "This name is reserved" else null
}
```

Here `form` is the `FormState<Profile>` from the first example. Adding a validator
appends it to an existing field. For an unregistered property, it registers a
field initialized from `initialData`. Register rules once during form setup;
calling `addValidator` directly on every Compose recomposition appends duplicates.

## Compose integration

Use `rememberFormState` to retain form state across recompositions. This example
uses the `example.Registration` model above and Material 3 components:

```kotlin
package example

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import formdoc.registry.FormMetadataRegistry
import me.deference.formdoc.FieldState
import me.deference.formdoc.FormContent
import me.deference.formdoc.rememberFormState

@Composable
fun RegistrationForm(onRegistered: (Registration) -> Unit) {
    val metadata = remember {
        requireNotNull(FormMetadataRegistry.get<Registration>())
    }
    val form = rememberFormState(initial = Registration(), metadata = metadata)

    FormContent(form) {
        Column {
            RegistrationInput(form.getState(Registration::email))
            RegistrationInput(form.getState(Registration::password), password = true)
            RegistrationInput(form.getState(Registration::confirmPassword), password = true)

            Button(onClick = {
                form.submit(
                    onValid = { values ->
                        onRegistered(
                            Registration(
                                email = values[Registration::email] as String,
                                password = values[Registration::password] as String,
                                confirmPassword = values[Registration::confirmPassword] as String
                            )
                        )
                    },
                    onInvalid = { /* FieldState.error already contains each result. */ }
                )
            }) {
                Text("Register")
            }
            Button(onClick = { form.clear() }) { Text("Reset") }
        }
    }
}

@Composable
private fun RegistrationInput(field: FieldState<String>, password: Boolean = false) {
    OutlinedTextField(
        value = field.value,
        onValueChange = {
            field.value = it
            field.touched = true
        },
        label = { Text(field.label) },
        enabled = field.enabled,
        isError = field.error != null,
        supportingText = { field.error?.let { Text(it) } },
        visualTransformation = if (password) PasswordVisualTransformation()
            else VisualTransformation.None
    )
}
```

`FormContent` provides a `FormScope` receiver with `state` and `getState(...)`, and
observes values to refresh dynamic flags. It does not create input components
or automatically submit the form. You can also bind fields directly without
`FormContent`; refresh dynamic flags yourself when needed.

`rememberFormState` uses `remember(initial)`. A different initial value creates
a new form, but changing only `metadata` or `scope` does not recreate it. Keep
metadata stable for a form's lifetime. If you deliberately replace rules, use an
appropriate Compose `key` around the form or create a new `FormState`.

## Validation timing and errors

| Action | Result |
| --- | --- |
| Set `field.value` | Updates observable state. Does not immediately update `field.error`. |
| `validateAll()` | Validates all registered enabled fields, writes errors, and returns a Boolean. |
| `submit(...)` | Calls `validateAll()`, then invokes one callback. |
| `errors()` | Reads the stored results; does not perform validation. |
| Background observation with a coroutine scope | Debounces changes by about 300 ms and recomputes `isValid`; does not write field errors. |

`rememberFormState` supplies a remembered coroutine scope by default. A manually
constructed `FormState` uses no scope unless you pass one, so it has no background
validity observation. Manage the lifetime of a manually supplied scope yourself.

`isValid` starts as `true`, and the current `validateAll()` implementation does
not assign to it. Use the return value of `validateAll()` or the callbacks of
`submit()` as the authoritative result for an explicit submission. Do not use
the initial or possibly stale `isValid` alone to accept data.

Each field stops at its first error. Annotation validators precede appended DSL
validators; DSL validators run in registration order. Avoid depending on a
particular ordering among different annotations for business logic.

Disabled fields are treated as valid and their errors are cleared on explicit
validation. Disabling a field does not erase its value or remove it from submitted
values. `touched` is not updated automatically and does not gate validation.

For errors that refresh as the user types, explicitly validate after changing a
value, optionally only after the first submission:

```kotlin
// Example onValueChange body; hasSubmitted is application-managed state.
field.value = newValue
field.touched = true
if (hasSubmitted) form.validateAll()
```

This revalidates cross-field rules too. There is currently no public single-field
validation method. Revalidation is also needed to clear a previous error after
its value becomes valid.

## Read values and submit

`values()` and `errors()` are keyed by property references, not string names:

```kotlin
val currentName: String = form.getState(Profile::name).value
val initialName: String = form.getInitial(Profile::name)
val allValues = form.values() // Map<KProperty1<Profile, *>, Any?>
val allErrors = form.errors() // Map<KProperty1<Profile, *>, String?>
val nameError: String? = allErrors[Profile::name]
```

The error map includes registered fields whose error is `null`; filter those
entries if you want only failures. Values include disabled and untouched fields.

Submission validates first and invokes exactly one callback:

```kotlin
form.submit(
    onValid = { values ->
        val updated = Profile(
            name = values[Profile::name] as String,
            email = values[Profile::email] as String
        )
        saveProfile(updated) // Your application function.
    },
    onInvalid = { errors ->
        val failures = errors.filterValues { it != null }
        showValidationSummary(failures) // Your application function.
    }
)
```

The callbacks are synchronous and are not suspending functions. Launch a coroutine
from application code if saving requires one. FormDoc does not generate a filled
copy of the data class, serialize values, or perform persistence.

For typed reads with a fallback, the runtime also provides:

```kotlin
import me.deference.formdoc.getValueFromMap

val name: String = form.values().getValueFromMap(Profile::name, "")
val optionalName: String? = form.values().getValueFromMap(Profile::name)
```

These helpers use casts internally. Property-based `getState(...).value` is the
most direct typed read when you already have a `FormState`.

## Reset and manage field state

Despite its name, `clear()` **restores initial values**. It does not force strings
to empty or nullable properties to null:

```kotlin
val editing = FormState(
    initialData = Profile(name = "Original", email = "original@example.com"),
    metadata = requireNotNull(FormMetadataRegistry.get<Profile>())
)
editing.getState(Profile::name).value = "Changed"
editing.clear()
check(editing.getState(Profile::name).value == "Original")
```

Reset clears field errors and sets `touched = false`. It does not remove validators,
reset labels, or explicitly recompute `isValid`. Dynamic flags refresh through
the usual observation/refresh path. Supply blank/default initial values when
resetting should produce a blank form.

`FieldState` exposes mutable `value`, `error`, `touched`, `label`, `required`, and
`enabled` properties. UI code controls how to display them. Dynamic metadata can
overwrite required/enabled flags during refresh; changing those flags does not
edit the metadata rules. Manually assigned errors can be overwritten by the next
validation pass.

## Standalone DSL and migration

For manually configured forms, use `formConfig` without annotations, KSP, or a
registry. Register every field you want the form to manage:

```kotlin
import me.deference.formdoc.*

data class ManualLogin(val password: String = "", val confirmation: String = "")

val metadata = formConfig<ManualLogin> {
    field(ManualLogin::password, label = "Password") {
        notBlank("Password is required")
        minLength(8)
    }
    field(ManualLogin::confirmation, label = "Confirm password") {
        validateWithForm { value, form ->
            if (value == form.getState(ManualLogin::password).value) null
            else "Passwords do not match"
        }
    }
}

val manualForm = FormState(ManualLogin(), metadata)
```

Calling `getState` for an unregistered property registers it with its initial value
and default metadata, but does not discover annotations. Explicitly register
dependency fields before validation so validators do not need to add fields while
the form is iterating through its registry.

To extend existing metadata manually:

```kotlin
val extended = formConfig(metadata) {
    field(ManualLogin::password) { maxLength(100) }
}
```

`FormMetadataBuilder.build()` snapshots field lists, validator lists, and metadata
maps. Later builder edits do not change already built metadata. Validator objects
themselves are not deep-copied, except for per-form bindings of the built-in
cross-field validator.

To migrate a standalone configuration to the companion workflow:

1. Add `@Validatable` and `@WithDsl` to the model and complete the plugin/KSP setup.
2. Move its `field(...)` configuration into `fun dsl(builder: FormMetadataBuilder<Model>)`.
3. Replace manual metadata lookup with `FormMetadataRegistry.get<Model>()`.
4. Remove duplicate DSL rules if those rules are now supplied by annotations.

The hook receives an already seeded builder. Do not create an unrelated
`formConfig { ... }` inside the hook and discard its return value; configure the
passed builder so the generated metadata receives your rules.

## API reference

### DSL validation helpers

Import `me.deference.formdoc.*` to bring the extension functions into scope.

| Helper inside `field(...)` | Supported values | Default message |
| --- | --- | --- |
| `notBlank(message)` | `String`, `String?` | `Required` |
| `notNull(message)` | Nullable values | `Required` |
| `notEmpty(message)` | The provided extension targets `List<Any>?`; use `validate` for other collection shapes. | `Add at least one item` |
| `min(bound, message)` | Non-null `Number` values; bound uses the same numeric type. | `Must be >= <bound>` |
| `max(bound, message)` | Non-null `Number` values; bound uses the same numeric type. | `Must be <= <bound>` |
| `minLength(length, message)` | `String`, `String?` | `Minimum length is <length>` |
| `maxLength(length, message)` | `String`, `String?` | `Maximum length is <length>` |
| `email(message)` | `String`, `String?` | `Invalid email address` |
| `pattern(regex, message)` | `String`, `String?` | `Invalid format` |
| `validate(validator)` | A matching `FieldValidator<V>` | Defined by the validator. |
| `validate { value -> ... }` | Any property type | Return a message or `null`. |
| `validateWithForm { value, form -> ... }` | Any property type with access to current form state | Return a message or `null`. |

Numeric helpers compare via `toDouble()` and should not be used for exact
high-precision numeric comparisons; use a typed custom rule when precision
matters. Null string length is treated as zero. Nullable `pattern` accepts null.

You can also use validator factories directly, for example
`Validators.notBlank("Required")` or `Validators.min(1)`, with `validate(...)`
or `addValidator(...)` when their types match. `Validators.email` requires a
message argument even though the DSL `email()` extension supplies a default.

### Form state

| API | Purpose |
| --- | --- |
| `FormState(initialData, metadata, scope = null)` | Create form state outside Compose. |
| `rememberFormState(initial, metadata = null, scope = rememberCoroutineScope())` | Remember state in Compose. Pass generated metadata explicitly for annotations. |
| `getState(property)` | Read or register typed field state and refresh that field's dynamic flags. |
| `getInitial(property)` | Read the original model's value for that property. |
| `addValidator(property, validator)` | Append a rule, registering the field if needed. |
| `refreshDynamicStates()` | Refresh Boolean-name-based required/enabled flags. |
| `validateAll()` | Validate, store errors, and return whether enabled registered fields passed. |
| `values()` | Snapshot current registered values. |
| `errors()` | Snapshot stored registered errors. |
| `submit(onValid, onInvalid)` | Validate and call the matching callback. |
| `clear()` | Restore initial values, clear errors, and reset touched flags. |
| `isValid` | Observable background validity; see timing caveats above. |

`FormMetadata<T>` exposes `fields`, `validators`, and `fieldMetadata`.
`FormScope<T>` exposes the form as `state` plus a typed `getState(property)` helper.
The unfinished `ValidatableInt` interface is not needed for the supported setup
or for registry lookup.

## Test your rules

Place tests in the configured module's `commonTest` and keep the annotated model
in `commonMain`. These tests use the `example.Registration` model above:

```kotlin
package example

import formdoc.registry.FormMetadataRegistry
import me.deference.formdoc.FormState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RegistrationRulesTest {
    @Test
    fun confirmationUsesCurrentPassword() {
        val metadata = assertNotNull(FormMetadataRegistry.get<Registration>())
        val form = FormState(
            Registration("sam@example.com", "password123", "password123"),
            metadata
        )
        assertTrue(form.validateAll())

        form.getState(Registration::password).value = "different123"
        assertFalse(form.validateAll())
        assertEquals("Passwords do not match", form.errors()[Registration::confirmPassword])

        form.getState(Registration::confirmPassword).value = "different123"
        assertTrue(form.validateAll())
    }

    @Test
    fun sharedMetadataKeepsFormsIndependent() {
        val metadata = assertNotNull(FormMetadataRegistry.get<Registration>())
        val first = FormState(Registration("a@example.com", "password123", "password123"), metadata)
        val second = FormState(Registration("b@example.com", "another123", "another123"), metadata)

        first.getState(Registration::confirmPassword).value = "wrong"
        assertFalse(first.validateAll())
        assertTrue(second.validateAll())
    }
}
```

Run the appropriate target task, for example `./gradlew :shared:jvmTest`. Direct
`validateAll()` tests do not need a coroutine scope or a debounce delay. Also test
empty inputs, conditional branches, disabled fields, custom-validator nullability,
and reset behavior that your UI depends on.

## Current constraints

These guides describe implemented behavior, not a promise of APIs planned for a
future release:

- `@WithDsl` requires a non-generic `@Validatable` data class. Use public,
  top-level models for the current KSP generator; nested/generic model generation
  and module-wide registries spanning dependency modules are not supported here.
- KSP still generates annotations and registries. The compiler plugin currently
  generates the companion hook, not the entire validation system.
- Annotation arguments are inserted into generated Kotlin source without general
  string escaping. Quotes, backslashes, dollar signs, or newlines in messages,
  labels, or regexes can break or change generated code. Put such values in the
  source-defined DSL/custom validators; prefer expressions like `[0-9]` instead
  of backslash-based regex escapes in annotations for this version.
- `@NotNull` generation uses the property's raw declaration name; generic types
  such as `List<String>?` need a typed DSL null check or custom validator instead.
  Numeric annotation generation is safest with non-null `Int` properties.
- `@Email` and nullable DSL `email()` use `Validators.email`: blank/null values
  return the supplied message, but malformed nonblank values currently return
  `Not a valid email`. The non-null DSL `email()` uses its supplied message for
  any mismatch. Use a custom validator when exact messaging must be consistent.
- Required-state metadata and explicit submission validation currently differ;
  enforce required values with actual validators as described above.
- Background `isValid` and displayed errors have different update paths. Use
  `validateAll()` or `submit()` for an immediate decision.
- There is no public validator-removal API, async validation API, single-field
  validation method, or generated conversion from state back to a data class.

For installation and compiler errors, see [setup troubleshooting](SETUP.MD#troubleshooting).
