package me.deference.formdoc

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.reflect.KProperty1
import kotlin.time.Duration.Companion.milliseconds

@Suppress("UNCHECKED_CAST")
class FormState<T>(
    val initialData: T,
    private val metadata: FormMetadata<T>?,
    private val scope: CoroutineScope? = null
) {

    private class FieldEntry<V>(
        val state: FieldState<V>,
        val validators: MutableList<FieldValidator<V>>,
        val metadata: FieldMetadata
    )

    private val fields = mutableMapOf<KProperty1<T, *>, FieldEntry<*>>()

    init {
        val allProps = metadata?.fields ?: emptyList()
        allProps.forEach { prop ->
            val validators = ((metadata?.validators?.get(prop) ?: emptyList()) as List<FieldValidator<Any?>>)
                .map { bindValidator(it) }
            val meta = metadata?.fieldMetadata?.get(prop) ?: FieldMetadata()
            val value = getInitial(prop)
            val entry = FieldEntry(
                state = FieldState(
                    initial = value,
                    label = meta.label,
                    required = meta.required,
                    enabled = meta.enabled
                ),
                validators = validators.toMutableList(),
                metadata = meta
            )
            fields[prop] = entry
        }

        scope?.let{ validationScope ->
            fields.forEach { (prop, entry) ->
                setupDebouncedValidation(entry,validationScope)
            }
        }
    }

    var isValid by mutableStateOf(true)
        private set

    private fun setupDebouncedValidation(
        entry: FieldEntry<*>,
        scope: CoroutineScope
    ) {
        scope.launch {
            snapshotFlow { entry.state.value }
                .collectLatest {
                    delay(300.milliseconds)
                    isValid = computeFormValidity()
                }
        }
    }

    private fun computeFormValidity(): Boolean {
        refreshDynamicStates()
        return fields.all { (prop, _) ->
            validateFieldWithoutUpdatingFieldState(prop) == null
        }
    }

    fun <V> getInitial(prop: KProperty1<T, V>): V = prop.get(initialData)

    private fun <V> bindValidator(validator: FieldValidator<V>): FieldValidator<V> {
        if (validator is FormStateValidator<*, *>) {
            // Metadata can be shared; each form needs its own cross-field binding.
            return (validator as FormStateValidator<Any, V>).boundTo(this as FormState<Any>)
        }
        if (validator is FormStateAware<*>) {
            (validator as FormStateAware<T>).setFormState(this)
        }
        return validator
    }

    @Suppress("UNCHECKED_CAST")
    fun <V> addValidator(prop: KProperty1<T, V>, validator: FieldValidator<V>) {
        val entry = fields[prop] as? FieldEntry<V>
        if (entry != null) {
            entry.validators.add(bindValidator(validator))
        } else {
            registerFieldInternal(prop, listOf(validator))
        }
    }

    private fun <V> registerFieldInternal(
        prop: KProperty1<T, V>,
        validators: List<FieldValidator<V>>,
        meta: FieldMetadata = FieldMetadata()
    ): FieldState<V> {
        val existing = fields[prop] as? FieldEntry<V>
        val state = existing?.state ?: FieldState(
            initial = getInitial(prop),
            label = meta.label,
            required = meta.required,
            enabled = meta.enabled
        )
        val entry = FieldEntry(
            state = state,
            validators = validators.map { bindValidator(it) }.toMutableList(),
            metadata = meta
        )
        fields[prop] = entry
        scope?.let { validationScope ->
            setupDebouncedValidation(entry, validationScope)
        }
        return entry.state
    }

    fun <V> getState(
        prop: KProperty1<T, V>,
    ): FieldState<V> {
        val existing = fields[prop] as? FieldEntry<V>
        val state = existing?.state ?: registerFieldInternal(prop, emptyList())
        refreshDynamicState(prop)
        return state
    }

    fun refreshDynamicStates() {
        fields.forEach { (_, entry) ->
            val meta = entry.metadata
            
            if (meta.enabledIf.isNotBlank()) {
                val dependentProp = fields.keys.find { it.name == meta.enabledIf }
                if (dependentProp != null) {
                    val dependentValue = fields[dependentProp]?.state?.value
                    entry.state.enabled = dependentValue as? Boolean ?: meta.enabled
                }
            }
            
            if (meta.requiredIf.isNotBlank()) {
                val dependentProp = fields.keys.find { it.name == meta.requiredIf }
                if (dependentProp != null) {
                    val dependentValue = fields[dependentProp]?.state?.value
                    entry.state.required = dependentValue as? Boolean ?: meta.required
                }
            }
        }
    }

    private fun refreshDynamicState(prop: KProperty1<T, *>) {
        val entry = fields[prop]
        if (entry != null) {
            val meta = entry.metadata
            if (meta.enabledIf.isNotBlank()) {
                val dependentProp = fields.keys.find { it.name == meta.enabledIf }
                if (dependentProp != null) {
                    val dependentValue = fields[dependentProp]?.state?.value
                    entry.state.enabled = dependentValue as? Boolean ?: meta.enabled
                }
            }

            if (meta.requiredIf.isNotBlank()) {
                val dependentProp = fields.keys.find { it.name == meta.requiredIf }
                if (dependentProp != null) {
                    val dependentValue = fields[dependentProp]?.state?.value
                    entry.state.required = dependentValue as? Boolean ?: meta.required
                }
            }
        }
    }


    private fun validateFieldWithoutUpdatingFieldState(prop: KProperty1<T, *>): String? {
        refreshDynamicStates()

        val entryAny = fields[prop] ?: return null
        val entry = entryAny as FieldEntry<Any?>

        if (!entry.state.enabled) {
            return null
        }

        val value = entry.state.value

        val validators = entry.validators.toMutableList()
        if (entry.state.required) {
            validators.add(0, Validators.notNull(entry.state.label + " is required"))
            if (value is String) {
                validators.add(1, Validators.notBlank(entry.state.label + " is required") as FieldValidator<Any?>)
            }
        }

        val firstError = validators.firstNotNullOfOrNull { it.validate(value) }

        return firstError
    }


    private fun validateField(prop: KProperty1<T, *>): Boolean {
        refreshDynamicStates()

        val entryAny = fields[prop] ?: return true
        val entry = entryAny as FieldEntry<Any?>
        
        if (!entry.state.enabled) {
            entry.state.error = null
            return true
        }

        val value = entry.state.value
        val validators = entry.validators.toMutableList()
        val firstError = validators.firstNotNullOfOrNull { it.validate(value) }

        entry.state.error = firstError
        return firstError == null
    }

    /**
     * Clears all the data from form fields
     */
    fun clear() {
        fields.forEach { (propAny, entryAny) ->
            val prop = propAny
            val entry = entryAny as FieldEntry<Any?>
            entry.state.value = prop.get(initialData)
            entry.state.error = null
            entry.state.touched = false
        }
    }

    fun validateAll(): Boolean {
        var ok = true
        for (propName in fields.keys) {
            if (!validateField(propName)) ok = false
        }
        return ok
    }

    fun values(): Map<KProperty1<T, *>, Any?> =
        fields.mapValues { (_, entryAny) ->
            val entry = entryAny as FieldEntry<Any?>
            entry.state.value
        }

    fun errors(): Map<KProperty1<T, *>, String?> =
        fields.mapValues { (_, entryAny) ->
            entryAny.state.error
        }

    fun submit(
        onValid: (Map<KProperty1<T, *>, Any?>) -> Unit,
        onInvalid: (Map<KProperty1<T, *>, String?>) -> Unit
    ) {
        if (validateAll()) {
            onValid(values())
        } else {
            onInvalid(errors())
        }
    }
}
