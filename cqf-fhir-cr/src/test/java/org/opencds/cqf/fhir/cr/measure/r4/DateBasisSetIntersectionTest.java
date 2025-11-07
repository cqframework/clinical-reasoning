package org.opencds.cqf.fhir.cr.measure.r4;

import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.Given;

@SuppressWarnings({"squid:S125", "java:S2699"})
class DateBasisSetIntersectionTest {
    private static final Given GIVEN = Measure.given().repositoryFor("DateBasisSetIntersection");

    @Test
    void dateBasisSetIntersectionWithIntersectionsScenario1() {
        GIVEN.when()
                .measureId("DateBasisSetIntersectionScenario1")
                .evaluate()
                .then()
                .hasGroupCount(1)
                .firstGroup()
                .hasPopulationCount(3)
                .population("initial-population")
                // This works because we have { @2024-01-01, @2024-01-02, @2024-01-03 }
                .hasCount(6)
                .up()
                .population("denominator")
                // This should be 2 because the intersection of
                // { @2024-01-01, @2024-01-02, @2024-01-03 }
                // and
                // { @2024-01-01, @2024-01-02 }
                // which is:
                // { @2024-01-01, @2024-01-02 }
                .hasCount(4)
                .up()
                .population("numerator")
                // This should be 1 because the intersection of
                // { @2024-01-01, @2024-01-02 }
                // and
                // { @2024-01-01, @2024-04-01 }
                // which is:
                // { @2024-01-01 }
                .hasCount(2)
                .up()
                .up()
                .report();
    }
}
