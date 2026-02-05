package org.opencds.cqf.fhir.cr.measure.r4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.IMPROVEMENT_NOTATION_SYSTEM_DECREASE;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.IMPROVEMENT_NOTATION_SYSTEM_INCREASE;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.MEASUREREPORT_IMPROVEMENT_NOTATION_EXTENSION;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Expression;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.Measure.MeasureGroupStratifierComponent;
import org.hl7.fhir.r4.model.Measure.MeasureGroupStratifierComponentComponent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.opencds.cqf.fhir.cr.measure.MeasureStratifierType;
import org.opencds.cqf.fhir.cr.measure.common.CodeDef;
import org.opencds.cqf.fhir.cr.measure.common.ConceptDef;
import org.opencds.cqf.fhir.cr.measure.common.GroupDef;
import org.opencds.cqf.fhir.cr.measure.common.MeasureDef;
import org.opencds.cqf.fhir.cr.measure.common.MeasureScoring;
import org.opencds.cqf.fhir.cr.measure.common.StratifierComponentDef;
import org.opencds.cqf.fhir.cr.measure.common.StratifierDef;
import org.opencds.cqf.fhir.cr.measure.constant.MeasureConstants;
import org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants;

/**
 * Test MeasureDefBuilder on different scenarios around group level Measure settings and if they are properly being set
 */
