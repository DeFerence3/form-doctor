package me.deference.formdoc

import kotlin.reflect.KProperty1

@Suppress("UNCHECKED_CAST")
fun <T,K> Map<KProperty1<K, *>, Any?>.getValueFromMap(
    property: KProperty1<K, *>,
    default: T,
): T {
    return try{
        this[property]?.let { it as T } ?: default
    }catch (e: Exception){
        default
    }
}

@Suppress("UNCHECKED_CAST")
fun <T,K> Map<KProperty1<K, *>, Any?>.getValueFromMap(
    property: KProperty1<K, *>
): T? {
    return try{
        this[property]?.let { it as T }
    }catch (e: Exception){
        null
    }
}