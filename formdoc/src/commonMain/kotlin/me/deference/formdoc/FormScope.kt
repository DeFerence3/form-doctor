package me.deference.formdoc

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable

class FormScope<T : Any>(
    val state: FormState<T>
) {
    fun <V> getState(prop: kotlin.reflect.KProperty1<T, V>) = state.getState(prop)
}

@Composable
fun <T : Any> rememberFormState(
    initial: T,
    metadata: FormMetadata<T>? = null
): FormState<T> {
    return remember(initial) { FormState(initialData = initial, metadata = metadata) }
}

@Composable
fun <T : Any> FormContent(
    state: FormState<T>,
    content: @Composable FormScope<T>.() -> Unit
) {
    val scope = remember(state) { FormScope(state) }
    
    // Automatically refresh dynamic states when any field value changes
    androidx.compose.runtime.LaunchedEffect(state) {
        androidx.compose.runtime.snapshotFlow { 
            state.values().values.toList() 
        }.collect {
            state.refreshDynamicStates()
        }
    }
    
    scope.content()
}