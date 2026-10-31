package me.deference.formdoc

import kotlin.reflect.KProperty1
import kotlin.jvm.JvmName

interface FormStateAware<T> {
    fun setFormState(formState: FormState<T>)
}

class FormStateValidator<T : Any, V>(
    private val block: (V, FormState<T>) -> String?
) : FieldValidator<V>, FormStateAware<T> {
    private var formStateRef: FormState<T>? = null

    override fun setFormState(formState: FormState<T>) {
        formStateRef = formState
    }

    override fun validate(value: V): String? {
        val state = formStateRef ?: return null
        return block(value, state)
    }

    internal fun boundTo(formState: FormState<T>): FormStateValidator<T, V> =
        FormStateValidator(block).also { it.setFormState(formState) }
}

class FormMetadataBuilder<T : Any>(base: FormMetadata<T>? = null) {
    private val fields = base?.fields?.distinct()?.toMutableList() ?: mutableListOf()
    private val validators = base?.validators?.mapValues { it.value.toMutableList() }?.toMutableMap()
        ?: mutableMapOf<KProperty1<T, *>, MutableList<FieldValidator<*>>>()
    private val fieldMetadata = base?.fieldMetadata?.toMutableMap()
        ?: mutableMapOf<KProperty1<T, *>, FieldMetadata>()

    fun <V> field(
        property: KProperty1<T, V>,
        label: String = fieldMetadata[property]?.label ?: "",
        required: Boolean = fieldMetadata[property]?.required ?: false,
        enabled: Boolean = fieldMetadata[property]?.enabled ?: true,
        requiredIf: String = fieldMetadata[property]?.requiredIf ?: "",
        enabledIf: String = fieldMetadata[property]?.enabledIf ?: "",
        block: (FieldMetadataBuilder<T, V>.() -> Unit)? = null
    ) {
        if (property !in fields) fields.add(property)
        
        val builder = FieldMetadataBuilder<T, V>(
            label = label,
            required = required,
            enabled = enabled,
            requiredIf = requiredIf,
            enabledIf = enabledIf
        )
        if (block != null) {
            builder.block()
        }
        
        fieldMetadata[property] = FieldMetadata(
            label = builder.label,
            required = builder.required,
            enabled = builder.enabled,
            requiredIf = builder.requiredIf,
            enabledIf = builder.enabledIf
        )
        validators.getOrPut(property) { mutableListOf() }.addAll(builder.validators)
    }

    fun build(): FormMetadata<T> {
        val builtFields = fields.toList()
        val builtValidators = validators.mapValues { it.value.toList() }
        val builtMetadata = fieldMetadata.toMap()
        return object : FormMetadata<T> {
            override val fields: List<KProperty1<T, *>> = builtFields
            override val validators: Map<KProperty1<T, *>, List<FieldValidator<*>>> = builtValidators
            override val fieldMetadata: Map<KProperty1<T, *>, FieldMetadata> = builtMetadata
        }
    }
}

class FieldMetadataBuilder<T : Any, V>(
    var label: String = "",
    var required: Boolean = false,
    var enabled: Boolean = true,
    var requiredIf: String = "",
    var enabledIf: String = ""
) {
    val validators = mutableListOf<FieldValidator<V>>()

    fun validate(validator: FieldValidator<V>) {
        validators.add(validator)
    }

    fun validate(block: (V) -> String?) {
        validators.add(FieldValidator { block(it) })
    }

    fun validateWithForm(block: (V, FormState<T>) -> String?) {
        validators.add(FormStateValidator(block))
    }
}

fun <T : Any> formConfig(block: FormMetadataBuilder<T>.() -> Unit): FormMetadata<T> {
    val builder = FormMetadataBuilder<T>()
    builder.block()
    return builder.build()
}

/** Applies companion DSL rules after the annotation-generated metadata. */
fun <T : Any> formConfig(base: FormMetadata<T>, block: FormMetadataBuilder<T>.() -> Unit): FormMetadata<T> {
    val builder = FormMetadataBuilder(base)
    builder.block()
    return builder.build()
}

// Extension functions for standard validators

fun <T : Any> FieldMetadataBuilder<T, String?>.notBlank(message: String = "Required") {
    validate(Validators.notBlank(message))
}

@JvmName("notBlankNonNull")
fun <T : Any> FieldMetadataBuilder<T, String>.notBlank(message: String = "Required") {
    validate { value ->
        if (value.isBlank()) message else null
    }
}

fun <T : Any, V> FieldMetadataBuilder<T, V?>.notNull(message: String = "Required") {
    validate(Validators.notNull(message))
}

fun <T : Any> FieldMetadataBuilder<T, List<Any>?>.notEmpty(message: String = "Add at least one item") {
    validate(Validators.notEmpty(message))
}

fun <T : Any, V : Number> FieldMetadataBuilder<T, V>.min(min: V, message: String = "Must be >= $min") {
    validate(Validators.min(min, message))
}

fun <T : Any, V : Number> FieldMetadataBuilder<T, V>.max(max: V, message: String = "Must be <= $max") {
    validate(Validators.max(max, message))
}

fun <T : Any> FieldMetadataBuilder<T, String?>.minLength(min: Int, message: String = "Minimum length is $min") {
    validate(Validators.minLength(min, message))
}

@JvmName("minLengthNonNull")
fun <T : Any> FieldMetadataBuilder<T, String>.minLength(min: Int, message: String = "Minimum length is $min") {
    validate { value ->
        if (value.length < min) message else null
    }
}

fun <T : Any> FieldMetadataBuilder<T, String?>.maxLength(max: Int, message: String = "Maximum length is $max") {
    validate(Validators.maxLength(max, message))
}

@JvmName("maxLengthNonNull")
fun <T : Any> FieldMetadataBuilder<T, String>.maxLength(max: Int, message: String = "Maximum length is $max") {
    validate { value ->
        if (value.length > max) message else null
    }
}

fun <T : Any> FieldMetadataBuilder<T, String?>.email(message: String = "Invalid email address") {
    validate(Validators.email(message))
}

@JvmName("emailNonNull")
fun <T : Any> FieldMetadataBuilder<T, String>.email(message: String = "Invalid email address") {
    validate { value ->
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$"
        if (!value.matches(emailRegex.toRegex())) message else null
    }
}

fun <T : Any> FieldMetadataBuilder<T, String?>.pattern(regex: String, message: String = "Invalid format") {
    validate(Validators.pattern(regex, message))
}

@JvmName("patternNonNull")
fun <T : Any> FieldMetadataBuilder<T, String>.pattern(regex: String, message: String = "Invalid format") {
    validate { value ->
        if (!value.matches(regex.toRegex())) message else null
    }
}
