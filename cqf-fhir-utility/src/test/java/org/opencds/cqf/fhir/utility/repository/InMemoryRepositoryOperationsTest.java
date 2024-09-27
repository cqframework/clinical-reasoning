package org.opencds.cqf.fhir.utility.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.StringType;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.api.Repository;
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
        private Repository repository;
        private String configParam;

        Recursive(Repository repository, String configParam) {
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
}
