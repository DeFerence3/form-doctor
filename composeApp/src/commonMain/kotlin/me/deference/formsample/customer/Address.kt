package me.deference.formsample.customer

import me.deference.formdoc.NotBlank
import me.deference.formdoc.Validatable
import me.deference.formdoc.getValueFromMap
import kotlin.reflect.KProperty1

@Validatable
data class Address(
    @NotBlank("Required Bro")
    val usage: String = "",
    var line1: String = "",
    var line2: String = "",
    var line3: String = "",
    var place: String = "",
    var pin: String = "",
    var state: StateMaster? = null,
    var district: DistrictMaster? = null,
    @NotBlank("Required Bro")
    val mobileNumber: String = "",
    val mobileNumber2: String = "",
    val email: String = "",
    val country: String = "",
    var isDefault: Boolean = false,
){
    companion object{
        fun fromMap(values:  Map<KProperty1<Address, *>, Any?>): Address{
            return Address(
                usage = values.getValueFromMap(Address::usage, "BILLING"),
                line1 = values.getValueFromMap(Address::line1, ""),
                line2 = values.getValueFromMap(Address::line2, ""),
                line3 = values.getValueFromMap(Address::line3, ""),
                place = values.getValueFromMap(Address::place, ""),
                pin = values.getValueFromMap(Address::pin, ""),
                isDefault = values.getValueFromMap(Address::isDefault, false),
                state = values.getValueFromMap(Address::state, null as StateMaster?),
                district = values.getValueFromMap(Address::district, null as DistrictMaster?),
                mobileNumber = values.getValueFromMap(Address::mobileNumber, ""),
                mobileNumber2 = values.getValueFromMap(Address::mobileNumber2, ""),
                email = values.getValueFromMap(Address::email, ""),
                country = values.getValueFromMap(Address::country, "")
            )
        }
    }
}