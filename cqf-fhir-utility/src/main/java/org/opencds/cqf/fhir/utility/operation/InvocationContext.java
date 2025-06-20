package org.opencds.cqf.fhir.utility.operation;

import static java.util.Objects.requireNonNull;

import ca.uhn.fhir.repository.IRepository;
import jakarta.annotation.Nonnull;
import java.util.function.Function;

/*
 * The InvocationContext is a wrapper around a method annotated with @Operation. It contains a reference to the method
 * and to a factory for the class that contains this method. This allows instantiating the class as needed so that the
 * referenced method can be invoked.
 *
 * This allows the OperationRegistry to pass the Repository to the class instance.
 */
class InvocationContext<T> {

    private final MethodBinder methodBinder;
    private final Function<IRepository, T> factory;

    InvocationContext(Function<IRepository, T> factory, MethodBinder methodBinder) {
        this.factory = requireNonNull(factory, "factory cannot be null");
        this.methodBinder = requireNonNull(methodBinder, "methodBinder cannot be null");
    }

    @Nonnull
    MethodBinder methodBinder() {
        return this.methodBinder;
    }

    @Nonnull
    Function<IRepository, T> factory() {
        return this.factory;
    }
}
