package me.deference.formsample.customer.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
fun <T> Selector(
    selected: T?,
    onSelect: (T) -> Unit,
    state: SelectorStatus<T>,
    label: (T) -> String,
    selectorTag: String,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier,
    required: Boolean = false,
    errorMessage: String? = null
) {
    var expanded by remember { mutableStateOf(false) }
    when (state) {
        is SelectorStatus.Loading -> {
            Text("Loading $selectorTag...", modifier = modifier.padding(8.dp))
        }

        is SelectorStatus.Error -> {
            Text(
                text = state.message,
                color = MaterialTheme.colorScheme.error,
                modifier = modifier.padding(8.dp)
            )
        }

        is SelectorStatus.Success -> {
            val textState = rememberTextFieldState(if (selected != null) label(selected) else "")
            val rawList = state.data
            var filteredList by remember { mutableStateOf(rawList) }
            LaunchedEffect(Unit){
                snapshotFlow { textState.text }
                    .debounce(500)
                    .distinctUntilChanged()
                    .collectLatest { text ->
                        filteredList = if (text.isNotEmpty()) rawList.filter { label(it).contains(other = text,ignoreCase = true) } else rawList
                    }
            }

            LaunchedEffect(selected){
                if (selected != null) {
                    textState.setTextAndPlaceCursorAtEnd(label(selected))
                }else{
                    textState.clearText()
                }
            }
            DropDownSelector(
                modifier = modifier,
                onSelect = {
                    onSelect(it)
                },
                onExpandedChange = { expanded = it },
                list = filteredList.take(8),
                expanded = expanded,
                label = { label(it) },
                anchor = {
                    TextField(
                        modifier = Modifier
                            .onFocusChanged { focusState ->
                                expanded = focusState.hasFocus
                                if (selected != null && !focusState.hasFocus) {
                                    textState.setTextAndPlaceCursorAtEnd(label(selected))
                                }
                            }
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true)
                            .onPreviewKeyEvent { keyEvent ->
                                if (keyEvent.type == KeyEventType.KeyDown) {
                                    when (keyEvent.key) {
                                        Key.Enter -> {
                                            if (keyEvent.isCtrlPressed) {
                                                onSearchClick()
                                                true
                                            } else {
                                                false
                                            }
                                        }

                                        else -> false
                                    }
                                } else false
                            },
                        state = textState,
                        label = { Text(selectorTag) },
                        trailingIcon = {
                            IconButton(
                                onClick = onSearchClick
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search Icon"
                                )
                            }
                        },
                        isError = errorMessage != null,
                        supportingText = errorMessage?.let { { Text(it)} }
                    )
                }
            )
        }
    }
}

sealed interface SelectorStatus<out T> {
    data object Loading: SelectorStatus<Nothing>
    data class Success<T>(val data: List<T>): SelectorStatus<T>
    data class Error(val message: String): SelectorStatus<Nothing>
}