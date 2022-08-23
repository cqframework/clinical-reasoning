package org.opencds.cqf.cql.evaluator.measure.r4;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import java.util.HashSet;

import org.hl7.fhir.r4.model.MeasureReport;
import org.testng.annotations.Test;

public class MeasureProcessorSdeSanityTest extends BaseMeasureProcessorTest {
    public MeasureProcessorSdeSanityTest() {
        super("ContentBundleCustom.json");
    }

    @Test
    public void measure_eval_unique_extension_list() {
        MeasureReport report = this.measureProcessor.evaluateMeasure("http://cds.optum.com/dqm-diabetes/fhir/Measure/DM1Measure",
                "2020-01-01", "2022-06-29", "subject",
                "Patient/DM1-patient-1", null, null,
                endpoint, endpoint, endpoint, null);

        assertNotNull(report);

        HashSet<String> set = new HashSet<>();
        report.getExtension().forEach(x -> set.add(x.getValue().toString()));
        assertEquals(set.size(), report.getExtension().size());
    }
}
