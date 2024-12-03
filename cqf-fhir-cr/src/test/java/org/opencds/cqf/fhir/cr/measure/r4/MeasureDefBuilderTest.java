package org.opencds.cqf.fhir.cr.measure.r4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.IMPROVEMENT_NOTATION_SYSTEM_DECREASE;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.IMPROVEMENT_NOTATION_SYSTEM_INCREASE;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.MEASUREREPORT_IMPROVEMENT_NOTATION_EXTENSION;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Measure;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.measure.common.MeasureDef;
import org.opencds.cqf.fhir.cr.measure.common.MeasureScoring;
import org.opencds.cqf.fhir.cr.measure.constant.MeasureConstants;
import org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants;

/**
 * Test MeasureDefBuilder on different scenarios around group level Measure settings and if they are properly being set
 */
public class MeasureDefBuilderTest {
    private final CodeableConcept increase = new CodeableConcept(new Coding(
            MeasureReportConstants.MEASUREREPORT_IMPROVEMENT_NOTATION_SYSTEM,
            IMPROVEMENT_NOTATION_SYSTEM_INCREASE,
            MeasureReportConstants.IMPROVEMENT_NOTATION_SYSTEM_INCREASE_DISPLAY));

    private final CodeableConcept decrease = new CodeableConcept(new Coding(
            MeasureReportConstants.MEASUREREPORT_IMPROVEMENT_NOTATION_SYSTEM,
            IMPROVEMENT_NOTATION_SYSTEM_DECREASE,
            MeasureReportConstants.IMPROVEMENT_NOTATION_SYSTEM_DECREASE_DISPLAY));
    private final CodeableConcept invalid = new CodeableConcept(
            new Coding(MeasureReportConstants.MEASUREREPORT_IMPROVEMENT_NOTATION_SYSTEM, "fake", "Fake"));
    private final FhirContext fhirContext = FhirContext.forCached(FhirVersionEnum.R4);
    private final IParser parser = fhirContext.newJsonParser();

    public MeasureDef measureDefBuilder(
            String group1Basis,
            String group1Scoring,
            CodeableConcept group1ImpNotation,
            String group2Basis,
            String group2Scoring,
            CodeableConcept group2ImpNotation,
            String measureBasis,
            String measureScoring,
            CodeableConcept measureImpNotation) {

        R4MeasureDefBuilder defBuilder = new R4MeasureDefBuilder();
        Measure measure = (org.hl7.fhir.r4.model.Measure)
                parser.parseResource(MeasureDefBuilderTest.class.getResourceAsStream("TemplateMeasure.json"));

        var group1 = measure.getGroup().stream()
                .filter(t -> t.getId().equals("group-1"))
                .findFirst()
                .get();
        if (group1Basis != null) {
            group1.addExtension(new Extension()
                    .setUrl(MeasureConstants.POPULATION_BASIS_URL)
                    .setValue(new CodeType(group1Basis)));
        }
        if (group1Scoring != null) {
            group1.addExtension(new Extension()
                    .setUrl(MeasureConstants.CQFM_SCORING_EXT_URL)
                    .setValue(new CodeableConcept()
                            .addCoding(new Coding()
                                    .setCode(group1Scoring)
                                    .setSystem("http://terminology.hl7.org/CodeSystem/measure-scoring"))));
        }
        if (group1ImpNotation != null) {
            group1.addExtension(new Extension()
                    .setUrl(MEASUREREPORT_IMPROVEMENT_NOTATION_EXTENSION)
                    .setValue(group1ImpNotation));
        }
        var group2 = measure.getGroup().stream()
                .filter(t -> t.getId().equals("group-2"))
                .findFirst()
                .get();
        if (group2Basis != null) {
            group2.addExtension(new Extension()
                    .setUrl(MeasureConstants.POPULATION_BASIS_URL)
                    .setValue(new CodeType(group2Basis)));
        }
        if (group2Scoring != null) {
            group2.addExtension(new Extension()
                    .setUrl(MeasureConstants.CQFM_SCORING_EXT_URL)
                    .setValue(new CodeableConcept()
                            .addCoding(new Coding()
                                    .setCode(group2Scoring)
                                    .setSystem("http://terminology.hl7.org/CodeSystem/measure-scoring"))));
        }
        if (group2ImpNotation != null) {
            group2.addExtension(new Extension()
                    .setUrl(MEASUREREPORT_IMPROVEMENT_NOTATION_EXTENSION)
                    .setValue(group2ImpNotation));
        }
        if (measureScoring != null) {
            measure.setScoring(new CodeableConcept()
                    .addCoding(new Coding()
                            .setCode(measureScoring)
                            .setSystem("http://terminology.hl7.org/CodeSystem/measure-scoring")));
        }
        if (measureImpNotation != null) {
            measure.setImprovementNotation(measureImpNotation);
        }
        if (measureBasis != null) {
            measure.addExtension(new Extension()
                    .setUrl(MeasureConstants.POPULATION_BASIS_URL)
                    .setValue(new CodeType(measureBasis)));
        }
        return defBuilder.build(measure);
    }

