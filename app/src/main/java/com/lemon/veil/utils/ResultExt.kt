package com.lemon.veil.utils

import kotlinx.coroutines.CancellationException

/**
 * Executes a suspendable block safely, catching exceptions and returning a Result.
 * Ignores CancellationException to allow coroutines to be cancelled properly.
 */
suspend fun <T> safeApiCall(block: suspend () -> T): Result<T> {
    return try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }
}
