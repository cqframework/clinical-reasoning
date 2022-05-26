package org.opencds.cqf.cql.evaluator.measure.r4;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.r4.model.MeasureReport;
import org.testng.annotations.Test;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

@Test(singleThreaded = true)
public class MeasureProcessorAddExtensionTest extends BaseMeasureProcessorTest {
    public MeasureProcessorAddExtensionTest() {
        super("EXM124-9.0.000-bundle.json");
    }

    @Test
    public void exm124_subject_list() {
        MeasureReport report = this.measureProcessor.evaluateMeasure("http://hl7.org/fhir/us/cqfmeasures/Measure/EXM124",
                "2019-01-01", "2020-01-01", "subject",
                "Patient/numer-EXM124", null, null,
                endpoint, endpoint, endpoint, null);

        assertFalse(report.getEvaluatedResource().isEmpty());
        assertTrue(report.getEvaluatedResource().get(0).hasExtension());
        assertNotNull(report.getEvaluatedResource().get(0).getExtensionByUrl("http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-isPertinent"));

    }


}
