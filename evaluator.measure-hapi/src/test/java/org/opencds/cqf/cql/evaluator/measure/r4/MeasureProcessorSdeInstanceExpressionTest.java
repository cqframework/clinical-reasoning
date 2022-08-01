package org.opencds.cqf.cql.evaluator.measure.r4;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.r4.model.MeasureReport;
import org.testng.annotations.Test;

import static org.testng.Assert.assertNotNull;

public class MeasureProcessorSdeInstanceExpressionTest extends BaseMeasureProcessorTest {
    public MeasureProcessorSdeInstanceExpressionTest() {
        super("ConditionCategoryPoc.json");
    }

    @Test
    public void measure_eval_non_retrieve_resource() {
      //  http://localhost:8080/fhir/Measure/ConditionCategoryPOC/$evaluate-measure?subject=hist-open-HCC189&periodStart=2022-01-01&periodEnd=2022-12-31

        MeasureReport report = this.measureProcessor.evaluateMeasure("https://build.fhir.org/ig/HL7/davinci-ra/ConditionCategoryPOC",
                "2022-01-01", "2022-12-31", "subject",
                "Patient/hist-closed-HCC189", null, null,
                endpoint, endpoint, endpoint, null);

        assertNotNull(report);

        FhirContext context = FhirContext.forR4();
        System.out.println(context.newJsonParser().setPrettyPrint(true).encodeResourceToString(report));

    }
}
