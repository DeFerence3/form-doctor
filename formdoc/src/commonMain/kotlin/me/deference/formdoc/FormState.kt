package me.deference.formdoc

import kotlin.reflect.KProperty1

@Suppress("UNCHECKED_CAST")
class FormState<T : Any>(
    val initialData: T,
    metadata: FormMetadata<T>?
) {

    private class FieldEntry<V>(
        val state: FieldState<V>,
        val validators: MutableList<FieldValidator<V>>
    )

    private val fields = mutableMapOf<KProperty1<T, *>, FieldEntry<*>>()

    init {
        metadata?.validators?.forEach { (prop, validators) ->
            val value = getInitial(prop)
            val entry = FieldEntry(
                state = FieldState(value),
                validators = (validators as List<FieldValidator<Any?>>).toMutableList()
            )
            fields[prop] = entry
        }
    }

    fun <V> getInitial(prop: KProperty1<T, V>): V = prop.get(initialData)

    private fun <V> registerFieldInternal(
        prop: KProperty1<T, V>,
        validators: List<FieldValidator<V>>
    ): FieldState<V> {
        val existing = fields[prop] as? FieldEntry<V>
        val state = existing?.state ?: FieldState(getInitial(prop))
        val entry = FieldEntry(
            state = state,
            validators = validators.toMutableList()
        )
        fields[prop] = entry
        return entry.state
    }

    fun <V> getState(
        prop: KProperty1<T, V>,
    ): FieldState<V> {
        val existing = fields[prop] as? FieldEntry<V>
        return existing?.state ?: registerFieldInternal(prop,emptyList())
    }

    private fun validateField(prop: KProperty1<T, *>): Boolean {
        val entryAny = fields[prop] ?: return true
        val entry = entryAny as FieldEntry<Any?>
        val value = entry.state.value

        val firstError = entry.validators
            .asSequence()
            .mapNotNull { it.validate(value) }
            .firstOrNull()

        println("Validating ${prop.name} (${entry.validators.size}): $firstError")

        entry.state.error = firstError
        return firstError == null
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