class MeasureDefBuilderTest {
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
            @Nullable String group1Scoring,
            CodeableConcept group1ImpNotation,
            List<MeasureGroupStratifierComponent> group1Stratifiers,
            String group2Basis,
            @Nullable String group2Scoring,
            CodeableConcept group2ImpNotation,
            List<MeasureGroupStratifierComponent> group2Stratifiers,
            String measureBasis,
            @Nullable String measureScoring,
            CodeableConcept measureImpNotation) {

        if (measureScoring != null && group1Scoring != null || group2Scoring != null) {
            throw new IllegalArgumentException("Cannot have both a measure and group score at the same time");
        }

        R4MeasureDefBuilder defBuilder = new R4MeasureDefBuilder();
        Measure measure = (org.hl7.fhir.r4.model.Measure)
                parser.parseResource(MeasureDefBuilderTest.class.getResourceAsStream("TemplateMeasure.json"));

        var group1 = measure.getGroup().stream()
                .filter(t -> t.getId().equals("group-1"))
                .findFirst()
                .orElseThrow();
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
        Optional.ofNullable(group1Stratifiers).ifPresent(nonNull -> nonNull.forEach(group1::addStratifier));
        var group2 = measure.getGroup().stream()
                .filter(t -> t.getId().equals("group-2"))
                .findFirst()
                .orElse(null);
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
        Optional.ofNullable(group2Stratifiers).ifPresent(nonNull -> nonNull.forEach(group2::addStratifier));
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
            MeasureScoring measureScoring,
            boolean group1IsBooleanBasis,
            String group1Basis,
            boolean group1IsGroupImpNotation,
            String group1ImpNotationValue,
            MeasureScoring group1DirectMeasureScoring,
            MeasureScoring group1CombinedMeasureScoring,
            List<StratifierDef> group1Stratifiers,
            boolean group2IsBooleanBasis,
            String group2Basis,
            boolean group2IsGroupImpNotation,
            String group2ImpNotationValue,
            MeasureScoring group2DirectMeasureScoring,
            MeasureScoring group2CombinedMeasureScoring,
            List<StratifierDef> group2Stratifiers) {

        assertEquals(measureScoring, measureDef.measureScoring());

        var groupsById = measureDef.groups().stream().collect(Collectors.toMap(GroupDef::id, entry -> entry));

        var group1 = groupsById.get("group-1");
        // Basis
        assertEquals(group1IsBooleanBasis, group1.isBooleanBasis());
        assertEquals(group1Basis, group1.getPopulationBasis().code());
        // Improvement Notation
        assertEquals(group1IsGroupImpNotation, group1.isGroupImprovementNotation());
        assertEquals(group1ImpNotationValue, group1.getImprovementNotation().code());
        // Scoring
        assertEquals(group1DirectMeasureScoring, group1.measureScoring());
        assertEquals(group1CombinedMeasureScoring, group1.getMeasureOrGroupScoring(measureDef));
        validateStratifiers(group1Stratifiers, group1);

        var group2 = groupsById.get("group-2");
        // Basis
        assertNotNull(group2);
        assertEquals(group2IsBooleanBasis, group2.isBooleanBasis());
        assertEquals(group2Basis, group2.getPopulationBasis().code());
        // Improvement Notation
        assertEquals(group2IsGroupImpNotation, group2.isGroupImprovementNotation());
        assertEquals(group2ImpNotationValue, group2.getImprovementNotation().code());
        // Scoring
        assertEquals(group2DirectMeasureScoring, group2.measureScoring());
        assertEquals(group2CombinedMeasureScoring, group2.getMeasureOrGroupScoring(measureDef));
        validateStratifiers(group2Stratifiers, group2);
    }

    @Test
    void basisMeasure() {
        var def = measureDefBuilder(null, null, null, null, null, null, null, null, "boolean", "ratio", decrease);

        validateMeasureDef(
                def,
                MeasureScoring.RATIO,
                true,
                "boolean",
                false,
                "decrease",
                null,
                MeasureScoring.RATIO,
                null,
                true,
                "boolean",
                false,
                "decrease",
                null,
                MeasureScoring.RATIO,
                null);
    }

    @Test
    void basisMeasureAndGroup() {
        var def = measureDefBuilder(
                "Encounter", "cohort", increase, null, "Encounter", "ratio", increase, null, "boolean", null, decrease);

        validateMeasureDef(
                def,
                null,
                false,
                "Encounter",
                true,
                "increase",
                MeasureScoring.COHORT,
                MeasureScoring.COHORT,
                null,
                false,
                "Encounter",
                true,
                "increase",
                MeasureScoring.RATIO,
                MeasureScoring.RATIO,
                null);
    }

    @Test
    void basisOnlyGroup() {
        var def = measureDefBuilder(
                "Encounter", "cohort", increase, null, "Encounter", "cohort", increase, null, null, null, null);

        validateMeasureDef(
                def,
                null,
                false,
                "Encounter",
                true,
                "increase",
                MeasureScoring.COHORT,
                MeasureScoring.COHORT,
                null,
                false,
                "Encounter",
                true,
                "increase",
                MeasureScoring.COHORT,
                MeasureScoring.COHORT,
                null);
    }

    @Test
    void basisDifferentGroup() {
        var def = measureDefBuilder(
                "Encounter", "cohort", decrease, null, "boolean", "cohort", increase, null, null, null, null);

        validateMeasureDef(
                def,
                null,
                false,
                "Encounter",
                true,
                "decrease",
                MeasureScoring.COHORT,
                MeasureScoring.COHORT,
                null,
                true,
                "boolean",
                true,
                "increase",
                MeasureScoring.COHORT,
                MeasureScoring.COHORT,
                null);
    }

    @Test
    void basisNotDefined() {
        var def = measureDefBuilder(null, "cohort", decrease, null, null, "cohort", increase, null, null, null, null);

        validateMeasureDef(
                def,
                null,
                true,
                "boolean",
                true,
                "decrease",
                MeasureScoring.COHORT,
                MeasureScoring.COHORT,
                null,
                true,
                "boolean",
                true,
                "increase",
                MeasureScoring.COHORT,
                MeasureScoring.COHORT,
                null);
    }

    @Test
    void basisPartiallyDefinedGroup() {
        var def = measureDefBuilder(
                null, "cohort", decrease, null, "Encounter", "cohort", increase, null, null, null, null);

        validateMeasureDef(
                def,
                null,
                true,
                "boolean",
                true,
                "decrease",
                MeasureScoring.COHORT,
                MeasureScoring.COHORT,
                null,
                false,
                "Encounter",
                true,
                "increase",
                MeasureScoring.COHORT,
                MeasureScoring.COHORT,
                null);
    }

    @Test
    void basisInvalidGroup() {
        try {
            measureDefBuilder("fish", "cohort", decrease, null, "fish", "cohort", increase, null, null, null, null);
            fail("invalid code, should fail");
        } catch (FHIRException e) {
            assertTrue(e.getMessage().contains("Unknown FHIRAllTypes code 'fish'"));
        }
    }

    @Test
    void BasisMeasureInvalid() {
        try {
            measureDefBuilder(null, null, null, null, null, null, null, null, "whale", "ratio", decrease);
            fail("invalid code, should fail");
        } catch (FHIRException e) {
            assertTrue(e.getMessage().contains("Unknown FHIRAllTypes code 'whale'"));
        }
    }

    private record ScoringMeasureScoringAndGroupParams(
            String measureScoring,
            @Nullable String group1Scoring,
            @Nullable String group2Scoring,
            MeasureScoring expectedMeasureScoring,
            MeasureScoring expectedDirectGroup1Scoring,
            MeasureScoring expectedCombinedGroup1Scoring,
            MeasureScoring expectedDirectGroup2Scoring,
            MeasureScoring expectedCombinedGroup2Scoring) {}

    private static Stream<ScoringMeasureScoringAndGroupParams> scoringMeasureScoringAndGroupParams() {
        return Stream.of(
                new ScoringMeasureScoringAndGroupParams(
                        "cohort",
                        null,
                        null,
                        MeasureScoring.COHORT,
                        null,
                        MeasureScoring.COHORT,
                        null,
                        MeasureScoring.COHORT),
                new ScoringMeasureScoringAndGroupParams(
                        null,
                        "cohort",
                        "ratio",
                        null,
                        MeasureScoring.COHORT,
                        MeasureScoring.COHORT,
                        MeasureScoring.RATIO,
                        MeasureScoring.RATIO));
    }

    @ParameterizedTest(name = "{index} => testCase={0}")
    @MethodSource("scoringMeasureScoringAndGroupParams")
    void scoringMeasureScoringAndGroup(ScoringMeasureScoringAndGroupParams testCase) {
        var def = measureDefBuilder(
                null,
                testCase.group1Scoring(),
                null,
                null,
                null,
                testCase.group2Scoring(),
                null,
                null,
                "boolean",
                testCase.measureScoring(),
                decrease);

        validateMeasureDef(
                def,
                testCase.expectedMeasureScoring(),
                true,
                "boolean",
                false,
                "decrease",
                testCase.expectedDirectGroup1Scoring(),
                testCase.expectedCombinedGroup1Scoring(),
                null,
                true,
                "boolean",
                false,
                "decrease",
                testCase.expectedDirectGroup2Scoring(),
                testCase.expectedCombinedGroup2Scoring(),
                null);
    }

    @Test
    void noScoring() {
        try {
            measureDefBuilder(null, null, null, null, null, null, null, null, "boolean", null, null);
            fail("measureScoring needs to be defined");
        } catch (InvalidRequestException e) {
            assertTrue(e.getMessage().contains("MeasureScoring must be specified on Group or Measure"));
        }
    }

    @Test
    void invalidMeasureScoring() {
        try {
            measureDefBuilder(null, null, null, null, null, null, null, null, "boolean", "attestation", null);
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
            measureDefBuilder(null, "attestation", null, null, null, "attestation", null, null, "boolean", null, null);
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
        var def = measureDefBuilder(
                null, "ratio", increase, null, null, "proportion", increase, null, "boolean", null, decrease);

        validateMeasureDef(
                def,
                null,
                true,
                "boolean",
                true,
                "increase",
                MeasureScoring.RATIO,
                MeasureScoring.RATIO,
                null,
                true,
                "boolean",
                true,
                "increase",
                MeasureScoring.PROPORTION,
                MeasureScoring.PROPORTION,
                null);
    }

    @Test
    void groupImprovementNotation() {
        var def = measureDefBuilder(
                null, "ratio", increase, null, null, "proportion", increase, null, "boolean", null, null);

        validateMeasureDef(
                def,
                null,
                true,
                "boolean",
                true,
                "increase",
                MeasureScoring.RATIO,
                MeasureScoring.RATIO,
                null,
                true,
                "boolean",
                true,
                "increase",
                MeasureScoring.PROPORTION,
                MeasureScoring.PROPORTION,
                null);
    }

    @Test
    void noImprovementNotation() {
        var def = measureDefBuilder(null, "ratio", null, null, null, "proportion", null, null, "boolean", null, null);

        validateMeasureDef(
                def,
                null,
                true,
                "boolean",
                false,
                "increase",
                MeasureScoring.RATIO,
                MeasureScoring.RATIO,
                null,
                true,
                "boolean",
                false,
                "increase",
                MeasureScoring.PROPORTION,
                MeasureScoring.PROPORTION,
                null);
    }

    @Test
    void invalidImprovementNotation() {
        try {
            measureDefBuilder(null, "ratio", invalid, null, null, "proportion", invalid, null, "boolean", null, null);
            fail("invalid improvement Notation value");
        } catch (InvalidRequestException e) {
            assertTrue(
                    e.getMessage()
                            .contains(
                                    "ImprovementNotation Coding has invalid System: http://terminology.hl7.org/CodeSystem/measure-improvement-notation, code: fake, combination for Measure: null"));
        }
    }

    private record BasicStratifiersParams(
            List<MeasureGroupStratifierComponent> inputStratifiersGroup1,
            List<MeasureGroupStratifierComponent> inputStratifiersGroup2,
            List<StratifierDef> outputStratifiersGroup1,
            List<StratifierDef> outputStratifiersGroup2) {}

    public static Stream<BasicStratifiersParams> basicStratifiersParams() {
        return Stream.of(
                new BasicStratifiersParams(
                        buildInputStratifiers(),
                        buildInputStratifiers(),
                        buildOutputStratifiers(),
                        buildOutputStratifiers()),
                new BasicStratifiersParams(
                        buildInputStratifiers(1),
                        buildInputStratifiers(2),
                        buildOutputStratifiers(1),
                        buildOutputStratifiers(2)),
                new BasicStratifiersParams(
                        buildInputStratifiers("InitialPopulation"),
                        buildInputStratifiers("Denominator"),
                        buildOutputStratifiers("InitialPopulation"),
                        buildOutputStratifiers("Denominator")));
    }

    @ParameterizedTest(name = "{index} => testCase={0}")
    @MethodSource("basicStratifiersParams")
    void basicStratifiers(BasicStratifiersParams testCase) {
        var def = measureDefBuilder(
                null,
                "ratio",
                null,
                testCase.inputStratifiersGroup1(),
                null,
                "proportion",
                null,
                testCase.inputStratifiersGroup2(),
                "boolean",
                null,
                null);

        validateMeasureDef(
                def,
                null,
                true,
                "boolean",
                false,
                "increase",
                MeasureScoring.RATIO,
                MeasureScoring.RATIO,
                testCase.outputStratifiersGroup1(),
                true,
                "boolean",
                false,
                "increase",
                MeasureScoring.PROPORTION,
                MeasureScoring.PROPORTION,
                testCase.outputStratifiersGroup2());
    }

    private static List<MeasureGroupStratifierComponent> buildInputStratifiers(String... expressions) {
        return buildInputStratifiers(0, expressions);
    }

    private static List<MeasureGroupStratifierComponent> buildInputStratifiers(
            int componentCount, String... expressions) {
        return Arrays.stream(expressions)
                .map(expression -> buildInputStratifier(componentCount, expression))
                .toList();
    }

    private static MeasureGroupStratifierComponent buildInputStratifier(int componentCount, String expression) {
        final MeasureGroupStratifierComponent component =
                (MeasureGroupStratifierComponent) new MeasureGroupStratifierComponent()
                        .setCode(new CodeableConcept()
                                .addCoding(new Coding().setSystem("system").setCode("code"))
                                .setText(expression))
                        .setCriteria(new Expression().setExpression(expression))
                        .setId(expression);

        IntStream.range(0, componentCount)
                .forEach(num -> component.addComponent(buildInputStratifierComponent(expression + num)));

        return component;
    }

    @Nonnull
    private static MeasureGroupStratifierComponentComponent buildInputStratifierComponent(String expression) {
        return (MeasureGroupStratifierComponentComponent) new MeasureGroupStratifierComponentComponent()
                .setCode(new CodeableConcept().setCoding(List.of(new Coding())).setText(expression))
                .setId(expression);
    }

    private static List<StratifierDef> buildOutputStratifiers(String... expressions) {
        return buildOutputStratifiers(0, expressions);
    }

    private static List<StratifierDef> buildOutputStratifiers(int componentCount, String... expressions) {
        return Arrays.stream(expressions)
                .map(expression -> buildOutputStratifierDef(componentCount, expression))
                .toList();
    }

    private static StratifierDef buildOutputStratifierDef(int componentCount, String expression) {
        return new StratifierDef(
                expression,
                new ConceptDef(List.of(new CodeDef("system", "code")), expression),
                expression,
                MeasureStratifierType.VALUE,
                IntStream.range(0, componentCount)
                        .mapToObj(num -> buildOutputStratifierComponentDef(expression + num))
                        .toList());
    }

    private static StratifierComponentDef buildOutputStratifierComponentDef(String text) {
        return new StratifierComponentDef(text, new ConceptDef(List.of(new CodeDef(null, null)), text), null);
    }

    private <T> void assertWithZip(List<T> expectedList, List<T> actualList, BiConsumer<T, T> assertConsumer) {
        if (expectedList == null || expectedList.isEmpty()) {
            // skip validation:
            return;
        }

        assertNotNull(actualList);
        assertFalse(actualList.isEmpty());

        for (int index = 0; index < expectedList.size(); index++) {
            assertConsumer.accept(expectedList.get(index), actualList.get(index));
        }
    }

    private void validateStratifiers(List<StratifierDef> expectedStratifiers, GroupDef actualGroupDef) {
        assertWithZip(expectedStratifiers, actualGroupDef.stratifiers(), this::validateStratifier);
    }

    private void validateStratifier(StratifierDef expectedStratifierDef, StratifierDef actualStratifierDef) {
        assertNotNull(expectedStratifierDef);

        assertEquals(expectedStratifierDef.id(), actualStratifierDef.id());
        assertComponentsEqual(expectedStratifierDef.components(), actualStratifierDef.components());
        assertCodesEqual(expectedStratifierDef.code(), actualStratifierDef.code());
        assertEquals(expectedStratifierDef.expression(), actualStratifierDef.expression());
        assertEquals(expectedStratifierDef.getResults(), actualStratifierDef.getResults());
    }

    private void assertComponentsEqual(
            List<StratifierComponentDef> expectedComponents, List<StratifierComponentDef> actualComponents) {

        assertWithZip(expectedComponents, actualComponents, this::assertComponentEquals);
    }

    private void assertComponentEquals(
            StratifierComponentDef expectedComponent, StratifierComponentDef actualComponent) {
        assertEquals(expectedComponent.id(), actualComponent.id());
        assertCodesEqual(expectedComponent.code(), actualComponent.code());
        assertEquals(expectedComponent.expression(), actualComponent.expression());
    }

    private void assertCodesEqual(ConceptDef expectedCode, ConceptDef actualCode) {
        assertWithZip(expectedCode.codes(), actualCode.codes(), this::assertCodeDefEquals);
        assertEquals(expectedCode.text(), actualCode.text());
    }

    private void assertCodeDefEquals(CodeDef expectedCodeDef, CodeDef actualCodeDef) {
        assertEquals(expectedCodeDef.code(), actualCodeDef.code());
    }
}
