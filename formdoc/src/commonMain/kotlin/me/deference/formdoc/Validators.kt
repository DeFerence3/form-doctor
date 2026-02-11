package me.deference.formdoc

object Validators {

    fun notBlank(
        message: String = "Required"
    ): FieldValidator<String?> = FieldValidator { value ->
        if (value.isNullOrBlank()) message else null
    }

    fun <T> notNull(
        message: String = "Required"
    ): FieldValidator<T?> = FieldValidator { value ->
        if (value == null) message else null
    }

    fun notEmpty(
        message: String = "Add at least one item"
    ): FieldValidator<List<Any>?> = FieldValidator { value ->
        if (value == null || value.isEmpty()) message else null
    }

    fun <T : Number> min(
        min: T,
        message: String = "Must be >= $min"
    ): FieldValidator<T> = FieldValidator { value ->
        if (value.toDouble() < min.toDouble()) message else null
    }

    fun minLength(
        min: Int,
        message: String = "Minimum length is $min"
    ): FieldValidator<String?> = FieldValidator { value ->
        if ((value?.length ?: 0) < min) message else null
    }

    fun email(message: String): FieldValidator<String?> = FieldValidator { value ->
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$"
        if (value.isNullOrBlank()) message
        else if (!value.matches(emailRegex.toRegex())) "Not a valid email"
        else null
    }
}