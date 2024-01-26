package org.opencds.cqf.fhir.cr.measure.r4;

import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.Given;

public class MultipleRateMeasureProcessorTest {

    protected static Given given = Measure.given().repositoryFor("FHIR347");

    @Test
    public void fhir347_singlePatient() {
        given.when()
                .measureId("FHIR347")
                .periodStart("2019-01-01")
                .periodEnd("2020-01-01")
                .subject("Patient/numer1-EXM347")
                .reportType("subject")
                .evaluate()
                .then()
                .firstGroup()
                .population("initial-population")
                .hasCount(1);
    }
}
