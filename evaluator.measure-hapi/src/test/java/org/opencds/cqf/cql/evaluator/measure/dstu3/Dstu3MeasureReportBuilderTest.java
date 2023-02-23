package org.opencds.cqf.cql.evaluator.measure.dstu3;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.parser.IParser;
import org.hl7.fhir.dstu3.model.Measure;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class Dstu3MeasureReportBuilderTest {

    protected Dstu3MeasureReportBuilder measureReportBuilder;
    protected FhirContext fhirContext;

    @BeforeClass
    public void setup() {
        this.measureReportBuilder = new Dstu3MeasureReportBuilder();
        this.fhirContext = FhirContext.forCached(FhirVersionEnum.DSTU3);
    }

    @Test
    public void checkIfNotBooleanBasedMeasure() {
        IParser parser = fhirContext.newJsonParser();
        Measure measureEncounterBias = (Measure) parser.parseResource(
                Dstu3MeasureReportBuilderTest.class.getResourceAsStream("EXM105FHIR3Sample.json"));
        Measure measureWithoutExtension =
                (Measure) parser.parseResource(Dstu3MeasureReportBuilderTest.class
                        .getResourceAsStream("EXM105FHIR3SampleWithoutExtension.json"));
        Measure measureBooleanBias =
                (Measure) parser.parseResource(Dstu3MeasureReportBuilderTest.class
                        .getResourceAsStream("EXM105FHIR3SampleWithBoolenPopulationBias.json"));

        assertNotNull(measureEncounterBias);
        assertTrue(this.measureReportBuilder.checkIfNotBooleanBasedMeasure(measureEncounterBias));

        assertNotNull(measureWithoutExtension);
        assertFalse(
                this.measureReportBuilder.checkIfNotBooleanBasedMeasure(measureWithoutExtension));

        assertNotNull(measureBooleanBias);
        assertFalse(this.measureReportBuilder.checkIfNotBooleanBasedMeasure(measureBooleanBias));

    }
}
