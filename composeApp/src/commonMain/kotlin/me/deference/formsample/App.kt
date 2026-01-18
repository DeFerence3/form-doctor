package me.deference.formsample

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import me.deference.formdoc.FormMetadata
import me.deference.formsample.customer.Address
import me.deference.formsample.customer.CustomerForm
import me.deference.formsample.customer.CustomerFormModel

@Composable
@Preview
fun App(
    metadata: FormMetadata<CustomerFormModel>? = null,
    addresMetadata: FormMetadata<Address>? = null,
) {
    MaterialTheme {
        CustomerForm(metadata,addresMetadata)
    }
}