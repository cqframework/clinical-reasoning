package org.opencds.cqf.fhir.utility.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.repository.IRepository;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.StringType;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.utility.operation.ExtraParams;

public class InMemoryRepositoryOperationsTest {

    public static final class Example {
        @Operation(name = "example")
        public IBaseParameters example(@OperationParam(name = "stringParam") IPrimitiveType<String> param) {
            return new Parameters().addParameter("result", new StringType(param.getValue()));
        }
    }

    @Test
    public void simpleExample() {
        var repo = new InMemoryFhirRepository(FhirContext.forR4Cached());
        repo.registerOperation(Example.class, r -> new Example());

        var result = repo.invoke(
                "example", new Parameters().addParameter("stringParam", new StringType("test")), Parameters.class);

        var p = assertInstanceOf(Parameters.class, result);
        var value = assertInstanceOf(StringType.class, p.getParameter("result").getValue())
                .getValue();
        assertEquals("test", value);
    }

    public static class Recursive {
        private IRepository repository;
        private String configParam;

        Recursive(IRepository repository, String configParam) {
            this.repository = repository;
            this.configParam = configParam;
        }

        @Operation(name = "example")
        public IBaseParameters example(
                @OperationParam(name = "stringParam") StringType param,
                @ExtraParams IBaseParameters everythingElseNotMapped) {
            return new Parameters()
                    .addParameter("result", new IntegerType(5))
                    .addParameter("config", new StringType(configParam));
        }

        @Operation(name = "recursive")
        public IBaseParameters recursive() {
            return this.repository.invoke("example", null, Parameters.class);
        }
    }

    @Test
    public void operationNotFound() {
        var repo = new InMemoryFhirRepository(FhirContext.forR4Cached());
        repo.registerOperation(Example.class, r -> new Example());

        var exception = assertThrows(
                InvalidRequestException.class, () -> repo.invoke("notFound", new Parameters(), Parameters.class));
        assertTrue(exception.getMessage().contains("No operation found"));
    }

    @Test
    public void recursiveExampleWithConfig() {
        var repo = new InMemoryFhirRepository(FhirContext.forR4Cached());
        repo.registerOperation(Recursive.class, r -> new Recursive(r, "test config"));
        var result = repo.invoke("example", null, Parameters.class);

        var p = assertInstanceOf(Parameters.class, result);
        var num = assertInstanceOf(IntegerType.class, p.getParameter("result").getValue())
                .getValue();
        assertEquals(5, num);
        var config = assertInstanceOf(StringType.class, p.getParameter("config").getValue())
                .getValue();
        assertEquals("test config", config);

        // Same as above, but using a reentrant operation
        result = repo.invoke("recursive", null, Parameters.class);
        p = assertInstanceOf(Parameters.class, result);
        num = assertInstanceOf(IntegerType.class, p.getParameter("result").getValue())
                .getValue();
        assertEquals(5, num);
        config = assertInstanceOf(StringType.class, p.getParameter("config").getValue())
                .getValue();
        assertEquals("test config", config);
    }

    public static class ServerScopeOperations {

        @Operation(name = "server")
        public IBaseParameters server() {
            return new Parameters().addParameter("result", new StringType("server success"));
        }

        @Operation(name = "serverNonFhirException")
        public IBaseParameters serverNonFhirException() {
            throw new RuntimeException("server error");
        }

        @Operation(name = "serverFhirException")
        public IBaseParameters serverFhirException() {
            throw new ResourceNotFoundException("server error");
        }
    }

    @Test
    public void serverScopeOperations() {
        var repo = new InMemoryFhirRepository(FhirContext.forR4Cached());
        repo.registerOperation(ServerScopeOperations.class, r -> new ServerScopeOperations());

        var result = repo.invoke("server", null, Parameters.class);
        assertNotNull(result);
        assertEquals(
                "server success", ((StringType) result.getParameter("result").getValue()).getValue());

        // Test non-FHIR exception, should be caught and rethrown as a FHIR exception
        var internalError = assertThrows(
                InternalErrorException.class, () -> repo.invoke("serverNonFhirException", null, Parameters.class));
        assertTrue(internalError.getCause().getMessage().contains("server error"));

        // Test FHIR exception, should be rethrown as-is
        var notFound = assertThrows(
                ResourceNotFoundException.class, () -> repo.invoke("serverFhirException", null, Parameters.class));
        assertTrue(notFound.getMessage().contains("server error"));

        // Try to invoke as instance scope, should throw an exception
        var invalidRequest = assertThrows(
                InvalidRequestException.class,
                () -> repo.invoke(new IdType("Library/123"), "server", null, Parameters.class));
        assertTrue(invalidRequest.getMessage().contains("No operation found"));
    }

