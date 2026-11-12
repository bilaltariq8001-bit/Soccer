package com.innovativeinside.pitchside.core

import java.io.IOException
import java.io.InterruptedIOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlinx.serialization.SerializationException

/** Typed errors the UI knows how to explain. Every failure in the data layer becomes one of these. */
sealed class AppError(override val message: String, cause: Throwable? = null) : Exception(message, cause) {
    /** No connection or DNS failure. */
    class Network(cause: Throwable? = null) : AppError("You're offline. Check your connection and try again.", cause)
    class Timeout(cause: Throwable? = null) : AppError("The server took too long to answer. Try again.", cause)
    class Unauthorized : AppError("Your session has ended. Please sign in again.")
    class Forbidden : AppError("You don't have access to this.")
    class NotFound(what: String = "This item") : AppError("$what is no longer available.")
    class Validation(message: String) : AppError(message)
    class RateLimited : AppError("Too many requests right now. Try again in a minute.")
    class Server(val code: Int) : AppError("Something went wrong on our side ($code). Try again shortly.")
    class Parse(cause: Throwable? = null) : AppError("We received data we couldn't read. Try again later.", cause)
    class Unknown(cause: Throwable? = null) : AppError("Something went wrong. Try again.", cause)

    /** True when retrying the same call may succeed. */
    val retryable: Boolean
        get() = this is Network || this is Timeout || this is Server || this is RateLimited || this is Unknown || this is Parse

    companion object {
        fun from(t: Throwable): AppError = when (t) {
            is AppError -> t
            is SocketTimeoutException -> Timeout(t)
            is InterruptedIOException -> Timeout(t)
            is UnknownHostException -> Network(t)
            is IOException -> Network(t)
            is SerializationException -> Parse(t)
            is IllegalArgumentException -> Parse(t)
            else -> Unknown(t)
        }

        fun fromHttp(code: Int, serverMessage: String? = null): AppError = when (code) {
            401 -> Unauthorized()
            403 -> Forbidden()
            404 -> NotFound()
            400, 422 -> Validation(serverMessage ?: "The request was not valid.")
            429 -> RateLimited()
            in 500..599 -> Server(code)
            else -> Unknown()
        }
    }
}
