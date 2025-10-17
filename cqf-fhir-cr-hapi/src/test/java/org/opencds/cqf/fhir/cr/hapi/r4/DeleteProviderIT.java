package org.opencds.cqf.fhir.cr.hapi.r4;

import ca.uhn.fhir.rest.server.exceptions.ResourceGoneException;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Parameters;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
public class DeleteProviderIT extends BaseCrR4TestServer {

    public void callDelete(String id) {
        ourClient
                .operation()
                .onInstance(id)
                .named("$delete")
                .withNoParameters(Parameters.class)
                .returnResourceType(Bundle.class)
                .execute();
    }

    @Test
    void deleteOperation_test() {
        loadBundle("ersd-small-retired-bundle.json");
        callDelete("Library/SpecificationLibrary");

        Assertions.assertThrows(ResourceGoneException.class, () -> {
            ourClient
                    .read()
                    .resource(Library.class)
                    .withId("SpecificationLibrary")
                    .execute();
        });
    }
}
