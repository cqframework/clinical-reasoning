package org.opencds.cqf.fhir.utility.operation;

import java.util.function.Function;
import org.opencds.cqf.fhir.api.Repository;

/*
 * An OperationClosure is a wrapper around a method annotated with @Operation. It contains a reference to the method
 * and to a factory for the class that contains this method. This allows instantiating the class as needed
 */
class OperationClosure {

    private final MethodBinder methodBinder;
    private final Function<Repository, Object> factory;

    OperationClosure(Function<Repository, Object> factory, MethodBinder methodBinder) {
        this.methodBinder = methodBinder;
        this.factory = factory;
    }

    MethodBinder methodBinder() {
        return this.methodBinder;
    }

    Function<Repository, Object> factory() {
        return this.factory;
    }
}
