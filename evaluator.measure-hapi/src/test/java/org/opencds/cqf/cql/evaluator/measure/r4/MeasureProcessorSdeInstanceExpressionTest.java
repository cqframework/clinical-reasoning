package org.opencds.cqf.cql.evaluator.measure.r4;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.hl7.fhir.r4.model.MeasureReport;
import org.testng.annotations.Test;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertThrows;

public class MeasureProcessorSdeInstanceExpressionTest extends BaseMeasureProcessorTest {
    public MeasureProcessorSdeInstanceExpressionTest() {
        super("ConditionCategoryPoc.json");
    }

    @Test
    public void measure_eval_non_retrieve_resource() {

        assertThrows(ResourceNotFoundException.class, () -> {
            this.measureProcessor.evaluateMeasure("https://build.fhir.org/ig/HL7/davinci-ra/ConditionCategoryPOC",
                    "2022-01-01", "2022-12-31", "subject",
                    "Patient/hist-open-HCC189", null, null,
                    endpoint, endpoint, endpoint, null);
        });

        MeasureReport report = this.measureProcessor.evaluateMeasure("https://build.fhir.org/ig/HL7/davinci-ra/ConditionCategoryPOC",
                "2022-01-01", "2022-12-31", "subject",
                "Patient/hist-closed-HCC189", null, null,
                endpoint, endpoint, endpoint, null);

        assertNotNull(report);
    }
}
