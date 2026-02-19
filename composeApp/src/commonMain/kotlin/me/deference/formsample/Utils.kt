package me.deference.formsample

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun DocTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    errorMessage: String? = null,
    enabled: Boolean = true
) {
    OutlinedTextField(
        modifier = modifier,
        value = value,
        onValueChange = onValueChange,
        label = label?.let{ { Text(it) } },
        isError = errorMessage != null,
        supportingText = errorMessage?.let{
            { Text(it) }
        },
        enabled = enabled
    )
}

@Composable
fun DocTextField(
    state: TextFieldState,
    modifier: Modifier = Modifier,
    label: String? = null,
    errorMessage: String? = null,
    enabled: Boolean = true,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    OutlinedTextField(
        modifier = modifier,
        state = state,
        label = label?.let{ { Text(it) } },
        isError = errorMessage != null,
        supportingText = errorMessage?.let{
            { Text(it) }
        },
        enabled = enabled,
        trailingIcon = trailingIcon
    )
}