    public static class TypeScopeOperations {

        @Operation(name = "type", type = Library.class)
        public IBaseParameters type() {
            return new Parameters().addParameter("result", new StringType("server success"));
        }

        @Operation(name = "typeNonFhirException", type = Library.class)
        public IBaseParameters typeNonFhirException() {
            throw new RuntimeException("server error");
        }

        @Operation(name = "typeFhirException", type = Library.class)
        public IBaseParameters typeFhirException() {
            throw new ResourceNotFoundException("server error");
        }
    }

    @Test
    public void typeScopeOperations() {
        var repo = new InMemoryFhirRepository(FhirContext.forR4Cached());
        repo.registerOperation(TypeScopeOperations.class, r -> new TypeScopeOperations());

        var result = repo.invoke(Library.class, "type", null, Parameters.class);
        assertNotNull(result);
        assertEquals(
                "server success", ((StringType) result.getParameter("result").getValue()).getValue());

        // Test non-FHIR exception, should be caught and rethrown as a FHIR exception
        var internalError = assertThrows(
                InternalErrorException.class,
                () -> repo.invoke(Library.class, "typeNonFhirException", null, Parameters.class));
        assertTrue(internalError.getCause().getMessage().contains("server error"));

        // Test FHIR exception, should be rethrown as-is
        var notFound = assertThrows(
                ResourceNotFoundException.class,
                () -> repo.invoke(Library.class, "typeFhirException", null, Parameters.class));
        assertTrue(notFound.getMessage().contains("server error"));

        // Try to invoke as instance scope, should throw an exception
        var invalidRequest = assertThrows(
                InvalidRequestException.class,
                () -> repo.invoke(new IdType("Library/123"), "type", null, Parameters.class));
        assertTrue(invalidRequest.getMessage().contains("No operation found"));
    }

    public static class InstanceScopeOperations {

        @Operation(name = "instance", type = Library.class)
        public IBaseParameters instance(@IdParam IdType id) {
            return new Parameters().addParameter("result", new StringType(id.getIdPart()));
        }

        @Operation(name = "instanceNonFhirException", type = Library.class)
        public IBaseParameters instanceNonFhirException(@IdParam IdType id) {
            throw new RuntimeException("server error");
        }

        @Operation(name = "instanceFhirException", type = Library.class)
        public IBaseParameters instanceFhirException(@IdParam IdType id) {
            throw new ResourceNotFoundException("server error");
        }
    }

    @Test
    public void instanceScopeOperations() {
        var repo = new InMemoryFhirRepository(FhirContext.forR4Cached());
        repo.registerOperation(InstanceScopeOperations.class, r -> new InstanceScopeOperations());

        var id = new IdType("Library/123");

        var result = repo.invoke(id, "instance", null, Parameters.class);
        assertNotNull(result);
        assertEquals("123", ((StringType) result.getParameter("result").getValue()).getValue());

        // Test non-FHIR exception, should be caught and rethrown as a FHIR exception
        var internalError = assertThrows(
                InternalErrorException.class,
                () -> repo.invoke(id, "instanceNonFhirException", null, Parameters.class));
        assertTrue(internalError.getCause().getMessage().contains("server error"));

        // Test FHIR exception, should be rethrown as-is
        var notFound = assertThrows(
                ResourceNotFoundException.class,
                () -> repo.invoke(id, "instanceFhirException", null, Parameters.class));
        assertTrue(notFound.getMessage().contains("server error"));

        // Try to invoke as server scope, should throw an exception
        var invalidRequest =
                assertThrows(InvalidRequestException.class, () -> repo.invoke("instance", null, Parameters.class));
        assertTrue(invalidRequest.getMessage().contains("No operation found"));
    }
}
