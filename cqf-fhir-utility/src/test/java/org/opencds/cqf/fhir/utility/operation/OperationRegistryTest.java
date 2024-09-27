package org.opencds.cqf.fhir.utility.operation;

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import java.util.function.Function;
import org.hl7.fhir.dstu2.model.IdType;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.StringType;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.api.Repository;
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
    void registerOperation() {
        final class Example {
            private String configParam;

            Example(String configParam) {
                this.configParam = configParam;
            }

            @Operation(name = "example")
            public IBaseParameters example(
                    @OperationParam(name = "stringParam") IPrimitiveType<String> param,
                    @ExtraParams IBaseParameters everythingElseNotBound) {
                return new Parameters()
                        .addParameter("result", new IntegerType(5))
                        .addParameter("config", new StringType(configParam));
            }
        }

        var o = constructAndRegister(Example.class, r -> new Example("test config"))
                .buildContext(repo, "example");
        var result = assertDoesNotThrow(() -> o.execute());

        var p = assertInstanceOf(Parameters.class, result);
        var num = assertInstanceOf(IntegerType.class, p.getParameter("result").getValue())
                .getValue();
        assertEquals(5, num);
        var config = assertInstanceOf(StringType.class, p.getParameter("config").getValue())
                .getValue();
        assertEquals("test config", config);
    }
}
