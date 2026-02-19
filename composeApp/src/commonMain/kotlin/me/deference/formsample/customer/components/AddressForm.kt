package me.deference.formsample.customer.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.deference.formdoc.FieldValidator
import me.deference.formdoc.FormContent
import me.deference.formdoc.FormMetadata
import me.deference.formdoc.rememberFormState
import me.deference.formsample.FormCheckbox
import me.deference.formsample.FormField
import me.deference.formsample.customer.Address
import kotlin.reflect.KProperty1


@Composable
fun AddressForm(
    initialAddress: Address,
    onDismiss: () -> Unit,
    onValid: (Address) -> Unit
) {
    val formState = rememberFormState(initialAddress,Address.metadata)
    SectionCard("Address ${initialAddress.usage}") {
        FormContent(state = formState) {
            val addressType = formState.getState(Address::usage)
            AddressTypeSelector(
                modifier = Modifier,
                selected = addressType.value,
                onSelect = {
                    addressType.value = it
                }
            )

            FormField(
                prop = Address::line1,
                label = "Line 1"
            )

            FormField(
                prop = Address::line2,
                label = "Line 2"
            )

            FormField(
                prop = Address::line3,
                label = "Line 3"
            )

            FormField(
                prop = Address::place,
                label = "Place"
            )

            FormField(
                prop = Address::email,
                label = "Email"
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                val state = formState.getState(Address::state)
                StateSelector(
                    modifier = Modifier
                        .weight(0.5f),
                    selected = state.value,
                    onSelect = {
                        state.value = it
                    }
                )
                val district = formState.getState(Address::district)
                DistrictSelector(
                    modifier = Modifier
                        .weight(0.5f),
                    stateMaster = state.value,
                    selected = district.value,
                    required = true,
                    onSelect = {
                        district.value = it
                    }
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                FormField(
                    modifier = Modifier
                        .weight(0.5f),
                    prop = Address::mobileNumber,
                    label = "Mobile Number"
                )
                FormField(
                    modifier = Modifier
                        .weight(0.5f),
                    prop = Address::mobileNumber2,
                    label = "Mobile Number 2"
                )
            }

            FormField(
                prop = Address::pin,
                label = "Pin"
            )

            FormCheckbox(
                prop = Address::isDefault,
                label = "Is Default"
            )

            Row {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.weight(0.5f)
                ) { Text("Dismiss") }
                Button(
                    onClick = {
                        formState.submit(
                            onValid = { values ->
                                onValid(Address.fromMap(values))
                            },
                            onInvalid = {
                                println("Invalid form")
                            },
                        )
                    },
                    modifier = Modifier.weight(0.5f)
                ) { Text("Save") }
            }
        }
    }
}
