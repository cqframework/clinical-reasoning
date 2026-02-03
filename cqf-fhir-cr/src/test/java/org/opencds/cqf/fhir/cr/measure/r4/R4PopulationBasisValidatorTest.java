package org.opencds.cqf.fhir.cr.measure.r4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.opencds.cqf.fhir.cr.measure.common.MeasurePopulationType.DENOMINATOR;
import static org.opencds.cqf.fhir.cr.measure.common.MeasurePopulationType.INITIALPOPULATION;
import static org.opencds.cqf.fhir.cr.measure.common.MeasurePopulationType.NUMERATOR;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import jakarta.annotation.Nonnull;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Enumeration;
import org.hl7.fhir.r4.model.GraphDefinition.CompartmentCodeEnumFactory;
import org.hl7.fhir.r4.model.Procedure;
import org.hl7.fhir.r4.model.Range;
import org.hl7.fhir.r4.model.Reference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.opencds.cqf.cql.engine.execution.EvaluationExpressionRef;
import org.opencds.cqf.cql.engine.execution.EvaluationResult;
import org.opencds.cqf.cql.engine.execution.ExpressionResult;
import org.opencds.cqf.cql.engine.runtime.Code;
import org.opencds.cqf.fhir.cr.measure.MeasureStratifierType;
import org.opencds.cqf.fhir.cr.measure.common.CodeDef;
import org.opencds.cqf.fhir.cr.measure.common.GroupDef;
import org.opencds.cqf.fhir.cr.measure.common.MeasureDef;
import org.opencds.cqf.fhir.cr.measure.common.MeasurePopulationType;
import org.opencds.cqf.fhir.cr.measure.common.MeasureScoring;
import org.opencds.cqf.fhir.cr.measure.common.PopulationDef;
import org.opencds.cqf.fhir.cr.measure.common.StratifierComponentDef;
import org.opencds.cqf.fhir.cr.measure.common.StratifierDef;
import org.opencds.cqf.fhir.cr.measure.constant.MeasureConstants;

class R4PopulationBasisValidatorTest {

    private static final String FAKE_MEASURE_URL = "fakeMeasureUrl";
    // Not ENTIRELY realistic since the GroupDefs are ultimately sourced from a MeasureDef, but for this simplistic
    // test, it works
    private static final MeasureDef MEASURE_DEF = new MeasureDef(null, FAKE_MEASURE_URL, null, null, null);
    private static final String EXPRESSION_INITIALPOPULATION = "InitialPopulation";
    private static final String EXPRESSION_DENOMINATOR = "Denominator";
    private static final String EXPRESSION_NUMERATOR = "Numerator";

    private static final Map<MeasurePopulationType, String> POPULATION_TYPE_TO_EXPRESSION = Map.of(
            INITIALPOPULATION, EXPRESSION_INITIALPOPULATION,
            DENOMINATOR, EXPRESSION_DENOMINATOR,
            NUMERATOR, EXPRESSION_NUMERATOR);

    private static final CodeDef BASIS_BOOLEAN = new CodeDef(MeasureConstants.POPULATION_BASIS_URL, "boolean");
    private static final CodeDef BASIS_ENCOUNTER = new CodeDef(MeasureConstants.POPULATION_BASIS_URL, "Encounter");
    private static final CodeDef BASIS_PROCEDURE = new CodeDef(MeasureConstants.POPULATION_BASIS_URL, "Procedure");
    private static final Encounter ENCOUNTER = new Encounter();
    private static final Procedure PROCEDURE = new Procedure();

    private enum Basis {
        BOOLEAN(BASIS_BOOLEAN),
        ENCOUNTER(BASIS_ENCOUNTER),
        PROCEDURE(BASIS_PROCEDURE);

        private final CodeDef codeDef;

        Basis(CodeDef codeDef) {
            this.codeDef = codeDef;
        }
    }

    private final R4PopulationBasisValidator testSubject = new R4PopulationBasisValidator();

    private record ValidateGroupBasisTypeHappyPathParams(GroupDef groupDef, EvaluationResult evaluationResult) {}

