package me.deference.formdoc

import kotlin.reflect.KProperty1

import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
annotation class Validatable

@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.PROPERTY)
annotation class NotBlank(val message: String = "This field is required")

@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.PROPERTY)
annotation class Email(val message: String = "Invalid email address")

@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.PROPERTY)
annotation class Pattern(
    val regex: String,
    val message: String = "Invalid format"
)

@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.PROPERTY)
annotation class ValidatedBy(val validator: KClass<out FieldValidator<*>>)

@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.PROPERTY)
annotation class Min(
    val value: Int,
    val message: String = "Value too small"
)

@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.PROPERTY)
annotation class Max(
    val value: Int,
    val message: String = "Value too large"
)

fun interface FieldValidator<T> {
    /**
     * @return error message, or null if valid
     */
    fun validate(value: T): String?
}

interface FormMetadata<T : Any> {
    val validators: Map<KProperty1<T, *>, List<FieldValidator<*>>>
}