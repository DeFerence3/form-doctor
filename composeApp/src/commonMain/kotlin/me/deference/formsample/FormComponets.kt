package me.deference.formsample

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import me.deference.formdoc.FormScope
import kotlin.reflect.KProperty1

@Composable
fun <T : Any> FormScope<T>.FormField(
    prop: KProperty1<T, String?>,
    modifier: Modifier = Modifier,
    inputType: InputType = InputType.STRING,
    label: String? = null,
    enabled: Boolean = true
) {
    val state = this.state.getState(prop)
    TextField(
        modifier = modifier
            .fillMaxWidth(),
        value = state.value ?: "",
        onValueChange = {
            state.value = it
            state.touched = true
        },
        label = label?.let { { Text(it) } },
        isError = state.error != null,
        supportingText = state.error?.let{
            { Text(it) }
        },
        enabled = enabled
    )
}

@Composable
fun <T : Any> FormScope<T>.FormCheckbox(
    prop: KProperty1<T, Boolean>,
    label: String,
    modifier: Modifier = Modifier,
) {
    val state = this.state.getState(prop)
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = state.value, onCheckedChange = {
            state.value = it
            state.touched = true
        })
        Text(label)
    }
}

enum class InputType {
    STRING,NUMBER,DOUBLE
}