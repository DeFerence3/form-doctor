package me.deference.formdoc

import kotlin.reflect.KProperty1

@Suppress("UNCHECKED_CAST")
class FormState<T : Any>(
    val initialData: T,
    private val metadata: FormMetadata<T>?
) {

    private class FieldEntry<V>(
        val state: FieldState<V>,
        val validators: MutableList<FieldValidator<V>>,
        val metadata: FieldMetadata
    )

    private val fields = mutableMapOf<KProperty1<T, *>, FieldEntry<*>>()

    init {
        val allProps = (metadata?.validators?.keys ?: emptySet()) + (metadata?.fieldMetadata?.keys ?: emptySet())
        allProps.forEach { prop ->
            val validators = (metadata?.validators?.get(prop) ?: emptyList()) as List<FieldValidator<Any?>>
            val meta = metadata?.fieldMetadata?.get(prop) ?: FieldMetadata()
            val value = getInitial(prop as KProperty1<T, Any?>)
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
    }

    fun <V> getInitial(prop: KProperty1<T, V>): V = prop.get(initialData)

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
            validators = validators.toMutableList(),
            metadata = meta
        )
        fields[prop] = entry
        return entry.state
    }

    fun <V> getState(
        prop: KProperty1<T, V>,
    ): FieldState<V> {
        val existing = fields[prop] as? FieldEntry<V>
        val state = existing?.state ?: registerFieldInternal(prop, emptyList())
        
        // Initial refresh to set correct dynamic states
        refreshDynamicStates()
        
        return state
    }

    fun refreshDynamicStates() {
        fields.forEach { (prop, entry) ->
            val meta = entry.metadata
            
            if (meta.enabledIf.isNotBlank()) {
                val dependentProp = fields.keys.find { it.name == meta.enabledIf }
                if (dependentProp != null) {
                    val dependentValue = fields[dependentProp]?.state?.value
                    entry.state.enabled = if (dependentValue is Boolean) {
                        dependentValue
                    } else {
                        // If not boolean, we could check if it's not null, or other logic.
                        // For now, let's assume boolean as requested.
                        meta.enabled
                    }
                }
            }
            
            if (meta.requiredIf.isNotBlank()) {
                val dependentProp = fields.keys.find { it.name == meta.requiredIf }
                if (dependentProp != null) {
                    val dependentValue = fields[dependentProp]?.state?.value
                    entry.state.required = if (dependentValue is Boolean) {
                        dependentValue
                    } else {
                        meta.required
                    }
                }
            }
        }
    }

    private fun validateField(prop: KProperty1<T, *>): Boolean {
        // Refresh dynamic states before validation to ensure we have the latest enabled/required status
        refreshDynamicStates()

        val entryAny = fields[prop] ?: return true
        val entry = entryAny as FieldEntry<Any?>
        
        if (!entry.state.enabled) {
            entry.state.error = null
            return true
        }

        val value = entry.state.value

        val validators = entry.validators.toMutableList() as MutableList<FieldValidator<Any?>>
        if (entry.state.required) {
            validators.add(0, Validators.notNull(entry.state.label + " is required"))
            if (value is String) {
                validators.add(1, Validators.notBlank(entry.state.label + " is required") as FieldValidator<Any?>)
            }
        }

        val firstError = validators
            .asSequence()
            .mapNotNull { it.validate(value) }
            .firstOrNull()

        println("Validating ${prop.name} (${validators.size}): $firstError")

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
