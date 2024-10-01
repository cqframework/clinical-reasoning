package org.opencds.cqf.fhir.utility.operation;

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.hl7.fhir.dstu2.model.IdType;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.StringType;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.operation.OperationRegistry.OperationInvocationParams;
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository;

class OperationRegistryTest {

    // Dummy repository for testing... Doesn't actually do anything in these tests
    private static final Repository repo = new InMemoryFhirRepository(FhirContext.forR4Cached());

    private static <T> OperationRegistry constructAndRegister(Class<T> clazz, Function<Repository, T> factory) {
        var registry = new OperationRegistry();
        registry.register(clazz, factory);
        return registry;
    }

    // Convenience method for when the operation provider is a no-arg constructor
    // See test below for example usage
    private static <T> OperationRegistry constructAndRegister(Class<T> clazz) {
        return constructAndRegister(clazz, r -> {
            try {
                return clazz.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    void noAnnotatedMethod_throws() {
        final class MissingAnnotationExample {
            @SuppressWarnings("unused")
            public void noAnnotation() {}
        }

        var operationRegistry = new OperationRegistry();
        assertThrows(
                IllegalArgumentException.class,
                () -> operationRegistry.register(MissingAnnotationExample.class, r -> new MissingAnnotationExample()));
    }

    @Test
    void noMatchingOperationByName_throws() {
        var registry = new OperationRegistry();
        // TODO: Need to think about this. This is a suboptimal API experience because
        // we don't actually check for the operation name when we start
        // the build process. We only check for it when we try to execute it.
        // Whether an operation invocation is scoped by instance, type, or server
        // depends on the parameters passed to the operation invocation. We don't
        // have all those parameters until we try to execute the operation.
        var ctx = registry.buildContext(repo, "nonExistentOperation");
        var e = assertThrows(IllegalArgumentException.class, () -> ctx.execute());
        assertTrue(e.getMessage().contains("name"));
    }

    @Test
    void mismatchedScope_throws() {

        // Server scoped operation, down below we'll try to execute it as an instance scoped operation
        final class ServerScopedExample {
            @Operation(name = "serverScoped")
            public void serverScoped() {}
        }

        var ctx = constructAndRegister(ServerScopedExample.class).buildContext(repo, "serverScoped");
        ctx.id(new IdType("Library/123"));
        var e = assertThrows(IllegalArgumentException.class, () -> ctx.execute());
        assertTrue(e.getMessage().toLowerCase().contains("scope"));
    }

    @Test
    void mismatchedType_throws() {
        // Type scoped operation
        final class TypedScopeExample {
            @Operation(name = "typeScoped", typeName = "Measure")
            public void typeScoped() {}
        }

        var ctx = constructAndRegister(TypedScopeExample.class).buildContext(repo, "typeScoped");
        ctx.resourceType(Library.class);
        var e = assertThrows(IllegalArgumentException.class, () -> ctx.execute());
        assertTrue(e.getMessage().toLowerCase().contains("type"));
    }

    @Test
    void ambiguousOperations_throws() {
        final class AmbiguousExample {
            @Operation(name = "ambiguous", typeName = "Library")
            public void ambiguous1() {}

            @Operation(name = "ambiguous", typeName = "Library")
            public void ambiguous2() {}
        }

        var ctx = constructAndRegister(AmbiguousExample.class).buildContext(repo, "ambiguous");
        ctx.resourceType(Library.class);
        var e = assertThrows(IllegalArgumentException.class, () -> ctx.execute());
        assertTrue(e.getMessage().toLowerCase().contains("multiple"));
    }

    @Test
    void parametersWithResource() {
        final class GetIdFromResource {

            @Operation(name = "get-id")
            public IBaseParameters getId(@OperationParam(name = "resource") IBaseResource resource) {
                return new Parameters()
                        .addParameter(
                                "result", new StringType(resource.getIdElement().getIdPart()));
            }
        }

        var registry = constructAndRegister(GetIdFromResource.class, r -> new GetIdFromResource());

        var arguments = new Parameters();
        arguments.addParameter().setName("resource").setResource(new Library().setId("123"));

        var op = registry.buildContext(repo, "get-id").parameters(arguments);
        var result = assertDoesNotThrow(() -> op.execute());

        var p = assertInstanceOf(Parameters.class, result);
        var id = assertInstanceOf(StringType.class, p.getParameter("result").getValue())
                .getValue();
        assertEquals("123", id);

        // Ensure that multiple parameters of the same name are not allowed
        arguments = new Parameters();
        arguments.addParameter().setName("resource").setResource(new Library().setId("123"));
        arguments.addParameter().setName("resource").setResource(new Library().setId("456"));

        var op2 = registry.buildContext(repo, "get-id").parameters(arguments);

        var e = assertThrows(IllegalArgumentException.class, () -> op2.execute());
        assertTrue(e.getMessage().contains("parts"));

        // Parameters with wrong type should fail
        arguments = new Parameters();
        arguments.addParameter().setName("resource").setValue(new StringType("123"));

        var op3 = registry.buildContext(repo, "get-id").parameters(arguments);
        e = assertThrows(IllegalArgumentException.class, () -> op3.execute());
        assertTrue(e.getMessage().contains("type"));
    }

    @Test
    void parametersWithList() {
        final class GetIdFromResources {

            @Operation(name = "get-id")
            public IBaseParameters getId(@OperationParam(name = "resources") List<IBaseResource> resources) {
                List<String> ids = resources.stream()
                        .map(r -> r.getIdElement().getIdPart())
                        .collect(Collectors.toList());

                var params = new Parameters();
                for (var id : ids) {
                    params.addParameter("result", new StringType(id));
                }

                return params;
            }
        }

        var arguments = new Parameters();
        var resourceParam = arguments.addParameter().setName("resources");
        resourceParam.addPart().setResource(new Library().setId("123"));
        resourceParam.addPart().setResource(new Measure().setId("456"));

        OperationInvocationParams op = constructAndRegister(GetIdFromResources.class, r -> new GetIdFromResources())
                .buildContext(repo, "get-id")
                .parameters(arguments);
        var result = assertDoesNotThrow(() -> op.execute());

        var p = assertInstanceOf(Parameters.class, result);
        var results = p.getParameters("result");
        assertEquals(2, results.size());
        assertEquals(
                "123",
                assertInstanceOf(StringType.class, results.get(0).getValue()).getValue());
        assertEquals(
                "456",
                assertInstanceOf(StringType.class, results.get(1).getValue()).getValue());
    }
}
