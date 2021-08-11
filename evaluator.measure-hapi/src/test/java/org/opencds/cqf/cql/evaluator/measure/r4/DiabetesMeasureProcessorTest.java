package org.opencds.cqf.cql.evaluator.measure.r4;

import org.testng.annotations.Test;

import ca.uhn.fhir.parser.IParser;

import org.hl7.fhir.r4.model.MeasureReport;

import static org.testng.Assert.assertNotNull;

public class DiabetesMeasureProcessorTest extends BaseMeasureProcessorTest {

        
    public DiabetesMeasureProcessorTest() {
        super("DiabetesHemoglobinA1cHbA1cPoorControl9FHIR-bundle.json");
    }

    @Test
    public void a1c_population() {

        MeasureReport report = this.measureProcessor.evaluateMeasure("http://hl7.org/fhir/us/chronic-ds/Measure/DiabetesHemoglobinA1cHbA1cPoorControl9FHIR", "2019-01-01", "2020-01-01", "subject-list", null, null, null, endpoint, endpoint, endpoint, null);
        assertNotNull(report);

        // IParser parser = fhirContext.newJsonParser();
        // parser.setPrettyPrint(true);

        // System.out.println(parser.encodeResourceToString(report));
    }
}
