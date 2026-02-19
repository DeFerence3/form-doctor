package me.deference.formdoc

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.CoroutineScope

class FormScope<T : Any>(
    val state: FormState<T>
) {
    fun <V> getState(prop: kotlin.reflect.KProperty1<T, V>) = state.getState(prop)
}

@Composable
fun <T : Any> rememberFormState(
    initial: T,
    metadata: FormMetadata<T>? = null,
    scope: CoroutineScope = rememberCoroutineScope()
): FormState<T> {
    return remember(initial) { FormState(initialData = initial, metadata = metadata,scope) }
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