package org.opencds.cqf.fhir.utility.operation;

import jakarta.annotation.Nonnull;
import java.util.function.Function;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.opencds.cqf.fhir.api.Repository;

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

    @Nonnull
    String name() {
        return methodBinder.name();
    }

    Scope scope() {
        return methodBinder.scope();
    }

    String typeName() {
        return methodBinder.typeName();
    }

    IBaseResource execute(Repository repository, IIdType id, IBaseParameters parameters) throws Exception {
        var callable = methodBinder.bind(factory.apply(repository), id, parameters);
        return callable.call();
    }
}