    private static Stream<ValidateGroupBasisTypeHappyPathParams> validateGroupBasisTypeHappyPathParams() {
        return Stream.of(
                new ValidateGroupBasisTypeHappyPathParams(
                        buildGroupDef(
                                Basis.BOOLEAN,
                                buildPopulationDefs(Basis.BOOLEAN, INITIALPOPULATION, DENOMINATOR, NUMERATOR),
                                buildStratifierDefs(
                                        MeasureStratifierType.VALUE,
                                        EXPRESSION_INITIALPOPULATION,
                                        EXPRESSION_DENOMINATOR,
                                        EXPRESSION_NUMERATOR)),
                        buildEvaluationResult(Map.of(
                                EXPRESSION_INITIALPOPULATION,
                                Boolean.TRUE,
                                EXPRESSION_DENOMINATOR,
                                Boolean.TRUE,
                                EXPRESSION_NUMERATOR,
                                Boolean.TRUE))),
                new ValidateGroupBasisTypeHappyPathParams(
                        buildGroupDef(
                                Basis.BOOLEAN,
                                buildPopulationDefs(Basis.BOOLEAN, INITIALPOPULATION, DENOMINATOR, NUMERATOR),
                                buildStratifierDefs(
                                        MeasureStratifierType.VALUE,
                                        EXPRESSION_INITIALPOPULATION,
                                        EXPRESSION_DENOMINATOR,
                                        EXPRESSION_NUMERATOR)),
                        buildEvaluationResult(Map.of(
                                EXPRESSION_INITIALPOPULATION,
                                List.of(Boolean.TRUE, Boolean.TRUE, Boolean.TRUE),
                                EXPRESSION_DENOMINATOR,
                                List.of(Boolean.TRUE, Boolean.TRUE, Boolean.TRUE),
                                EXPRESSION_NUMERATOR,
                                List.of(Boolean.TRUE, Boolean.TRUE, Boolean.TRUE)))),
                new ValidateGroupBasisTypeHappyPathParams(
                        buildGroupDef(
                                Basis.ENCOUNTER,
                                buildPopulationDefs(Basis.ENCOUNTER, INITIALPOPULATION, DENOMINATOR, NUMERATOR),
                                buildStratifierDefs(
                                        MeasureStratifierType.VALUE,
                                        EXPRESSION_INITIALPOPULATION,
                                        EXPRESSION_DENOMINATOR,
                                        EXPRESSION_NUMERATOR)),
                        buildEvaluationResult(Map.of(
                                EXPRESSION_INITIALPOPULATION,
                                ENCOUNTER,
                                EXPRESSION_DENOMINATOR,
                                ENCOUNTER,
                                EXPRESSION_NUMERATOR,
                                ENCOUNTER))),
                new ValidateGroupBasisTypeHappyPathParams(
                        buildGroupDef(
                                Basis.ENCOUNTER,
                                buildPopulationDefs(Basis.ENCOUNTER, INITIALPOPULATION, DENOMINATOR, NUMERATOR),
                                buildStratifierDefs(
                                        MeasureStratifierType.VALUE,
                                        EXPRESSION_INITIALPOPULATION,
                                        EXPRESSION_DENOMINATOR,
                                        EXPRESSION_NUMERATOR)),
                        buildEvaluationResult(Map.of(
                                EXPRESSION_INITIALPOPULATION,
                                List.of(ENCOUNTER, ENCOUNTER, ENCOUNTER),
                                EXPRESSION_DENOMINATOR,
                                List.of(ENCOUNTER, ENCOUNTER, ENCOUNTER),
                                EXPRESSION_NUMERATOR,
                                List.of(ENCOUNTER, ENCOUNTER, ENCOUNTER)))),
                new ValidateGroupBasisTypeHappyPathParams(
                        buildGroupDef(
                                Basis.PROCEDURE,
                                buildPopulationDefs(Basis.PROCEDURE, INITIALPOPULATION, DENOMINATOR, NUMERATOR),
                                buildStratifierDefs(
                                        MeasureStratifierType.VALUE,
                                        EXPRESSION_INITIALPOPULATION,
                                        EXPRESSION_DENOMINATOR,
                                        EXPRESSION_NUMERATOR)),
                        buildEvaluationResult(Map.of(
                                EXPRESSION_INITIALPOPULATION,
                                PROCEDURE,
                                EXPRESSION_DENOMINATOR,
                                PROCEDURE,
                                EXPRESSION_NUMERATOR,
                                PROCEDURE))),
                new ValidateGroupBasisTypeHappyPathParams(
                        buildGroupDef(
                                Basis.PROCEDURE,
                                buildPopulationDefs(Basis.PROCEDURE, INITIALPOPULATION, DENOMINATOR, NUMERATOR),
                                buildStratifierDefs(
                                        MeasureStratifierType.VALUE,
                                        EXPRESSION_INITIALPOPULATION,
                                        EXPRESSION_DENOMINATOR,
                                        EXPRESSION_NUMERATOR)),
                        buildEvaluationResult(Map.of(
                                EXPRESSION_INITIALPOPULATION,
                                List.of(PROCEDURE, PROCEDURE, PROCEDURE),
                                EXPRESSION_DENOMINATOR,
                                List.of(PROCEDURE, PROCEDURE, PROCEDURE),
                                EXPRESSION_NUMERATOR,
                                List.of(PROCEDURE, PROCEDURE, PROCEDURE)))));
    }

