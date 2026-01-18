package me.deference.formsample.customer.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuBoxScope
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> DropDownSelector(
    modifier: Modifier = Modifier,
    onSelect: (T) -> Unit,
    onExpandedChange: (Boolean) -> Unit,
    expanded: Boolean,
    list: List<T>,
    label: (T) -> String,
    anchor: @Composable ExposedDropdownMenuBoxScope.() -> Unit
) {
    ExposedDropdownMenuBox(
        modifier = modifier,
        expanded = expanded,
        onExpandedChange = {
            onExpandedChange(it)
        },
    ){
        anchor(this)
        ExposedDropdownMenu(
            expanded = expanded,
            modifier = Modifier,
            onDismissRequest = { onExpandedChange(false) },
            shape = RoundedCornerShape(2.dp)
        ) {
            list.map {
                DropdownMenuItem(
                    text = {
                        Text(label(it))
                    },
                    onClick = {
                        onSelect(it)
                        onExpandedChange(false)
                    }
                )
            }
        }
    }
}