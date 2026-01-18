package me.deference.formdoc

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class FieldState<T>(
    initial: T
) {
    var value by mutableStateOf(initial)
    var error by mutableStateOf<String?>(null)
    var touched by mutableStateOf(false)
}