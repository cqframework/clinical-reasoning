package org.opencds.cqf.fhir.cr.measure.r4;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Extension;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.measure.common.MeasureConstants;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.Given;

class MeasureProcessorSdeAddCriteriaExtensionTest {

    protected static Given given = Measure.given().repositoryFor("InitialInpatientPopulation");

    @Test
    void exm124_subject_list() {
        var report = given.when()
                .measureId("InitialInpatientPopulation")
                .periodStart("2019-01-01")
                .periodEnd("2020-01-01")
                .subject("Patient/97f27374-8a5c-4aa1-a26f-5a1ab03caa47")
                .reportType("subject")
                .evaluate()
                .then()
                .firstGroup()
                .population("initial-population")
                .hasCount(2)
                .up()
                .up()
                .report();

        // TODO: What's this actually suppoed to test? That we have SDEs?
        for (Extension extension : report.getExtension()) {
            if (StringUtils.equalsIgnoreCase(extension.getUrl(), MeasureConstants.EXT_SDE_URL)) {
                assertNotNull(extension.getValue());
                break;
            }
        }
    }
}