    @ParameterizedTest(name = "{index} => testCase={0}")
    @MethodSource("validateGroupBasisTypeHappyPathParams")
    void validateGroupBasisTypeHappyPath(ValidateGroupBasisTypeHappyPathParams testCase) {
        testSubject.validateGroupPopulations(MEASURE_DEF, testCase.groupDef(), testCase.evaluationResult());
    }

    private record ValidateGroupBasisTypeErrorPathParams(
            GroupDef groupDef, EvaluationResult evaluationResult, String expectedExceptionMessage) {}

    private static Stream<ValidateGroupBasisTypeErrorPathParams> validateGroupBasisTypeErrorPathParams() {
        return Stream.of(
                new ValidateGroupBasisTypeErrorPathParams(
                        buildGroupDef(
                                Basis.BOOLEAN,
                                buildPopulationDefs(Basis.BOOLEAN, INITIALPOPULATION, DENOMINATOR, NUMERATOR),
                                buildStratifierDefs(
                                        MeasureStratifierType.VALUE,
                                        EXPRESSION_INITIALPOPULATION,
                                        EXPRESSION_DENOMINATOR,
                                        EXPRESSION_NUMERATOR)),
                        buildEvaluationResult(Map.of(
                                EXPRESSION_INITIALPOPULATION,
                                List.of(ENCOUNTER),
                                EXPRESSION_DENOMINATOR,
                                List.of(ENCOUNTER),
                                EXPRESSION_NUMERATOR,
                                List.of(ENCOUNTER))),
                        "group expression criteria results for expression: [InitialPopulation] and scoring: [PROPORTION] must fall within accepted types for population basis: [boolean] for Measure: [fakeMeasureUrl] due to mismatch between total result classes: [Encounter] and matching result classes: []"),
                new ValidateGroupBasisTypeErrorPathParams(
                        buildGroupDef(
                                Basis.BOOLEAN,
                                buildPopulationDefs(Basis.BOOLEAN, INITIALPOPULATION, DENOMINATOR, NUMERATOR),
                                buildStratifierDefs(
                                        MeasureStratifierType.VALUE,
                                        EXPRESSION_INITIALPOPULATION,
                                        EXPRESSION_DENOMINATOR,
                                        EXPRESSION_NUMERATOR)),
                        buildEvaluationResult(Map.of(
                                EXPRESSION_INITIALPOPULATION,
                                List.of(Boolean.TRUE, Boolean.TRUE, ENCOUNTER),
                                EXPRESSION_DENOMINATOR,
                                List.of(Boolean.TRUE, Boolean.TRUE, Boolean.TRUE),
                                EXPRESSION_NUMERATOR,
                                List.of(Boolean.TRUE, Boolean.TRUE, Boolean.TRUE))),
                        "group expression criteria results for expression: [InitialPopulation] and scoring: [PROPORTION] must fall within accepted types for population basis: [boolean] for Measure: [fakeMeasureUrl] due to mismatch between total result classes: [Boolean, Boolean, Encounter] and matching result classes: [Boolean, Boolean]"),
                new ValidateGroupBasisTypeErrorPathParams(
                        buildGroupDef(
                                Basis.BOOLEAN,
                                buildPopulationDefs(Basis.BOOLEAN, INITIALPOPULATION, DENOMINATOR, NUMERATOR),
                                buildStratifierDefs(
                                        MeasureStratifierType.VALUE,
                                        EXPRESSION_INITIALPOPULATION,
                                        EXPRESSION_DENOMINATOR,
                                        EXPRESSION_NUMERATOR)),
                        buildEvaluationResult(Map.of(
                                EXPRESSION_INITIALPOPULATION,
                                List.of(Boolean.TRUE, Boolean.TRUE, Boolean.TRUE),
                                EXPRESSION_DENOMINATOR,
                                List.of(Boolean.TRUE, Boolean.TRUE, Boolean.TRUE),
                                EXPRESSION_NUMERATOR,
                                List.of(Boolean.TRUE, Boolean.TRUE, ENCOUNTER))),
                        "group expression criteria results for expression: [Numerator] and scoring: [PROPORTION] must fall within accepted types for population basis: [boolean] for Measure: [fakeMeasureUrl] due to mismatch between total result classes: [Boolean, Boolean, Encounter] and matching result classes: [Boolean, Boolean]"),
                new ValidateGroupBasisTypeErrorPathParams(
                        buildGroupDef(
                                Basis.ENCOUNTER,
                                buildPopulationDefs(Basis.ENCOUNTER, INITIALPOPULATION, DENOMINATOR, NUMERATOR),
                                buildStratifierDefs(
                                        MeasureStratifierType.VALUE,
                                        EXPRESSION_INITIALPOPULATION,
                                        EXPRESSION_DENOMINATOR,
                                        EXPRESSION_NUMERATOR)),
                        buildEvaluationResult(Map.of(
                                EXPRESSION_INITIALPOPULATION,
                                Boolean.TRUE,
                                EXPRESSION_DENOMINATOR,
                                Boolean.TRUE,
                                EXPRESSION_NUMERATOR,
                                Boolean.TRUE)),
                        "group expression criteria results for expression: [InitialPopulation] and scoring: [PROPORTION] must fall within accepted types for population basis: [Encounter] for Measure: [fakeMeasureUrl] due to mismatch between total result classes: [Boolean] and matching result classes: []"),
                new ValidateGroupBasisTypeErrorPathParams(
                        buildGroupDef(
                                Basis.ENCOUNTER,
                                buildPopulationDefs(Basis.ENCOUNTER, INITIALPOPULATION, DENOMINATOR, NUMERATOR),
                                buildStratifierDefs(
                                        MeasureStratifierType.VALUE,
                                        EXPRESSION_INITIALPOPULATION,
                                        EXPRESSION_DENOMINATOR,
                                        EXPRESSION_NUMERATOR)),
                        buildEvaluationResult(Map.of(
                                EXPRESSION_INITIALPOPULATION,
                                Boolean.TRUE,
                                EXPRESSION_DENOMINATOR,
                                Boolean.TRUE,
                                EXPRESSION_NUMERATOR,
                                Boolean.TRUE)),
                        "group expression criteria results for expression: [InitialPopulation] and scoring: [PROPORTION] must fall within accepted types for population basis: [Encounter] for Measure: [fakeMeasureUrl] due to mismatch between total result classes: [Boolean] and matching result classes: []"),
                new ValidateGroupBasisTypeErrorPathParams(
                        buildGroupDef(
                                Basis.PROCEDURE,
                                buildPopulationDefs(Basis.PROCEDURE, INITIALPOPULATION, DENOMINATOR, NUMERATOR),
                                buildStratifierDefs(
                                        MeasureStratifierType.VALUE,
                                        EXPRESSION_INITIALPOPULATION,
                                        EXPRESSION_DENOMINATOR,
                                        EXPRESSION_NUMERATOR)),
                        buildEvaluationResult(Map.of(
                                EXPRESSION_INITIALPOPULATION,
                                List.of(ENCOUNTER),
                                EXPRESSION_DENOMINATOR,
                                List.of(ENCOUNTER),
                                EXPRESSION_NUMERATOR,
                                List.of(ENCOUNTER))),
                        "group expression criteria results for expression: [InitialPopulation] and scoring: [PROPORTION] must fall within accepted types for population basis: [Procedure] for Measure: [fakeMeasureUrl] due to mismatch between total result classes: [Encounter] and matching result classes: []"),
                new ValidateGroupBasisTypeErrorPathParams(
                        buildGroupDef(
                                Basis.ENCOUNTER,
                                buildPopulationDefs(Basis.ENCOUNTER, INITIALPOPULATION, DENOMINATOR, NUMERATOR),
                                buildStratifierDefs(
                                        MeasureStratifierType.VALUE,
                                        EXPRESSION_INITIALPOPULATION,
                                        EXPRESSION_DENOMINATOR,
                                        EXPRESSION_NUMERATOR)),
                        buildEvaluationResult(Map.of(
                                EXPRESSION_INITIALPOPULATION,
                                List.of(ENCOUNTER),
                                EXPRESSION_DENOMINATOR,
                                List.of(ENCOUNTER),
                                EXPRESSION_NUMERATOR,
                                List.of(ENCOUNTER, PROCEDURE, ENCOUNTER))),
                        "group expression criteria results for expression: [Numerator] and scoring: [PROPORTION] must fall within accepted types for population basis: [Encounter] for Measure: [fakeMeasureUrl] due to mismatch between total result classes: [Encounter, Procedure, Encounter] and matching result classes: [Encounter, Encounter]"));
    }

