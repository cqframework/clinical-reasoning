package org.opencds.cqf.fhir.cr.measure.r4;

import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.Given;

class R4LibraryNameCqlMismatchTest {
    private static final Given GIVEN_REPO = Measure.given().repositoryFor("LibraryUrlNameCqlMismatch");

    @Test
    void sanityCheck() {

        GIVEN_REPO
                .when()
                .measureId("SampleReporting-2024.2.0")
                .evaluate()
                .then()
                .group("group-1")
                .population("initial-population")
                .hasCount(1);
    }
}