    public void validateMeasureDef(
            MeasureDef measureDef,
            boolean group1IsBooleanBasis,
            String group1Basis,
            boolean group1IsGroupImpNotation,
            String group1ImpNotationValue,
            MeasureScoring group1MeasureScoring,
            boolean group2IsBooleanBasis,
            String group2Basis,
            boolean group2IsGroupImpNotation,
            String group2ImpNotationValue,
            MeasureScoring group2MeasureScoring) {

        var group1 = measureDef.groups().stream()
                .filter(t -> t.id().equals("group-1"))
                .findFirst()
                .orElse(null);
        // Basis
        assertEquals(group1IsBooleanBasis, group1.isBooleanBasis());
        assertEquals(group1Basis, group1.getPopulationBasis().code());
        // Improvement Notation
        assertEquals(group1IsGroupImpNotation, group1.isGroupImprovementNotation());
        assertEquals(group1ImpNotationValue, group1.getImprovementNotation().code());
        // Scoring
        assertEquals(group1MeasureScoring, group1.measureScoring());

        var group2 = measureDef.groups().stream()
                .filter(t -> t.id().equals("group-2"))
                .findFirst()
                .orElse(null);
        // Basis
        assertEquals(group2IsBooleanBasis, group2.isBooleanBasis());
        assertEquals(group2Basis, group2.getPopulationBasis().code());
        // Improvement Notation
        assertEquals(group2IsGroupImpNotation, group2.isGroupImprovementNotation());
        assertEquals(group2ImpNotationValue, group2.getImprovementNotation().code());
        // Scoring
        assertEquals(group2MeasureScoring, group2.measureScoring());
    }

    @Test
    void basisMeasure() {
        var def = measureDefBuilder(null, null, null, null, null, null, "boolean", "ratio", decrease);

        validateMeasureDef(
                def,
                true,
                "boolean",
                false,
                "decrease",
                MeasureScoring.RATIO,
                true,
                "boolean",
                false,
                "decrease",
                MeasureScoring.RATIO);
    }

    @Test
    void basisMeasureAndGroup() {
        var def = measureDefBuilder(
                "Encounter", "cohort", increase, "Encounter", "cohort", increase, "boolean", "ratio", decrease);

        validateMeasureDef(
                def,
                false,
                "Encounter",
                true,
                "increase",
                MeasureScoring.COHORT,
                false,
                "Encounter",
                true,
                "increase",
                MeasureScoring.COHORT);
    }

    @Test
    void basisOnlyGroup() {
        var def = measureDefBuilder("Encounter", "cohort", increase, "Encounter", "cohort", increase, null, null, null);

        validateMeasureDef(
                def,
                false,
                "Encounter",
                true,
                "increase",
                MeasureScoring.COHORT,
                false,
                "Encounter",
                true,
                "increase",
                MeasureScoring.COHORT);
    }

    @Test
    void basisDifferentGroup() {
        var def = measureDefBuilder("Encounter", "cohort", decrease, "boolean", "cohort", increase, null, null, null);

        validateMeasureDef(
                def,
                false,
                "Encounter",
                true,
                "decrease",
                MeasureScoring.COHORT,
                true,
                "boolean",
                true,
                "increase",
                MeasureScoring.COHORT);
    }

    @Test
    void basisNotDefined() {
        var def = measureDefBuilder(null, "cohort", decrease, null, "cohort", increase, null, null, null);

        validateMeasureDef(
                def,
                true,
                "boolean",
                true,
                "decrease",
                MeasureScoring.COHORT,
                true,
                "boolean",
                true,
                "increase",
                MeasureScoring.COHORT);
    }

    @Test
    void basisPartiallyDefinedGroup() {
        var def = measureDefBuilder(null, "cohort", decrease, "Encounter", "cohort", increase, null, null, null);

        validateMeasureDef(
                def,
                true,
                "boolean",
                true,
                "decrease",
                MeasureScoring.COHORT,
                false,
                "Encounter",
                true,
                "increase",
                MeasureScoring.COHORT);
    }

    @Test
    void basisInvalidGroup() {
        try {
            measureDefBuilder("fish", "cohort", decrease, "fish", "cohort", increase, null, null, null);
            fail("invalid code, should fail");
        } catch (FHIRException e) {
            assertTrue(e.getMessage().contains("Unknown FHIRAllTypes code 'fish'"));
        }
    }