    @ParameterizedTest(name = "{index} => testCase={0}")
    @MethodSource("validateGroupBasisTypeErrorPathParams")
    void validateGroupBasisTypeErrorPath(ValidateGroupBasisTypeErrorPathParams testCase) {
        final GroupDef groupDef = testCase.groupDef();
        final EvaluationResult evaluationResult = testCase.evaluationResult();
        try {
            testSubject.validateGroupPopulations(MEASURE_DEF, groupDef, evaluationResult);
            fail("Expected this test to fail");
        } catch (InvalidRequestException exception) {
            assertEquals(testCase.expectedExceptionMessage(), exception.getMessage());
        }
    }

    /**
     *
     * Correction to Non-boolean population basis, these should not return type of Resource, they should stratify results based on single return type per subject
     * Of resulting Encounters, which are tied to Gender M or F, Age range 10-50 or 51-100...etc
     */
    private record ValidateStratifierBasisTypeHappyPathParams(GroupDef groupDef, EvaluationResult evaluationResult) {}

    private static Stream<ValidateStratifierBasisTypeHappyPathParams> validateStratifierBasisTypeHappyPathParams() {
        return Stream.of(
                new ValidateStratifierBasisTypeHappyPathParams(
                        buildGroupDef(
                                Basis.BOOLEAN,
                                buildPopulationDefs(Basis.BOOLEAN, INITIALPOPULATION, DENOMINATOR, NUMERATOR),
                                buildStratifierDefs(
                                        MeasureStratifierType.CRITERIA,
                                        EXPRESSION_INITIALPOPULATION,
                                        EXPRESSION_DENOMINATOR,
                                        EXPRESSION_NUMERATOR)),
                        buildEvaluationResult(Map.of(
                                EXPRESSION_INITIALPOPULATION,
                                Boolean.TRUE,
                                EXPRESSION_DENOMINATOR,
                                Boolean.TRUE,
                                EXPRESSION_NUMERATOR,
                                Boolean.TRUE))),
                new ValidateStratifierBasisTypeHappyPathParams(
                        buildGroupDef(
                                Basis.BOOLEAN,
                                buildPopulationDefs(Basis.BOOLEAN, INITIALPOPULATION, DENOMINATOR, NUMERATOR),
                                buildStratifierDefs(
                                        MeasureStratifierType.VALUE,
                                        EXPRESSION_INITIALPOPULATION,
                                        EXPRESSION_DENOMINATOR,
                                        EXPRESSION_NUMERATOR)),
                        buildEvaluationResult(Map.of(
                                EXPRESSION_INITIALPOPULATION,
                                Boolean.TRUE,
                                EXPRESSION_DENOMINATOR,
                                Boolean.TRUE,
                                EXPRESSION_NUMERATOR,
                                Boolean.TRUE))),
                new ValidateStratifierBasisTypeHappyPathParams(
                        buildGroupDef(
                                Basis.BOOLEAN,
                                buildPopulationDefs(Basis.BOOLEAN, INITIALPOPULATION, DENOMINATOR, NUMERATOR),
                                buildStratifierDefs(
                                        MeasureStratifierType.VALUE,
                                        EXPRESSION_INITIALPOPULATION,
                                        EXPRESSION_DENOMINATOR,
                                        EXPRESSION_NUMERATOR)),
                        buildEvaluationResult(Map.of(
                                EXPRESSION_INITIALPOPULATION,
                                List.of(Boolean.TRUE, Boolean.TRUE, Boolean.TRUE),
                                EXPRESSION_DENOMINATOR,
                                List.of(Boolean.TRUE, Boolean.TRUE, Boolean.TRUE),
                                EXPRESSION_NUMERATOR,
                                List.of(Boolean.TRUE, Boolean.TRUE, Boolean.TRUE)))),
                new ValidateStratifierBasisTypeHappyPathParams(
                        buildGroupDef(
                                Basis.BOOLEAN,
                                buildPopulationDefs(Basis.BOOLEAN, INITIALPOPULATION, DENOMINATOR, NUMERATOR),
                                buildStratifierDefs(
                                        MeasureStratifierType.VALUE,
                                        EXPRESSION_INITIALPOPULATION,
                                        EXPRESSION_DENOMINATOR,
                                        EXPRESSION_NUMERATOR)),
                        buildEvaluationResult(Map.of(
                                EXPRESSION_INITIALPOPULATION,
                                List.of(Boolean.TRUE, new CodeableConcept(), new Range()),
                                EXPRESSION_DENOMINATOR,
                                List.of(new Reference(), new Coding()),
                                EXPRESSION_NUMERATOR,
                                List.of(new Enumeration<>(new CompartmentCodeEnumFactory()), new Code())))),
                new ValidateStratifierBasisTypeHappyPathParams(
                        buildGroupDef(
                                Basis.ENCOUNTER,
                                buildPopulationDefs(Basis.ENCOUNTER, INITIALPOPULATION, DENOMINATOR, NUMERATOR),
                                buildStratifierDefs(
                                        MeasureStratifierType.VALUE,
                                        EXPRESSION_INITIALPOPULATION,
                                        EXPRESSION_DENOMINATOR,
                                        EXPRESSION_NUMERATOR)),
                        buildEvaluationResult(Map.of(
                                EXPRESSION_INITIALPOPULATION,
                                List.of(Boolean.TRUE, new CodeableConcept(), new Range()),
                                EXPRESSION_DENOMINATOR,
                                List.of(new Reference(), new Coding()),
                                EXPRESSION_NUMERATOR,
                                List.of(new Enumeration<>(new CompartmentCodeEnumFactory()), new Code())))),
                new ValidateStratifierBasisTypeHappyPathParams(
                        buildGroupDef(
                                Basis.ENCOUNTER,
                                buildPopulationDefs(Basis.ENCOUNTER, INITIALPOPULATION, DENOMINATOR, NUMERATOR),
                                buildStratifierDefs(
                                        MeasureStratifierType.VALUE,
                                        EXPRESSION_INITIALPOPULATION,
                                        EXPRESSION_DENOMINATOR,
                                        EXPRESSION_NUMERATOR)),
                        buildEvaluationResult(Map.of(
                                EXPRESSION_INITIALPOPULATION,
                                List.of(Boolean.TRUE, Boolean.TRUE, Boolean.TRUE),
                                EXPRESSION_DENOMINATOR,
                                List.of(Boolean.TRUE, Boolean.TRUE, Boolean.TRUE),
                                EXPRESSION_NUMERATOR,
                                List.of(Boolean.TRUE, Boolean.TRUE, Boolean.TRUE)))));
    }

