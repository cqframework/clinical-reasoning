package org.opencds.cqf.cql.evaluator.measure.r4;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import org.hl7.fhir.r4.model.Measure;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.parser.IParser;

public class R4MeasureReportBuilderTest {

    protected R4MeasureReportBuilder measureReportBuilder;
    protected FhirContext fhirContext;

    @BeforeClass
    public void setup() {
        this.measureReportBuilder = new R4MeasureReportBuilder();
        this.fhirContext = FhirContext.forCached(FhirVersionEnum.R4);
    }

    @Test
    public void checkIfNotBooleanBasedMeasure() {
        IParser parser = fhirContext.newJsonParser();
        Measure measureEncounterBias =
                (Measure) parser.parseResource(R4MeasureReportBuilderTest.class.getResourceAsStream(
                        "MeasureBuilderSampleWithPopulationBiasEncounter.json"));
        Measure measureWithoutExtension =
                (Measure) parser.parseResource(R4MeasureReportBuilderTest.class
                        .getResourceAsStream("MeasureBuilderSampleWithoutExtension.json"));
        Measure measureBooleanBias = (Measure) parser.parseResource(R4MeasureReportBuilderTest.class
                .getResourceAsStream("MeasureBuilderSampleWithPopulationBiasBoolean.json"));
        Measure measureWithEmptyExtension =
                (Measure) parser.parseResource(R4MeasureReportBuilderTest.class
                        .getResourceAsStream("MeasureBuilderSampleWithEmptyExtension.json"));

        assertNotNull(measureEncounterBias);
        assertFalse(this.measureReportBuilder.isBooleanBasis(measureEncounterBias));

        assertNotNull(measureWithoutExtension);
        assertFalse(this.measureReportBuilder.isBooleanBasis(measureWithoutExtension));

        assertNotNull(measureBooleanBias);
        assertTrue(this.measureReportBuilder.isBooleanBasis(measureBooleanBias));

        assertNotNull(measureWithEmptyExtension);
        assertFalse(this.measureReportBuilder.isBooleanBasis(measureWithEmptyExtension));

    }
}
