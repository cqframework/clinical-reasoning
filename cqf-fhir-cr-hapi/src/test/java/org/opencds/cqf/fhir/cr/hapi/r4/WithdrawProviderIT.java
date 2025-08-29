package org.opencds.cqf.fhir.cr.hapi.r4;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Parameters;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
public class WithdrawProviderIT extends BaseCrR4TestServer {

    public Bundle callWithdraw(String id) {
        return ourClient
            .operation()
            .onInstance(id)
            .named("$withdraw")
            .withNoParameters(Parameters.class)
            .returnResourceType(Bundle.class)
            .execute();
    }

    @Test
    public void testWithdraw() {
        loadBundle("ersd-small-approved-draft-bundle.json");
        var result = callWithdraw("Library/SpecificationLibrary");
        Assertions.assertNotNull(result);
    }

}
