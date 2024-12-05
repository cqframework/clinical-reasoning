package org.opencds.cqf.fhir.cr.measure.r4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Procedure;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opencds.cqf.cql.engine.execution.EvaluationResult;
import org.opencds.cqf.cql.engine.execution.ExpressionResult;
import org.opencds.cqf.fhir.cr.measure.common.CodeDef;
import org.opencds.cqf.fhir.cr.measure.common.GroupDef;
import org.opencds.cqf.fhir.cr.measure.common.MeasureDef;
import org.opencds.cqf.fhir.cr.measure.common.MeasurePopulationType;
import org.opencds.cqf.fhir.cr.measure.common.MeasureScoring;
import org.opencds.cqf.fhir.cr.measure.common.PopulationDef;
import org.opencds.cqf.fhir.cr.measure.common.StratifierDef;

class R4PopulationBasisValidatorTest {

    private static final String FAKE_MEASURE_URL = "fakeMeasureUrl";
    private static final MeasureDef MEASURE_DEF = new MeasureDef(null, FAKE_MEASURE_URL, null, null, null);
    private static final String INITIAL_POPULATION = "InitialPopulation";

    private static final PopulationDef POPULATION_INITIAL_POPULATION =
            buildPopulationDef(MeasurePopulationType.INITIALPOPULATION, INITIAL_POPULATION);

    private static final CodeDef BASIS_BOOLEAN = new CodeDef("", "boolean");
    private static final CodeDef BASIS_ENCOUNTER = new CodeDef("", "Encounter");
    private static final CodeDef BASIS_PROCEDURE = new CodeDef("", "Procedure");

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

    private static Stream<Arguments> validateGroupBasisTypeHappyPathParams() {
        return Stream.of(
                Arguments.of(
                        buildGroupDef(Basis.BOOLEAN, List.of(POPULATION_INITIAL_POPULATION)),
                        buildEvaluationResult(true)),
                Arguments.of(
                        buildGroupDef(Basis.ENCOUNTER, List.of(POPULATION_INITIAL_POPULATION)),
                        buildEvaluationResult(List.of(new Encounter()))),
                Arguments.of(
                        buildGroupDef(Basis.PROCEDURE, List.of(POPULATION_INITIAL_POPULATION)),
                        buildEvaluationResult(List.of(new Procedure()))));
    }

    @ParameterizedTest
    @MethodSource("validateGroupBasisTypeHappyPathParams")
    void validateGroupBasisTypeHappyPath(GroupDef groupDef, EvaluationResult evaluationResult) {
        testSubject.validateGroupPopulations(MEASURE_DEF, groupDef, evaluationResult);
    }

    private static Stream<Arguments> validateGroupBasisTypeErrorPathParams() {
        return Stream.of(
                Arguments.of(
                        buildGroupDef(Basis.BOOLEAN, List.of(POPULATION_INITIAL_POPULATION)),
                        buildEvaluationResult(new Encounter()),
                        "group expression criteria results for expression: [InitialPopulation] and scoring: [PROPORTION] must match the same type: [org.hl7.fhir.r4.model.Encounter] as population basis: [boolean] for Measure: fakeMeasureUrl"),
                Arguments.of(
                        buildGroupDef(Basis.ENCOUNTER, List.of(POPULATION_INITIAL_POPULATION)),
                        buildEvaluationResult(Boolean.TRUE),
                        "group expression criteria results for expression: [InitialPopulation] and scoring: [PROPORTION] must match the same type: [java.lang.Boolean] as population basis: [Encounter] for Measure: fakeMeasureUrl"),
                Arguments.of(
                        buildGroupDef(Basis.PROCEDURE, List.of(POPULATION_INITIAL_POPULATION)),
                        buildEvaluationResult(new Encounter()),
                        "group expression criteria results for expression: [InitialPopulation] and scoring: [PROPORTION] must match the same type: [org.hl7.fhir.r4.model.Encounter] as population basis: [Procedure] for Measure: fakeMeasureUrl"));
    }

    @ParameterizedTest
    @MethodSource("validateGroupBasisTypeErrorPathParams")
    void validateGroupBasisTypeErrorPath(
            GroupDef groupDef, EvaluationResult evaluationResult, String expectedExceptionMessage) {
        try {
            testSubject.validateGroupPopulations(MEASURE_DEF, groupDef, evaluationResult);
            fail("Expected this test to fail");
        } catch (InvalidRequestException exception) {
            assertEquals(expectedExceptionMessage, exception.getMessage());
        }
    }

    private static Stream<Arguments> validateStratifierBasisTypeHappyPathParams() {
        return Stream.of(Arguments.of(
                buildGroupDef(Basis.BOOLEAN, List.of(POPULATION_INITIAL_POPULATION)), buildEvaluationResult(true)));
    }

    @ParameterizedTest
    @MethodSource("validateStratifierBasisTypeHappyPathParams")
    void validateStratifierBasisTypeHappyPath(GroupDef groupDef, EvaluationResult evaluationResult) {
        testSubject.validateStratifiers(MEASURE_DEF, groupDef, evaluationResult);
    }

    private static Stream<Arguments> validateStratifierBasisTypeErrorPathParams() {
        return Stream.of(Arguments.of(
                buildGroupDef(Basis.BOOLEAN, List.of(POPULATION_INITIAL_POPULATION)),
                buildEvaluationResult(new Encounter())));
    }

    @ParameterizedTest
    @MethodSource("validateStratifierBasisTypeErrorPathParams")
    void validateStratifierBasisTypeErrorPath(GroupDef groupDef, EvaluationResult evaluationResult) {
        try {
            testSubject.validateStratifiers(MEASURE_DEF, groupDef, evaluationResult);
            fail("Expected this test to fail");
        } catch (InvalidRequestException exception) {
            assertEquals(
                    "stratifier expression criteria results for expression: [InitialPopulation] must fall within accepted types [org.hl7.fhir.r4.model.Encounter] for boolean population basis: [boolean] for Measure: fakeMeasureUrl",
                    exception.getMessage());
        }
    }

    @Nonnull
    private static GroupDef buildGroupDef(Basis basis, List<PopulationDef> populationDefs) {
        return new GroupDef(
                null,
                null,
                List.of(buildStratifierDef(INITIAL_POPULATION)),
                populationDefs,
                MeasureScoring.PROPORTION,
                false,
                null,
                basis.codeDef);
    }

    @Nonnull
    private static PopulationDef buildPopulationDef(MeasurePopulationType measurePopulationType, String expression) {
        return new PopulationDef(measurePopulationType.toCode(), null, measurePopulationType, expression);
    }

    @Nonnull
    private static StratifierDef buildStratifierDef(String expression) {
        return new StratifierDef(null, null, expression, List.of());
    }

    @Nonnull
    private static EvaluationResult buildEvaluationResult(Object expressionResult) {
        final EvaluationResult evaluationResult = new EvaluationResult();
        evaluationResult.expressionResults.put(INITIAL_POPULATION, new ExpressionResult(expressionResult, Set.of()));
        return evaluationResult;
    }
}
