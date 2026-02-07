package me.deference.formsample.customer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowColumn
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import me.deference.formdoc.FormContent
import me.deference.formdoc.rememberFormState
import me.deference.formsample.FormCheckbox
import me.deference.formsample.FormField
import me.deference.formsample.InputType
import me.deference.formsample.customer.components.AddressForm
import me.deference.formsample.customer.components.CurrencySelector
import me.deference.formsample.customer.components.SectionCard
import me.deference.formsample.customer.components.spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerForm() {
    val scroll = rememberScrollState()
    var selectedAddress: Address? by remember { mutableStateOf(null) }
    val customer = CustomerFormModel()
    val customerFormState = rememberFormState(CustomerFormModel(), CustomerFormModel.metadata)
    var isDiscardDialogShowing by remember { mutableStateOf(false) }
    if (isDiscardDialogShowing) Dialog(
        onDismissRequest = { isDiscardDialogShowing = isDiscardDialogShowing.not() }
    ) {
        SectionCard("Confirmation") {
            Text("Discard Form?")
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.smaller)
            ) {
                OutlinedButton(
                    onClick = {
                        isDiscardDialogShowing = isDiscardDialogShowing.not()
                    },
                    modifier = Modifier.weight(0.5f)
                ) { Text("No") }
                Button(
                    onClick = {

                    },
                    modifier = Modifier.weight(0.5f)
                ) { Text("Yes") }
            }
        }
    }

    var addresses: List<Address> by remember { mutableStateOf(customer.addresses)}
    if (selectedAddress != null) {
        val initialAddress = selectedAddress!!
        Dialog(
            onDismissRequest = { selectedAddress = null }
        ) {
            AddressForm(
                initialAddress = initialAddress,
                onDismiss = {
                    selectedAddress = null
                },
                onValid = { address ->
                    selectedAddress = null
                    val addressMap = addresses.associateBy { it.usage }.toMutableMap()
                    addressMap[address.usage] = address
                    addresses = addressMap.values.toList()
                }
            )
        }
    }

    Scaffold(
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.smaller, Alignment.End)
            ) {
                OutlinedButton(onClick = {
                    customerFormState.clear()
                }){
                    Text("Clear")
                }
                Button(onClick = {
                    customerFormState.submit(
                        onValid = { },
                        onInvalid = { }
                    )
                }){
                    Text("Submit")
                }
            }
        }
    ) { pad ->
        val commonModifier = Modifier.padding(pad)

        FlowColumn(
            Modifier
                .padding(pad)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(scroll),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            FormContent(customerFormState) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(Modifier.weight(1f), Arrangement.spacedBy(16.dp)) {
                        SectionCard("Basic Information") {
                            customer.id?.let{
                                FormField(
                                    prop = CustomerFormModel::code,
                                )
                            }
                            FormField(
                                prop = CustomerFormModel::name,
                            )
                            FormField(
                                prop = CustomerFormModel::tradeLegalName,
                            )
                            val currency = customerFormState.getState(CustomerFormModel::currency)
                            CurrencySelector(
                                selected = currency.value,
                                onSelect = {
                                    currency.value = it
                                },
                                modifier = Modifier,
                                error = currency.error,
                                required = true
                            )
                            customer.id?.let{
                                FormCheckbox(
                                    prop = CustomerFormModel::isActive
                                )
                            }
                            FormCheckbox(
                                prop = CustomerFormModel::isServiceParty
                            )
                            FormCheckbox(
                                prop = CustomerFormModel::isAlsoSupplier
                            )
                        }
                    }

                    Column(Modifier.weight(1f), Arrangement.spacedBy(16.dp)) {
                        SectionCard("Contact Details") {
                            FormField(
                                prop = CustomerFormModel::contactPersonName
                            )
                            FormField(
                                prop = CustomerFormModel::contactNumber
                            )
                        }

                        SectionCard("Credit Settings") {
                            FormField(
                                prop = CustomerFormModel::creditDays,
                                inputType = InputType.NUMBER
                            )
                            FormField(
                                prop = CustomerFormModel::creditLimit,
                                inputType = InputType.DOUBLE
                            )
                        }

                        customer.id?.let{
                            SectionCard("Blocking Options") {
                                FormCheckbox(
                                    prop = CustomerFormModel::isBlockForPurchase,
                                    label = "Block Purchase"
                                )
                                FormCheckbox(
                                    prop = CustomerFormModel::isBlockForPayment,
                                    label = "Block Payment"
                                )
                                FormCheckbox(
                                    prop = CustomerFormModel::isBlockForSales,
                                    label = "Block Sales"
                                )

                                FormCheckbox(
                                    prop = CustomerFormModel::isBlockForReceipt,
                                    label = "Block Receipt"
                                )
                            }
                        }
                    }
                    Column(Modifier.weight(1f), Arrangement.spacedBy(16.dp)) {
                        SectionCard("Tax & Compliance") {
                            FormCheckbox(
                                prop = CustomerFormModel::isTaxRegistered
                            )
                            FormField(
                                prop = CustomerFormModel::taxRegistrationNumber
                            )
                            FormField(
                                prop = CustomerFormModel::panNo
                            )
                        }
                        SectionCard("Addresses") {
                            FlowRow{
                                addresses.forEach { address ->
                                    OutlinedButton(
                                        onClick = {
                                            selectedAddress = address
                                        }
                                    ) {
                                        Text(address.usage)
                                    }
                                }
                            }

                            Button(
                                onClick = { selectedAddress = Address() },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Add Address") }
                        }
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}