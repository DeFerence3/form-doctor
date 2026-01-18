package me.deference.formdoc

import kotlin.reflect.KProperty1

@Suppress("UNCHECKED_CAST")
fun <T,K> Map<KProperty1<K, *>, Any?>.getValueFromMap(
    property: KProperty1<K, *>,
    string: T,
): T {
    return try{
        this[property]?.let { it as T } ?: string
    }catch (e: Exception){
        string
    }
}