package org.opencds.cqf.cql.evaluator.measure.r4;

import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Reference;
import org.opencds.cqf.cql.evaluator.measure.common.MeasureConstants;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;

@Test(singleThreaded = true)
public class MeasureProcessorEvaluatedResourceAddExtensionTest extends BaseMeasureProcessorTest {
    public MeasureProcessorEvaluatedResourceAddExtensionTest() {
        super("EXM124-9.0.000-bundle.json");
    }

    @Test
    public void exm124_subject_list() {
        MeasureReport report = this.measureProcessor.evaluateMeasure("http://hl7.org/fhir/us/cqfmeasures/Measure/EXM124",
                "2019-01-01", "2020-01-01", "subject",
                "Patient/numer-EXM124", null, null,
                endpoint, endpoint, endpoint, null);

        for(Reference reference :report.getEvaluatedResource()) {
            if( reference.getReference() != null && reference.getReference().equalsIgnoreCase("Observation/numer-EXM124-4")) {
                assertNotNull(reference.getExtensionByUrl(MeasureConstants.EXT_DAVINCI_POPULATION_REFERENCE));
                assertEquals(reference.getExtensionByUrl(MeasureConstants.EXT_DAVINCI_POPULATION_REFERENCE).getValue().toString(), "numerator");
                assertNotNull(reference.getExtensionByUrl(MeasureConstants.EXT_PERTINENT_URI));
            }
            if(reference.getReference() != null &&  reference.getReference().equalsIgnoreCase("Observation/numer-EXM124-3")) {
                assertNotNull(reference.getExtensionByUrl(MeasureConstants.EXT_DAVINCI_POPULATION_REFERENCE));
                assertEquals(reference.getExtensionByUrl(MeasureConstants.EXT_DAVINCI_POPULATION_REFERENCE).getValue().toString(), "numerator");
                assertNull(reference.getExtensionByUrl(MeasureConstants.EXT_PERTINENT_URI));

            }
        }
    }
}
