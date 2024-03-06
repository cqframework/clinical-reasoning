package org.opencds.cqf.fhir.utility.operation;

import static java.util.Objects.requireNonNull;

import ca.uhn.fhir.rest.annotation.Operation;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import java.util.Arrays;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.opencds.cqf.fhir.api.Repository;

/**
 * That class allows registering of methods annotated with @Operation, and then can be used to execute those
 * operations by name.
 */
public class OperationRegistry {

    private final Multimap<String, OperationClosure> operationMap;

    public OperationRegistry() {
        this.operationMap = MultimapBuilder.hashKeys().arrayListValues().build();
    }

    <T> void register(Class<T> clazz, Function<Repository, T> factory) {
        var methodBinders = Arrays.stream(clazz.getMethods())
                .filter(m -> m.isAnnotationPresent(Operation.class))
                .map(MethodBinder::new)
                .collect(Collectors.toList());

        if (methodBinders.isEmpty()) {
            throw new IllegalArgumentException("No operations found on class " + clazz.getName());
        }

        // TODO: additional validation to ensure conflicting functions are not registered
        for (var methodBinder : methodBinders) {
            var closure = new OperationClosure((Function<Repository, Object>) factory, methodBinder);
            operationMap.put(methodBinder.name(), closure);
        }
    }

    IBaseResource execute(
            Repository repository,
            String name,
            Class<? extends IBaseResource> resourceType,
            IIdType id,
            IBaseParameters parameters) {
        requireNonNull(repository, "Repository cannot be null");
        requireNonNull(name, "Operation name cannot be null");

        var scope = id != null ? Scope.INSTANCE : (resourceType != null ? Scope.TYPE : Scope.SERVER);
        var typeName = resourceType != null ? resourceType.getSimpleName() : null;
        var closure = findOperation(scope, name, typeName);

        var instance = closure.factory().apply(repository);
        var callable = closure.methodBinder().bind(instance, id, parameters);

        try {
            return callable.call();
        } catch (Exception e) {
            throw new RuntimeException("Error invoking operation " + name, e);
        }
    }

    private OperationClosure findOperation(Scope scope, String name, String typeName) {
        requireNonNull(scope, "Scope cannot be null");
        requireNonNull(name, "Operation name cannot be null");

        var closures = operationMap.get(name);
        if (closures.isEmpty()) {
            throw new IllegalArgumentException("No operation found with name " + name);
        }

        var scopedClosures =
                closures.stream().filter(c -> c.methodBinder().scope() == scope).collect(Collectors.toList());

        if (scopedClosures.isEmpty()) {
            throw new IllegalArgumentException("No operation found with name " + name + " and scope " + scope);
        }

        Predicate<OperationClosure> typePredicate =
                typeName != null ? c -> c.methodBinder().typeName().equals(typeName) : c -> true;
        var typedClosures = scopedClosures.stream().filter(typePredicate).collect(Collectors.toList());

        if (typedClosures.isEmpty()) {
            throw new IllegalArgumentException("No operation found with type " + typeName);
        }

        if (typedClosures.size() > 1) {
            throw new IllegalArgumentException("Multiple operations found with name " + name + " and type " + typeName);
        }

        return typedClosures.get(0);
    }
}
