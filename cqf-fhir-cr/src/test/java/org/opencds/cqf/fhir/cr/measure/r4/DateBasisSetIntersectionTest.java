package org.opencds.cqf.fhir.cr.measure.r4;

import ca.uhn.fhir.context.FhirContext;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.Given;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.SelectedReport;

class DateBasisSetIntersectionTest {
    private static final Given GIVEN = Measure.given().repositoryFor("DateBasisSetIntersection");

    @Test
    void dateBasisSetIntersectionWithIntersectionsScenario1() {
        final SelectedReport then = GIVEN.when()
                .measureId("DateBasisSetIntersectionScenario1")
                .evaluate()
                .then();

        System.out.println(
                FhirContext.forR4Cached().newJsonParser().setPrettyPrint(true).encodeResourceToString(then.report()));

        then.hasGroupCount(1)
                .firstGroup()
                .hasPopulationCount(3)
                .population("initial-population")
                // This works because we have { @2024-02-01, @2024-01-02 }
                .hasCount(2)
                .up()
                .population("denominator")
                // This should be 1 because the intersection of { @2024-02-01, @2024-01-02 } and { @2024-02-01,
                // @2024-01-02 } is { @2024-01-02 }
                .hasCount(1)
                .up()
                // This should be 1 because the intersection of { @2024-02-01, @2024-01-02 } and { @2024-02-03,
                // @2024-01-02 } is { @2024-01-02 }
                .population("numerator")
                .hasCount(1)
                .up()
                .up()
                .report();
    }
}
