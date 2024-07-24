package org.opencds.cqf.fhir.utility.operation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import java.nio.file.Path;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.StringType;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.repository.ig.EncodingBehavior;
import org.opencds.cqf.fhir.utility.repository.ig.IgConventions;
import org.opencds.cqf.fhir.utility.repository.ig.IgRepository;

class OperationRegistryTest {

    static class Example {
        private Repository repository;
        private String configParam;

        Example(Repository repository, String configParam) {
            this.repository = repository;
            this.configParam = configParam;
        }

        @Operation(name = "example")
        public IBaseParameters example(
                @OperationParam(name = "stringParam") String param,
                @UnboundParam IBaseParameters everythingElseNotBound) {
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
    void registerOperation() {

        // test directory setup for the IG repo
        var root = Path.of("/does-not-exist");

        // If you have some config above and beyond just a repo, you pass it as part of the
        // factory function
        var operationRegistry = new OperationRegistry();
        operationRegistry.register(Example.class, r -> new Example(r, "test config"));

        // Internally, the operation registry passes an instance of the repository to the operation
        // factory and constructs the operation provider on the fly.
        var repository = new IgRepository(FhirContext.forR4Cached(), root);
        var result = operationRegistry.buildOperation(repository, "example").execute();
        var p = assertInstanceOf(Parameters.class, result);
        var num = assertInstanceOf(IntegerType.class, p.getParameter("result").getValue())
                .getValue();
        assertEquals(5, num);
        var config = assertInstanceOf(StringType.class, p.getParameter("config").getValue())
                .getValue();
        assertEquals("test config", config);

        // Externally, the IG repo passes itself to the operation registry, allowing
        // calls back into the repository
        var igRepo = new IgRepository(
                FhirContext.forR4Cached(),
                root,
                IgConventions.autoDetect(root),
                EncodingBehavior.DEFAULT,
                operationRegistry);
        result = igRepo.invoke("example", null, Parameters.class);

        p = assertInstanceOf(Parameters.class, result);
        num = assertInstanceOf(IntegerType.class, p.getParameter("result").getValue())
                .getValue();
        assertEquals(5, num);
        config = assertInstanceOf(StringType.class, p.getParameter("config").getValue())
                .getValue();
        assertEquals("test config", config);

        // Same as above, but using a reentrant operation
        result = igRepo.invoke("recursive", null, Parameters.class);
        p = assertInstanceOf(Parameters.class, result);
        num = assertInstanceOf(IntegerType.class, p.getParameter("result").getValue())
                .getValue();
        assertEquals(5, num);
        config = assertInstanceOf(StringType.class, p.getParameter("config").getValue())
                .getValue();
        assertEquals("test config", config);
    }
}
