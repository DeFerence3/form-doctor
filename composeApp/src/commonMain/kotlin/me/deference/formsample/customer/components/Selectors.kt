package me.deference.formsample.customer.components

import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import me.deference.formsample.customer.CurrencyMaster
import me.deference.formsample.customer.DistrictMaster
import me.deference.formsample.customer.StateMaster

@Composable
fun AddressTypeSelector(
    selected: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null,
    required: Boolean = true,
) {
    val state = SelectorStatus.Success(listOf("BILLING","SHIPPING"))
    Selector(
        modifier = modifier,
        selected = selected,
        onSelect = onSelect,
        state = state,
        label = { it },
        selectorTag = "Address Type",
        onSearchClick = { },
        errorMessage = error,
        required = required
    )
}

@Composable
fun StateSelector(
    selected: StateMaster?,
    onSelect: (StateMaster) -> Unit,
    modifier: Modifier = Modifier
) {
    val state = SelectorStatus.Success(listOf(
        StateMaster("Andhra Pradesh"),
        StateMaster("Arunachal Pradesh"),
        StateMaster("Assam"),
    ))
    Selector(
        modifier = modifier,
        selected = selected,
        onSelect = onSelect,
        state = state,
        label = { it.stateName },
        selectorTag = "State",
        onSearchClick = { },
        required = true
    )
}

@Composable
fun DistrictSelector(
    stateMaster: StateMaster?,
    selected: DistrictMaster?,
    onSelect: (DistrictMaster) -> Unit,
    modifier: Modifier = Modifier,
    required: Boolean = false,
    error: String? = null,
) {
    val state = SelectorStatus.Success(listOf(
        DistrictMaster("District 1"),
        DistrictMaster("District 2"),
        DistrictMaster("District 3"),
        DistrictMaster("District 4")
    ))

    if (stateMaster == null) {
        return TextField(
            value = "Select State",
            modifier = modifier,
            onValueChange = { }
        )
    }

    Selector(
        modifier = modifier,
        selected = selected,
        onSelect = onSelect,
        state = state,
        label = { it.districtName },
        selectorTag = "District",
        onSearchClick = { },
        required = required,
        errorMessage = error
    )
}

@Composable
fun CurrencySelector(
    selected: CurrencyMaster?,
    onSelect: (CurrencyMaster) -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null,
    required: Boolean = false,
    label: String = "Currency"
) {

    val state = SelectorStatus.Success(listOf(
        CurrencyMaster("Cur 1","Currency 1"),
        CurrencyMaster("Cur 2","Currency 2"),
        CurrencyMaster("Cur 3","Currency 3"),
        CurrencyMaster("Cur 4","Currency 4")
    ))

    Selector(
        modifier = modifier,
        selected = selected,
        onSelect = onSelect,
        state = state,
        label = { it.name },
        selectorTag = label,
        onSearchClick = { },
        errorMessage = error,
        required = required
    )
}