    @ParameterizedTest(name = "{index} => testCase={0}")
    @MethodSource("validateStratifierBasisTypeHappyPathParams")
    void validateStratifierBasisTypeHappyPath(ValidateStratifierBasisTypeHappyPathParams testCase) {
        testSubject.validateStratifiers(MEASURE_DEF, testCase.groupDef(), testCase.evaluationResult());
    }

    @Test
    void mismatchBooleanBasisSingleEncounterResult() {
        var expectedGroupDef = buildGroupDef(
                Basis.BOOLEAN,
                buildPopulationDefs(Basis.BOOLEAN, INITIALPOPULATION, DENOMINATOR, NUMERATOR),
                buildStratifierDefs(
                        MeasureStratifierType.VALUE,
                        EXPRESSION_INITIALPOPULATION,
                        EXPRESSION_DENOMINATOR,
                        EXPRESSION_NUMERATOR));

        var expectedEvaluationResult = buildEvaluationResult(Map.of(
                EXPRESSION_INITIALPOPULATION,
                ENCOUNTER,
                EXPRESSION_DENOMINATOR,
                ENCOUNTER,
                EXPRESSION_NUMERATOR,
                ENCOUNTER));

        var expectedExceptionMessage =
                "stratifier expression criteria results for expression: [InitialPopulation] must fall within accepted types for population-basis: [boolean] for Measure: [fakeMeasureUrl] due to mismatch between total eval result classes: [Encounter] and matching result classes: []";

        validateStratifierBasisTypeErrorPath(expectedGroupDef, expectedEvaluationResult, expectedExceptionMessage);
    }

