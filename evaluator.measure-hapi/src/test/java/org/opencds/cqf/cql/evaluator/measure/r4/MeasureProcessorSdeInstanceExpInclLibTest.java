package org.opencds.cqf.cql.evaluator.measure.r4;

import org.hl7.fhir.r4.model.MeasureReport;
import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertEquals;

public class MeasureProcessorSdeInstanceExpInclLibTest extends BaseMeasureProcessorTest {
    public MeasureProcessorSdeInstanceExpInclLibTest() {
        super("ContentBundleCustom.json");
    }

    @Test
    public void measure_eval_non_retrieve_resource_incl_lib_ref() {
        MeasureReport report = this.measureProcessor.evaluateMeasure(
                "http://cds.optum.com/dqm-diabetes/fhir/Measure/DM1Measure",
                "2020-01-01", "2022-06-29", "subject",
                "Patient/DM1-patient-1", null, null,
                endpoint, endpoint, endpoint, null);

        assertEquals(3, report.getContained().size());
    }
}
