package org.opencds.cqf.fhir.utility.operation

import ca.uhn.fhir.repository.IRepository
import java.util.*
import java.util.function.Function

/*
 * The InvocationContext is a wrapper around a method annotated with @Operation. It contains a reference to the method
 * and to a factory for the class that contains this method. This allows instantiating the class as needed so that the
 * referenced method can be invoked.
 *
 * This allows the OperationRegistry to pass the Repository to the class instance.
 */
internal class InvocationContext<T>(
    private val factory: Function<IRepository?, T?>,
    private val methodBinder: MethodBinder,
) {
    fun methodBinder(): MethodBinder {
        return this.methodBinder
    }

    fun factory(): Function<IRepository?, T?> {
        return this.factory
    }
}