    @Test
    void mismatchBooleanBasisMultipleEncounterResults() {

        var expectedGroupDef = buildGroupDef(
                Basis.BOOLEAN,
                buildPopulationDefs(Basis.BOOLEAN, INITIALPOPULATION, DENOMINATOR, NUMERATOR),
                buildStratifierDefs(
                        MeasureStratifierType.VALUE,
                        EXPRESSION_INITIALPOPULATION,
                        EXPRESSION_DENOMINATOR,
                        EXPRESSION_NUMERATOR));

        var expectedEvaluationResult = buildEvaluationResult(Map.of(
                EXPRESSION_INITIALPOPULATION,
                List.of(ENCOUNTER, ENCOUNTER, ENCOUNTER),
                EXPRESSION_DENOMINATOR,
                List.of(ENCOUNTER, ENCOUNTER, ENCOUNTER),
                EXPRESSION_NUMERATOR,
                List.of(ENCOUNTER, ENCOUNTER, ENCOUNTER)));

        var expectedExceptionMessage =
                "stratifier expression criteria results for expression: [InitialPopulation] must fall within accepted types for population-basis: [boolean] for Measure: [fakeMeasureUrl] due to mismatch between total eval result classes: [Encounter, Encounter, Encounter] and matching result classes: []";

        validateStratifierBasisTypeErrorPath(expectedGroupDef, expectedEvaluationResult, expectedExceptionMessage);
    }

