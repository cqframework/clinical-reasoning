package org.opencds.cqf.fhir.utility.operation;

import static java.util.Objects.requireNonNull;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.opencds.cqf.fhir.utility.Resources;

import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;

class MethodBinder {

    private final Method method;
    private final Operation operation;
    private final Description description;
    private final String name;
    private final String typeName;
    private final Scope scope;
    private final List<ParameterBinder> parameterBinders;
    private final Optional<IdParam> idParam;

    MethodBinder(Method method) {
        this.method = Objects.requireNonNull(method, "Method cannot be null");
        this.operation = Objects.requireNonNull(method.getAnnotation(Operation.class), "Method must be annotated with @Operation");
        this.parameterBinders = Arrays.stream(method.getParameters())
            .map(ParameterBinder::new)
            .collect(Collectors.toList());

        validateParameterBinders(parameterBinders);

        this.name = requireNonNull(operation.name(), "Operation name cannot be null");
        this.idParam = parameterBinders.stream()
            .filter(ParameterBinder::isIdParam)
            .findFirst()
            .flatMap(ParameterBinder::idParam);

        this.typeName = typeNameFor(operation);
        this.scope = idParam.isPresent() ? Scope.INSTANCE : !typeName.isEmpty() ? Scope.TYPE : Scope.SERVER;
        this.description = method.getAnnotation(Description.class);
    }

    Operation operation() {
        return operation;
    }

    String name() {
        return name;
    }

    String typeName() {
        return typeName;
    }

    String canonical() {
        return operation.canonicalUrl() != null ? operation.canonicalUrl() : "";
    }

    Method method() {
        return method;
    }

    String description() {
        return description != null && description.shortDefinition() != null ? description.shortDefinition() : "";
    }

    Scope scope() {
        return scope;
    }

    List<ParameterBinder> parameters() {
        return parameterBinders;
    }

    List<Object> args(IIdType id, IBaseParameters parameters) {
        // The binding process consumes the parameters it binds,
        // so we need to clone the parameters to avoid modifying the original
        var cloned = Resources.clone(parameters);
        var args = new ArrayList<>(parameterBinders.size());
        int paramIndex = 0;

        // If we have an id parameter for the method
        // it's guaranteed to be the first parameter
        if (this.idParam != null) {
            args.add(id);
            paramIndex++;
        }

        for (; paramIndex < parameterBinders.size(); paramIndex++) {
            args.add(parameterBinders.get(paramIndex).value(cloned));
        }

        if (!cloned.isEmpty()) {
            throw new IllegalArgumentException("Parameters were not bound to @Operation invocation: " + cloned);
        }

        return args;
    }

    static void validateParameterBinders(List<ParameterBinder> parameterBinders) {
        var idParamCount = parameterBinders.stream().filter(ParameterBinder::isIdParam).count();
        if (idParamCount > 1) {
            throw new IllegalArgumentException("Method cannot have more than one @IdParam");
        }

        var unboundParamCount = parameterBinders.stream().filter(ParameterBinder::isUnboundParam).count();
        if (unboundParamCount > 1) {
            throw new IllegalArgumentException("Method cannot have more than one @UnboundParam");
        }

        if(idParamCount > 0 && !parameterBinders.get(0).isIdParam()) {
            throw new IllegalArgumentException("If @IdParam is present, it must be the first parameter");
        }

        if (unboundParamCount > 0 && !parameterBinders.get(parameterBinders.size() - 1).isUnboundParam()){
            throw new IllegalArgumentException("If @UnboundParam is present, it must be the last parameter");
        }
    }

    static String typeNameFor(Operation operation) {
        if (operation.type() != null) {
            return operation.type().getSimpleName();
        } else if (operation.typeName() != null) {
            return operation.typeName();
        } else {
            return "";
        }
    }


    public Callable<? extends IBaseResource> bind(Object provider, IIdType id, IBaseParameters parameters) {
        var args = args(id, parameters);
        return () -> {
            return (IBaseResource) method.invoke(provider, args);
        };
    }
}
