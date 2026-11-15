package com.example.micromobility.core.validation

data class PasswordValidationStatus(
    val hasMinLength: Boolean,
    val hasNoWhitespace: Boolean,
    val hasLetter: Boolean,
    val hasDigit: Boolean,
    val hasSpecialChar: Boolean
) {
    val isValid: Boolean
        get() = hasMinLength && hasNoWhitespace && hasLetter && hasDigit && hasSpecialChar
}

object PasswordValidator {
    const val MIN_LENGTH = 8

    fun evaluate(password: String): PasswordValidationStatus {
        return PasswordValidationStatus(
            hasMinLength = password.length >= MIN_LENGTH,
            hasNoWhitespace = password.none { it.isWhitespace() },
            hasLetter = password.any { it.isLetter() },
            hasDigit = password.any { it.isDigit() },
            hasSpecialChar = password.any { !it.isLetterOrDigit() && !it.isWhitespace() }
        )
    }

    fun validate(password: String): String? {
        val status = evaluate(password)
        return when {
            !status.hasMinLength ->
                "Password must be at least $MIN_LENGTH characters."
            !status.hasNoWhitespace ->
                "Password must not contain spaces."
            !status.hasLetter ->
                "Password must contain at least one letter."
            !status.hasDigit ->
                "Password must contain at least one digit."
            !status.hasSpecialChar ->
                "Password must contain at least one special character (e.g. @, #, !, .)."
            else -> null
        }
    }
}