    @Test
    void mismatchBooleanBasisMixedMultipleBooleanAndEncounterResults() {
        var expectedGroupDef = buildGroupDef(
                Basis.BOOLEAN,
                buildPopulationDefs(Basis.BOOLEAN, INITIALPOPULATION, DENOMINATOR, NUMERATOR),
                buildStratifierDefs(
                        MeasureStratifierType.VALUE,
                        EXPRESSION_INITIALPOPULATION,
                        EXPRESSION_DENOMINATOR,
                        EXPRESSION_NUMERATOR));

        var expectedEvaluationResult = buildEvaluationResult(Map.of(
                EXPRESSION_INITIALPOPULATION,
                List.of(Boolean.TRUE, Boolean.TRUE, Boolean.TRUE),
                EXPRESSION_DENOMINATOR,
                List.of(Boolean.TRUE, Boolean.TRUE, Boolean.TRUE),
                EXPRESSION_NUMERATOR,
                List.of(Boolean.TRUE, Boolean.TRUE, ENCOUNTER)));

        var expectedExceptionMessage =
                "stratifier expression criteria results for expression: [Numerator] must fall within accepted types for population-basis: [boolean] for Measure: [fakeMeasureUrl] due to mismatch between total eval result classes: [Boolean, Boolean, Encounter] and matching result classes: [Boolean, Boolean]";

        validateStratifierBasisTypeErrorPath(expectedGroupDef, expectedEvaluationResult, expectedExceptionMessage);
    }

