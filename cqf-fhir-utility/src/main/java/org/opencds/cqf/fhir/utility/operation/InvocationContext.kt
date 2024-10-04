package org.opencds.cqf.fhir.utility.operation

import org.opencds.cqf.fhir.api.Repository
import java.util.function.Function

/*
* The InvocationContext is a wrapper around a method annotated with @Operation. It contains a reference to the method
* and to a factory for the class that contains this method. This allows instantiating the class as needed so that the
* referenced method can be invoked.
*
* This allows the OperationRegistry to pass the Repository to the class instance.
*/
internal data class InvocationContext<T : Any>(val factory: Function<Repository?, T>, val methodBinder: MethodBinder)
