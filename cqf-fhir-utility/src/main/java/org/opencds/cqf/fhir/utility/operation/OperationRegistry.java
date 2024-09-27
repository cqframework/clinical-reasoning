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

    public class OperationInvocationParams {

        private final Repository repository;
        private final String name;
        private IBaseParameters parameters;
        private IIdType id;
        private Class<? extends IBaseResource> resourceType;

        OperationInvocationParams(Repository repository, String name) {
            this.repository = requireNonNull(repository, "Repository cannot be null");
            this.name = requireNonNull(name, "Operation name cannot be null");
        }

        public Repository repository() {
            return repository;
        }

        public OperationInvocationParams id(IIdType id) {
            this.id = id;
            return this;
        }

        public IIdType id() {
            return id;
        }

        public String name() {
            return name;
        }

        public IBaseParameters parameters() {
            return parameters;
        }

        public OperationInvocationParams parameters(IBaseParameters parameters) {
            this.parameters = parameters;
            return this;
        }

        public OperationInvocationParams resourceType(Class<? extends IBaseResource> resourceType) {
            this.resourceType = resourceType;
            return this;
        }

        Scope scope() {
            if (id != null) {
                return Scope.INSTANCE;
            } else if (resourceType != null) {
                return Scope.TYPE;
            } else {
                return Scope.SERVER;
            }
        }

        String typeName() {
            if (resourceType != null) {
                return resourceType.getSimpleName();
            } else if (id != null) {
                return id.getResourceType();
            } else {
                return "";
            }
        }

        public IBaseResource execute() throws Exception {
            return OperationRegistry.this.execute(this);
        }
    }

    private final Multimap<String, InvocationContext<?>> invocationContextByName;

    /**
     * Creates a new OperationRegistry instance. This instance will be used to register and execute operations for
     * a Repository.
     */
    public OperationRegistry() {
        this.invocationContextByName =
                MultimapBuilder.hashKeys().arrayListValues().build();
    }

    /**
     * Used to register a new Operation provider. The class must have methods annotated with @Operation.
     * @param <T> The type of the class to register
     * @param clazz The class to register
     * @param factory A factory function that will create an instance of the class
     */
    public <T> void register(Class<T> clazz, Function<Repository, T> factory) {
        var methodBinders = Arrays.stream(clazz.getMethods())
                .filter(m -> m.isAnnotationPresent(Operation.class))
                .map(MethodBinder::new)
                .collect(Collectors.toList());

        if (methodBinders.isEmpty()) {
            throw new IllegalArgumentException("No operations found on class " + clazz.getName());
        }

        for (var methodBinder : methodBinders) {
            var context = new InvocationContext<T>(factory, methodBinder);
            invocationContextByName.put(methodBinder.name(), context);
        }
    }

    /**
     * Used to build an OperationInvocationParams object that can be used to execute an operation.
     * @param repository the repository to use for data access and recursive invocations
     * @param operationName the name of the operation to execute
     * @return an OperationInvocationParams object that can be used to execute the operation
     */
    public OperationInvocationParams buildContext(Repository repository, String operationName) {
        return new OperationInvocationParams(repository, operationName);
    }

    IBaseResource execute(OperationInvocationParams params) throws Exception {
        requireNonNull(params, "Operation invocation parameters cannot be null");
        var context = findInvocationContext(params.scope(), params.name(), params.typeName());

        var instance = context.factory().apply(params.repository);
        var callable = context.methodBinder().bind(instance, params.id(), params.parameters());

        return callable.call();
    }

    private InvocationContext<?> findInvocationContext(Scope scope, String name, String typeName) {
        requireNonNull(scope, "scope cannot be null");
        requireNonNull(name, "operation name cannot be null");
        requireNonNull(typeName, "typeName cannot be null");

        var contexts = invocationContextByName.get(name);
        if (contexts.isEmpty()) {
            throw new IllegalArgumentException("No operation found with name " + name);
        }

        var scopedContexts =
                contexts.stream().filter(c -> c.methodBinder().scope() == scope).collect(Collectors.toList());

        if (scopedContexts.isEmpty()) {
            throw new IllegalArgumentException("No operation found with name " + name + " and scope " + scope);
        }

        // Only filter by type if the typeName is not empty
        Predicate<InvocationContext<?>> typePredicate = typeName.isEmpty()
                ? c -> true
                : c -> c.methodBinder().typeName().equals(typeName);
        var typeContexts = scopedContexts.stream().filter(typePredicate).collect(Collectors.toList());

        if (typeContexts.isEmpty()) {
            throw new IllegalArgumentException("No operation found with type " + typeName);
        }

        if (typeContexts.size() > 1) {
            throw new IllegalArgumentException("Multiple operations found with name " + name + " and type " + typeName);
        }

        return typeContexts.get(0);
    }
}
