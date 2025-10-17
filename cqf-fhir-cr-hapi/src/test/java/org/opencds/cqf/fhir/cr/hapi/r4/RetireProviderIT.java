package org.opencds.cqf.fhir.cr.hapi.r4;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Parameters;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
public class RetireProviderIT extends BaseCrR4TestServer {

    public void callRetire(String id) {
        ourClient
                .operation()
                .onInstance(id)
                .named("$retire")
                .withNoParameters(Parameters.class)
                .returnResourceType(Bundle.class)
                .execute();
    }

    @Test
    void retireOperation_test() {
        loadBundle("ersd-active-transaction-bundle-example.json");
        callRetire("Library/SpecificationLibrary");
        Library retiredLibrary = ourClient
                .read()
                .resource(Library.class)
                .withId("SpecificationLibrary")
                .execute();
        Assertions.assertEquals(retiredLibrary.getStatus().name(), Enumerations.PublicationStatus.RETIRED.name());
    }
}