    private void validateStratifierBasisTypeErrorPath(
            GroupDef groupDef, EvaluationResult evaluationResult, String expectedExceptionMessage) {
        try {
            testSubject.validateStratifiers(MEASURE_DEF, groupDef, evaluationResult);
            fail("Expected this test to fail");
        } catch (InvalidRequestException exception) {
            assertEquals(expectedExceptionMessage, exception.getMessage());
        }
    }

    @Nonnull
    private static GroupDef buildGroupDef(
            Basis basis, List<PopulationDef> populationDefs, List<StratifierDef> stratifierDefs) {
        return new GroupDef(
                null, null, stratifierDefs, populationDefs, MeasureScoring.PROPORTION, false, null, basis.codeDef);
    }

    @Nonnull
    private static List<PopulationDef> buildPopulationDefs(
            Basis basis, MeasurePopulationType... measurePopulationTypes) {
        return Arrays.stream(measurePopulationTypes)
                .map(type -> buildPopulationDef(basis, type))
                .toList();
    }

    @Nonnull
    private static PopulationDef buildPopulationDef(Basis basis, MeasurePopulationType measurePopulationType) {
        return new PopulationDef(
                measurePopulationType.toCode(),
                null,
                measurePopulationType,
                resolveExpressionFor(measurePopulationType),
                basis.codeDef,
                null,
                null,
                null);
    }

    private static String resolveExpressionFor(MeasurePopulationType theMeasurePopulationType) {
        return POPULATION_TYPE_TO_EXPRESSION.get(theMeasurePopulationType);
    }

    @Nonnull
    private static List<StratifierDef> buildStratifierDefs(
            MeasureStratifierType stratifierType, String... populations) {
        return Arrays.stream(populations)
                .map(population -> buildStratifierDef(stratifierType, population))
                .toList();
    }

    @Nonnull
    private static StratifierDef buildStratifierDef(MeasureStratifierType stratifierType, String expression) {
        final List<StratifierComponentDef> stratifierComponentDefs;
        if (stratifierType == MeasureStratifierType.VALUE) {
            stratifierComponentDefs = List.of(new StratifierComponentDef(null, null, expression));
        } else {
            stratifierComponentDefs = List.of();
        }
        return new StratifierDef(null, null, expression, stratifierType, stratifierComponentDefs);
    }

    @Nonnull
    private static EvaluationResult buildEvaluationResult(Map<String, Object> expressionResultMap) {
        final EvaluationResult evaluationResult = new EvaluationResult();
        expressionResultMap.forEach((key, value) ->
                evaluationResult.set(new EvaluationExpressionRef(key), new ExpressionResult(value, Set.of())));
        return evaluationResult;
    }
}
