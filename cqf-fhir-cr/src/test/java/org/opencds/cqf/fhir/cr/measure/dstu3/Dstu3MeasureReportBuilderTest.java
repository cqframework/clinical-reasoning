package org.opencds.cqf.fhir.cr.measure.dstu3;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.parser.IParser;
import org.hl7.fhir.dstu3.model.Measure;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

@TestInstance(Lifecycle.PER_CLASS)
class Dstu3MeasureReportBuilderTest {

    protected Dstu3MeasureReportBuilder measureReportBuilder;
    protected FhirContext fhirContext;

    @BeforeAll
    void setup() {
        this.measureReportBuilder = new Dstu3MeasureReportBuilder();
        this.fhirContext = FhirContext.forCached(FhirVersionEnum.DSTU3);
    }

    @Test
    void checkIfNotBooleanBasedMeasure() {
        IParser parser = fhirContext.newJsonParser();
        Measure measureEncounterBias = (Measure)
                parser.parseResource(Dstu3MeasureReportBuilderTest.class.getResourceAsStream("EXM105FHIR3Sample.json"));
        Measure measureWithoutExtension = (Measure) parser.parseResource(
                Dstu3MeasureReportBuilderTest.class.getResourceAsStream("EXM105FHIR3SampleWithoutExtension.json"));
        Measure measureBooleanBias =
                (Measure) parser.parseResource(Dstu3MeasureReportBuilderTest.class.getResourceAsStream(
                        "EXM105FHIR3SampleWithBoolenPopulationBias.json"));
        Dstu3MeasureBasisDef measureBasisDef = new Dstu3MeasureBasisDef();

        assertNotNull(measureEncounterBias);
        // Encounter Basis
        assertFalse(measureBasisDef.isBooleanBasis(measureEncounterBias));
        // No specified Basis
        assertNotNull(measureWithoutExtension);
        assertFalse(measureBasisDef.isBooleanBasis(measureWithoutExtension));
        // Boolean Basis
        assertNotNull(measureBooleanBias);
        assertTrue(measureBasisDef.isBooleanBasis(measureBooleanBias));
    }
}
