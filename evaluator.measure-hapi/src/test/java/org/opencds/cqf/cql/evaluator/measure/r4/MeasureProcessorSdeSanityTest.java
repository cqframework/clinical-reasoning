package org.opencds.cqf.cql.evaluator.measure.r4;

//import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.r4.model.MeasureReport;
import org.testng.annotations.Test;

import static org.testng.Assert.assertNotNull;

public class MeasureProcessorSdeSanityTest extends BaseMeasureProcessorTest {
    public MeasureProcessorSdeSanityTest() {
        super("ContentBundleCustom.json");
    }

    @Test
    public void exm124_subject_list() {
        MeasureReport report = this.measureProcessor.evaluateMeasure("http://cds.optum.com/dqm-diabetes/fhir/Measure/DM1Measure",
                "2020-01-01", "2022-06-29", "subject",
                "Patient/DM1-patient-1", null, null,
                endpoint, endpoint, endpoint, null);

        assertNotNull(report);
//        FhirContext fhirContext = FhirContext.forR4();
//        System.out.println("Resource:" + fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(report));
    }
}
