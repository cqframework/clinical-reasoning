package org.opencds.cqf.fhir.utility.operation;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.opencds.cqf.fhir.api.Repository;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;

import ca.uhn.fhir.rest.annotation.Operation;

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
            var closure = new OperationClosure((Function<Repository, Object>)factory, methodBinder);
            operationMap.put(methodBinder.name(), closure);
        }
    }

    IBaseResource execute(Repository repository, String name, Class<? extends IBaseResource> resourceType, IIdType id, IBaseParameters parameters) throws Exception {
        Objects.requireNonNull(repository, "Repository cannot be null");
        Objects.requireNonNull(name, "Operation name cannot be null");
        if (id != null && resourceType == null) {
            throw new IllegalArgumentException("Resource type must be provided when an id is provided");
        }

        var scope = id != null ? Scope.INSTANCE : resourceType != null ? Scope.TYPE : Scope.SERVER;

        var closures = operationMap.get(name);
        if (closures.isEmpty()) {
            throw new IllegalArgumentException("No operation found with name " + name);
        }

        Predicate<OperationClosure> scopePredicate = c -> c.scope() == scope;
        var scopedClosures = closures.stream()
            .filter(scopePredicate)
            .collect(Collectors.toList());
        
        if (scopedClosures.isEmpty()) {
            throw new IllegalArgumentException("No operation found with name " + name + " and scope " + scope);
        }

        Predicate<OperationClosure> typePredicate = resourceType != null ? c -> c.typeName().equals(resourceType.getSimpleName()) : c -> true;
        var typedClosures = scopedClosures.stream()
            .filter(typePredicate)
            .collect(Collectors.toList());

        if (typedClosures.isEmpty()) {
            throw new IllegalArgumentException("No operation found with type " + resourceType.getSimpleName());
        }

        if (typedClosures.size() > 1) {
            throw new IllegalArgumentException("Multiple operations found with name " + name + " and type " + resourceType.getSimpleName());
        }

        var closure = typedClosures.get(0);

        return closure.execute(repository, id, parameters);
    }
}
