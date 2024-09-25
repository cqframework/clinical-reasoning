package org.opencds.cqf.fhir.utility.operation;

import static java.util.Objects.requireNonNull;
import static org.opencds.cqf.fhir.utility.operation.ParameterBinder.validate;

import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.Operation;
import jakarta.annotation.Nonnull;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.opencds.cqf.fhir.utility.Resources;
import org.opencds.cqf.fhir.utility.operation.ParameterBinder.Type;

/*
 * This class is responsible for binding a Parameters resource to a Method annotated
 * with @Operation. Once the parameters are bound, the method can be invoked.
 *
 * Each individual parameter of the Method is bound using a ParameterBinder.
 */
class MethodBinder {

    private final Method method;
    private final Operation operation;
    private final Description description;
    private final String name;
    private final String typeName;
    private final Scope scope;
    private final List<ParameterBinder> parameterBinders;

    // Hack to allow for a default instance for the Description annotation
    // If a given method doesn't have a Description annotation, we use this one
    private static @Description final class DefaultDescription {}

    private static final Description DEFAULT_DESCRIPTION = DefaultDescription.class.getAnnotation(Description.class);

    MethodBinder(Method method) {
        this.method = requireNonNull(method, "method cannot be null");
        this.operation =
                requireNonNull(method.getAnnotation(Operation.class), "method must be annotated with @Operation");
        this.description = requireNonNullElse(method.getAnnotation(Description.class), DEFAULT_DESCRIPTION);
        this.name = normalizeName(requireNonNull(operation.name(), "@Operation name cannot be null"));
        this.typeName = typeNameFrom(operation);
        this.parameterBinders = validate(parameterBindersFrom(method));
        this.scope = determineScope(parameterBinders, typeName);
    }

    private static <T> T requireNonNullElse(T obj, T defaultObj) {
        return obj != null ? obj : defaultObj;
    }

    private static Scope determineScope(List<ParameterBinder> parameterBinders, String typeName) {
        boolean hasIdParam = parameterBinders.stream().anyMatch(x -> x.type() == Type.ID);
        if (hasIdParam) {
            return Scope.INSTANCE;
        } else if (typeName.isEmpty()) {
            return Scope.SERVER;
        } else {
            return Scope.TYPE;
        }
    }

    // Normalize the operation name to remove the $ prefix if present
    private static String normalizeName(String name) {
        return name.startsWith("$") ? name.substring(1) : name;
    }

    // Create a list of ParameterBinders from the method's parameters
    private static List<ParameterBinder> parameterBindersFrom(Method method) {
        return Arrays.stream(method.getParameters()).map(ParameterBinder::from).collect(Collectors.toList());
    }

    private static String typeNameFrom(Operation operation) {
        // IBaseResource is the default type for an operation, meaning the type is not specified
        if (operation.type() != IBaseResource.class) {
            return operation.type().getSimpleName();
        } else {
            return operation.typeName();
        }
    }

    private List<Object> args(IIdType id, IBaseParameters parameters) {

        var args = new ArrayList<>(parameterBinders.size());
        int paramIndex = 0;

        // If we have an id parameter for the method
        // it's guaranteed to be the first parameter
        if (this.scope == Scope.INSTANCE) {
            args.add(id);
            paramIndex++;
        }

        // The binding process consumes the parameters it binds,
        // so we need to clone the parameters to avoid modifying the original
        var cloned = parameters != null ? Resources.clone(parameters) : null;

        for (; paramIndex < parameterBinders.size(); paramIndex++) {
            args.add(parameterBinders.get(paramIndex).bind(cloned));
        }

        if (cloned != null && !cloned.isEmpty()) {
            throw new IllegalArgumentException(
                    "Parameters were not bound to @Operation invocation: " + Resources.stringify(cloned));
        }

        return args;
    }

    @Nonnull
    Operation operation() {
        return operation;
    }

    @Nonnull
    String name() {
        return name;
    }

    @Nonnull
    String typeName() {
        return typeName;
    }

    @Nonnull
    String canonicalUrl() {
        return operation.canonicalUrl();
    }

    @Nonnull
    Method method() {
        return method;
    }

    @Nonnull
    String description() {
        return description.value();
    }

    @Nonnull
    Scope scope() {
        return scope;
    }

    @Nonnull
    List<ParameterBinder> parameters() {
        return parameterBinders;
    }

    /**
     * Bind the parameters to the method and return a Callable that can be used to invoke the method.
     *
     * This means that the parameters of the Parameters resource and mapped to the Java method's arguments
     * @param provider the object that contains the method to be invoked
     * @param id the id of the resource that the operation is being invoked on
     * @param parameters the Parameters resource that contains the operation's parameters
     * @return a Callable that can be used to invoke the method
     */
    public Callable<IBaseResource> bind(Object provider, IIdType id, IBaseParameters parameters) {
        var args = args(id, parameters);
        return () -> (IBaseResource) method.invoke(provider, args.toArray());
    }
}
