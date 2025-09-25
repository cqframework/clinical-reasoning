package org.opencds.cqf.fhir.cr.hapi.r4;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class RiskAdjustmentOperationProviderIT extends BaseCrR4TestServer {

    @Test
    void testRiskAdjustmentOperationInvalidRequest() {
        try {
            ourClient
                    .operation()
                    .onType("Measure")
                    .named("ra-submit-data")
                    .withNoParameters(Parameters.class)
                    .execute();
            Assertions.fail();
        } catch (InvalidRequestException ire) {
            // Passes
        }
    }

    @Test
    void testRiskAdjustmentOperationInvalidProfile() {
        var mr = (MeasureReport) readResource("ra-datax-measurereport01.json");
        mr.getMeta().setProfile(null);
        var params = org.opencds.cqf.fhir.utility.r4.Parameters.parameters(
                org.opencds.cqf.fhir.utility.r4.Parameters.part("measureReport", mr));
        try {
            ourClient
                    .operation()
                    .onType("Measure")
                    .named("ra-submit-data")
                    .withParameters(params)
                    .execute();
            Assertions.fail();
        } catch (InvalidRequestException ire) {
            // Passes
        }
    }

    @Test
    void testRiskAdjustmentOperationValidRequest() {
        var mr = readResource("ra-datax-measurereport01.json");
        var params = org.opencds.cqf.fhir.utility.r4.Parameters.parameters(
                org.opencds.cqf.fhir.utility.r4.Parameters.part("measureReport", (Resource) mr));
        var result = ourClient
                .operation()
                .onType("Measure")
                .named("ra-submit-data")
                .withParameters(params)
                .execute();
        Assertions.assertNotNull(result);
    }
}
