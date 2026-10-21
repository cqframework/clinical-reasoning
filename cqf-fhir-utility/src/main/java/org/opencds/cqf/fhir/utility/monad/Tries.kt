package org.opencds.cqf.fhir.utility.monad

import java.util.function.Supplier

object Tries {
    @JvmStatic
    fun <T : Any> of(error: Exception?, value: T?): Try<T> {
        return if (error != null) ofException(error) else of(value!!)
    }

    @JvmStatic
    fun <T : Any> ofException(error: Exception): Try<T> {
        return Try(error, null)
    }

    @JvmStatic
    fun <T : Any> of(value: T): Try<T> {
        return Try(null, value)
    }

    @JvmStatic
    fun <T : Any> of(valueSupplier: Supplier<T>): Try<T> {
        try {
            return of(valueSupplier.get())
        } catch (e: Exception) {
            return ofException(e)
        }
    }
}
