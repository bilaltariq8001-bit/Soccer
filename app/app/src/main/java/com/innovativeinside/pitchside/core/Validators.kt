package com.innovativeinside.pitchside.core

/** Form validation rules, shared by the auth screens and their tests. Returns null when valid. */
object Validators {
    private val emailRegex = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    fun email(value: String): String? = when {
        value.isBlank() -> "Enter your email."
        !emailRegex.matches(value.trim()) -> "Enter a valid email, like name@example.com."
        else -> null
    }

    fun password(value: String): String? = when {
        value.isEmpty() -> "Enter your password."
        value.length < 8 -> "Use at least 8 characters."
        value.none { it.isDigit() } || value.none { it.isLetter() } -> "Use letters and at least one number."
        else -> null
    }

    fun loginPassword(value: String): String? = if (value.isEmpty()) "Enter your password." else null

    fun displayName(value: String): String? = when {
        value.isBlank() -> "Enter a name."
        value.trim().length > 40 -> "Keep it under 40 characters."
        else -> null
    }

    fun confirm(password: String, confirm: String): String? = if (password != confirm) "Passwords don't match." else null
}