    @Test
    void BasisMeasureInvalid() {
        try {
            measureDefBuilder(null, null, null, null, null, null, "whale", "ratio", decrease);
            fail("invalid code, should fail");
        } catch (FHIRException e) {
            assertTrue(e.getMessage().contains("Unknown FHIRAllTypes code 'whale'"));
        }
    }

    @Test
    void scoringMeasureScoringAndGroup() {
        var def = measureDefBuilder(null, "ratio", null, null, "proportion", null, "boolean", "cohort", decrease);

        validateMeasureDef(
                def,
                true,
                "boolean",
                false,
                "decrease",
                MeasureScoring.RATIO,
                true,
                "boolean",
                false,
                "decrease",
                MeasureScoring.PROPORTION);
    }

    @Test
    void scoringMeasure() {
        var def = measureDefBuilder(null, null, null, null, null, null, "boolean", "cohort", decrease);

        validateMeasureDef(
                def,
                true,
                "boolean",
                false,
                "decrease",
                MeasureScoring.COHORT,
                true,
                "boolean",
                false,
                "decrease",
                MeasureScoring.COHORT);
    }

    @Test
    void groupScoring() {
        var def = measureDefBuilder(null, "ratio", null, null, "proportion", null, "boolean", null, decrease);

        validateMeasureDef(
                def,
                true,
                "boolean",
                false,
                "decrease",
                MeasureScoring.RATIO,
                true,
                "boolean",
                false,
                "decrease",
                MeasureScoring.PROPORTION);
    }

    @Test
    void noScoring() {
        try {
            measureDefBuilder(null, null, null, null, null, null, "boolean", null, null);
            fail("measureScoring needs to be defined");
        } catch (InvalidRequestException e) {
            assertTrue(e.getMessage().contains("MeasureScoring must be specified on Group or Measure"));
        }
    }

    @Test
    void invalidMeasureScoring() {
        try {
            measureDefBuilder(null, null, null, null, null, null, "boolean", "attestation", null);
            fail("measureScoring needs to be defined");
        } catch (InvalidRequestException e) {
            assertTrue(
                    e.getMessage()
                            .contains(
                                    "Measure Scoring code: attestation, is not a valid Measure Scoring Type for measure: null."));
        }
    }

    @Test
    void invalidGroupScoring() {
        try {
            measureDefBuilder(null, "attestation", null, null, "attestation", null, "boolean", null, null);
            fail("measureScoring needs to be defined");
        } catch (InvalidRequestException e) {
            assertTrue(
                    e.getMessage()
                            .contains(
                                    "Measure Scoring code: attestation, is not a valid Measure Scoring Type for measure: null."));
        }
    }

    @Test
    void groupAndMeasureImprovementNotation() {
        var def = measureDefBuilder(null, "ratio", increase, null, "proportion", increase, "boolean", null, decrease);

        validateMeasureDef(
                def,
                true,
                "boolean",
                true,
                "increase",
                MeasureScoring.RATIO,
                true,
                "boolean",
                true,
                "increase",
                MeasureScoring.PROPORTION);
    }

    @Test
    void groupImprovementNotation() {
        var def = measureDefBuilder(null, "ratio", increase, null, "proportion", increase, "boolean", null, null);

        validateMeasureDef(
                def,
                true,
                "boolean",
                true,
                "increase",
                MeasureScoring.RATIO,
                true,
                "boolean",
                true,
                "increase",
                MeasureScoring.PROPORTION);
    }

    @Test
    void measureImprovementNotation() {
        var def = measureDefBuilder(null, "ratio", null, null, "proportion", null, "boolean", null, decrease);

        validateMeasureDef(
                def,
                true,
                "boolean",
                false,
                "decrease",
                MeasureScoring.RATIO,
                true,
                "boolean",
                false,
                "decrease",
                MeasureScoring.PROPORTION);
    }

    @Test
    void noImprovementNotation() {
        var def = measureDefBuilder(null, "ratio", null, null, "proportion", null, "boolean", null, null);

        validateMeasureDef(
                def,
                true,
                "boolean",
                false,
                "increase",
                MeasureScoring.RATIO,
                true,
                "boolean",
                false,
                "increase",
                MeasureScoring.PROPORTION);
    }

    @Test
    void invalidImprovementNotation() {
        try {
            measureDefBuilder(null, "ratio", invalid, null, "proportion", invalid, "boolean", null, null);
            fail("invalid improvement Notation value");
        } catch (InvalidRequestException e) {
            assertTrue(
                    e.getMessage()
                            .contains(
                                    "ImprovementNotation Coding has invalid System: http://terminology.hl7.org/CodeSystem/measure-improvement-notation, code: fake, combination for Measure: null"));
        }
    }
}
