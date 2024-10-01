package org.opencds.cqf.fhir.cr.measure.r4;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Extension;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.measure.constant.MeasureConstants;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.Given;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;

class MeasureProcessorSdeAddCriteriaExtensionTest {

    protected static Given given = Measure.given().repositoryFor("InitialInpatientPopulation");

    @Test
    void exm124_subject_list() {
        var report = given.when()
                .measureId("InitialInpatientPopulation")
                .periodStart(LocalDate.of(2019, Month.JANUARY, 1).atStartOfDay(ZoneId.systemDefault()))
                .periodEnd(LocalDate.of(2020, Month.JANUARY, 1).atStartOfDay(ZoneId.systemDefault()))
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
