package me.deference.formsample

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import me.deference.formsample.customer.CustomerForm

@Composable
@Preview
fun App() {
    MaterialTheme {
        CustomerForm()
    }
}