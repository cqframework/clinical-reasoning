package org.opencds.cqf.fhir.cr.measure.r4;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.parser.IParser;
import org.hl7.fhir.r4.model.Measure;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

@TestInstance(Lifecycle.PER_CLASS)
public class R4MeasureReportBuilderTest {

    protected R4MeasureReportBuilder measureReportBuilder;
    protected FhirContext fhirContext;

    @BeforeAll
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
        Measure measureWithoutExtension = (Measure) parser.parseResource(
                R4MeasureReportBuilderTest.class.getResourceAsStream("MeasureBuilderSampleWithoutExtension.json"));
        Measure measureBooleanBias =
                (Measure) parser.parseResource(R4MeasureReportBuilderTest.class.getResourceAsStream(
                        "MeasureBuilderSampleWithPopulationBiasBoolean.json"));
        Measure measureWithEmptyExtension = (Measure) parser.parseResource(
                R4MeasureReportBuilderTest.class.getResourceAsStream("MeasureBuilderSampleWithEmptyExtension.json"));

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
