package me.deference.formsample.customer

import me.deference.formdoc.FieldValidator
import me.deference.formdoc.NotBlank
import me.deference.formdoc.Validatable
import me.deference.formdoc.ValidatedBy
import me.deference.formdoc.registry.FormMetadataRegistry

@Validatable
data class CustomerFormModel(
    val id: Long? = null,
    val code: String? = "",
    @NotBlank
    val name: String = "",
    val tradeLegalName: String = "",
    val isActive: Boolean = true,
    val isServiceParty: Boolean = false,
    val isAlsoSupplier: Boolean = false,
    val isTaxRegistered: Boolean = false,
    val taxRegistrationNumber: String = "",
    val panNo: String = "",
    @NotBlank
    val contactPersonName: String = "",
    @NotBlank("Please Provide Contact Number")
    val contactNumber: String = "",
    val creditDays: String = "",
    val creditLimit: String = "",
    val isBlockForPurchase: Boolean = false,
    val isBlockForPayment: Boolean = false,
    val isBlockForSales: Boolean = false,
    val isBlockForReceipt: Boolean = false,
    @ValidatedBy(NotNullValidator::class)
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


class NotNullValidator: FieldValidator<Any?> {
    override fun validate(value: Any?): String? {
        if (value == null) return "This field is required"
        return null
    }
}