package me.deference.formdoc

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class FieldState<T>(
    initial: T,
    label: String = "",
    required: Boolean = false,
    enabled: Boolean = true
) {
    var value by mutableStateOf(initial)
    var error by mutableStateOf<String?>(null)
    var touched by mutableStateOf(false)
    var label by mutableStateOf(label)
    var required by mutableStateOf(required)
    var enabled by mutableStateOf(enabled)
}