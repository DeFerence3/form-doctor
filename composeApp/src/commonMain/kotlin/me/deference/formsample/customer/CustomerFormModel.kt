package me.deference.formsample.customer

import formdoc.registry.FormMetadataRegistry
import me.deference.formdoc.FieldValidator
import me.deference.formdoc.FormElement
import me.deference.formdoc.NotBlank
import me.deference.formdoc.NotNull
import me.deference.formdoc.Validatable
import me.deference.formdoc.ValidatedBy

@Validatable
data class CustomerFormModel(
    val id: Long? = null,
    val code: String? = "",
    @FormElement(label = "Name", required = true)
    val name: String = "",
    @FormElement(label = "Trade Legal Name")
    val tradeLegalName: String = "",
    @FormElement(label = "Is Active")
    val isActive: Boolean = true,
    @FormElement(label = "Is Service Party")
    val isServiceParty: Boolean = false,
    @FormElement(label = "Is Also Supplier")
    val isAlsoSupplier: Boolean = false,
    @FormElement(label = "Is Tax Registered")
    val isTaxRegistered: Boolean = false,
    @FormElement(label = "Tax Registration Number", enabledIf = "isTaxRegistered", requiredIf = "isTaxRegistered")
    val taxRegistrationNumber: String = "",
    @FormElement(label = "PAN", enabledIf = "isTaxRegistered")
    val panNo: String = "",
    @NotBlank
    @FormElement(label = "Contact Person Name")
    val contactPersonName: String = "",
    @NotBlank("Please Provide Contact Number")
    @FormElement(label = "Contact Number")
    val contactNumber: String = "",
    val creditDays: String = "",
    val creditLimit: String = "",
    val isBlockForPurchase: Boolean = false,
    val isBlockForPayment: Boolean = false,
    val isBlockForSales: Boolean = false,
    val isBlockForReceipt: Boolean = false,
    @NotNull("Select Currency")
    val currency: CurrencyMaster? = null,
    @ValidatedBy(AddressValidator::class)
    val addresses: List<Address> = emptyList()
){
    companion object {
        val metadata = FormMetadataRegistry.get<CustomerFormModel>()
    }
}


class AddressValidator : FieldValidator<List<Any?>> {
    override fun validate(value: List<Any?>): String? {
        if (value.isEmpty()) return "At least one address required"
        return null
    }
}