package com.inscopelabs.osx.util

/**
 * Universal Result monad for OS/10 console domain operations.
 */
sealed class ConsoleResult<out T> {
    data class Success<out T>(val data: T) : ConsoleResult<T>()
    data class Failure(val error: ConsoleError) : ConsoleResult<Nothing>()

    val isSuccess: Boolean get() = this is Success
    val isFailure: Boolean get() = this is Failure

    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Failure -> null
    }

    fun getOrThrow(): T = when (this) {
        is Success -> data
        is Failure -> throw IllegalStateException("Operation failed: ${error.message} (code=${error.code})")
    }

    inline fun <R> map(transform: (T) -> R): ConsoleResult<R> = when (this) {
        is Success -> Success(transform(data))
        is Failure -> Failure(error)
    }

    inline fun onSuccess(action: (T) -> Unit): ConsoleResult<T> {
        if (this is Success) action(data)
        return this
    }

    inline fun onFailure(action: (ConsoleError) -> Unit): ConsoleResult<T> {
        if (this is Failure) action(error)
        return this
    }
}

data class ConsoleError(
    val code: String,
    val message: String,
    val cause: Throwable? = null,
    val cnsSpec: String = "CNS-1"
)
