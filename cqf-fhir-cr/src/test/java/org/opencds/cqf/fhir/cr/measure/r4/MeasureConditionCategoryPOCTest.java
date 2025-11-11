package org.opencds.cqf.fhir.cr.measure.r4;

import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.measure.constant.MeasureConstants;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.Given;

@SuppressWarnings("squid:S2699")
public class MeasureConditionCategoryPOCTest {
    protected static Given given = Measure.given().repositoryFor("ConditionCategoryPoc");

    /**
     * This is a POC Measure that creates a resource in CQL based on patient chart data
     * This test only validates that an SDE can create an inline resource based on patient data.
     * The example here captures a contained Encounter that suspects an issue.
     */
    @Test
    void measure_eval_non_retrieve_resource() {
        given.when()
                .measureId("ConditionCategoryPOC")
                .periodStart("2022-01-01")
                .periodEnd("2022-12-31")
                .subject("Patient/hist-closed-HCC189")
                .reportType("subject")
                .evaluate()
                .then()
                .hasContainedResource(x -> x.getId().startsWith("hist-closed-HCC189-suspecting-algorithm-encounter"))
                .hasExtension(MeasureConstants.EXT_SDE_REFERENCE_URL, 8);
    }
}
