package me.deference.formdoc

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable

class FormScope<T : Any>(
    val state: FormState<T>
)

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
    scope.content()
}