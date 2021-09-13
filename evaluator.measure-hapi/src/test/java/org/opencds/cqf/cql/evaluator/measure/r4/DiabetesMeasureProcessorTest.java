package org.opencds.cqf.cql.evaluator.measure.r4;

import org.testng.annotations.Test;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Bundle;

import static org.testng.Assert.assertNotNull;

import java.io.IOException;


@Test(singleThreaded = true)
public class DiabetesMeasureProcessorTest extends BaseMeasureProcessorTest {

        
    public DiabetesMeasureProcessorTest() {
        super("DiabetesHemoglobinA1cHbA1cPoorControl9FHIR-bundle.json");
    }

    @Test
    public void a1c_singlePatient_numerator() {

        MeasureReport report = this.measureProcessor.evaluateMeasure("http://hl7.org/fhir/us/chronic-ds/Measure/DiabetesHemoglobinA1cHbA1cPoorControl9FHIR", "2019-01-01", "2020-01-01", "patient", "numer-CMS122-Patient", null, null, endpoint, endpoint, endpoint, null);
        assertNotNull(report);

        validateGroup(report.getGroup().get(0), "numerator", 1);
        validateGroup(report.getGroup().get(0), "denominator", 1);

        // java.io.File yourFile = new java.io.File("target/sample.json");
        // yourFile.createNewFile(); // if file already exists will do nothing 

        // ca.uhn.fhir.parser.IParser parser = fhirContext.newJsonParser();
        // parser.setPrettyPrint(true);

        // java.io.FileWriter fileWriter = new java.io.FileWriter(yourFile);
        // fileWriter.write(parser.encodeResourceToString(report));
        // fileWriter.flush();
        // fileWriter.close();


    }

    @Test
    public void a1c_population() throws IOException {

        MeasureReport report = this.measureProcessor.evaluateMeasure("http://hl7.org/fhir/us/chronic-ds/Measure/DiabetesHemoglobinA1cHbA1cPoorControl9FHIR", "2019-01-01", "2020-01-01", "population", null, null, null, endpoint, endpoint, endpoint, null);
        assertNotNull(report);

        validateGroup(report.getGroup().get(0), "numerator", 1);
        validateGroup(report.getGroup().get(0), "denominator", 2);

        // Potentially an error in the data... TBD
        // validateGroup(report.getGroup().get(0), "initial-population", 3);
    }

    @Test
    public void a1c_additionalData() {
        Bundle additionalData = 
        (Bundle) fhirContext.newJsonParser()
                .parseResource(BaseMeasureProcessorTest.class.getResourceAsStream("CMS122-AdditionalData-bundle.json"));

        MeasureReport report = this.measureProcessor.evaluateMeasure("http://hl7.org/fhir/us/chronic-ds/Measure/DiabetesHemoglobinA1cHbA1cPoorControl9FHIR", "2019-01-01", "2020-01-01", "patient", "numer-CMS122-2-Patient", null, null, endpoint, endpoint, null, additionalData);
        assertNotNull(report);

        validateGroup(report.getGroup().get(0), "numerator", 1);
        validateGroup(report.getGroup().get(0), "denominator", 1);
    }
}
