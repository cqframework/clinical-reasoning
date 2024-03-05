package org.opencds.cqf.fhir.utility.operation;

import org.hl7.fhir.dstu2.model.IdType;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.repository.ig.IgRepository;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;

class OperationRegistryTest {

    static class Example {
        private Repository repository;
        private String someOtherConfigParam;

        Example(Repository repository, String someOtherConfigParam) {
            this.repository = repository;
        }

        @Operation(name = "example")
        public void example(@IdParam IdType id, @OperationParam(name = "stringParam") String param, @UnboundParam IBaseParameters everythingElseNotBound) {
            System.out.println("the value of some other config, like evaluation settings is : " + someOtherConfigParam);
        }
    }

    @Test
    void registerOperation() {
        // If you have some config above and beyond just a repo, you pass it as part of the
        // factory function
        var operationRegistry = new OperationRegistry();
        operationRegistry.register(Example.class, r -> new Example(r, "test config"));

        // Internally, the operation registry passes an instance of the repository to the operation
        // factory and constructs the operation provider on the fly.
        var repository = new Repository();
        operationRegistry.execute(repository, "example", null, null, null);


        // Externally, the IG repo passes itself to the operation registry, allowing
        // reentrant/recursive operations to be called
        var igRepo = new IgRepository(FhirContext.forR4Cached(), "testDirectory", operationRegistry);
        igRepo.invoke("example", null);
    }
}