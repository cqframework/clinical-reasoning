package org.opencds.cqf.fhir.cr.measure.r4;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opencds.cqf.cql.engine.execution.EvaluationResult;
import org.opencds.cqf.fhir.cr.measure.common.GroupDef;
import org.opencds.cqf.fhir.cr.measure.common.MeasureDef;

class R4PopulationBasisValidatorTest {

    private static final String FAKE_MEASURE_URL = "fakeMeasureUrl";
    private static final MeasureDef MEASURE_DEF = new MeasureDef(null, FAKE_MEASURE_URL, null, null, null);

    private final R4PopulationBasisValidator testSubject = new R4PopulationBasisValidator();

    private static Stream<Arguments> validateGroupBasisTypeParams() {
        return Stream.of(
            Arguments.of(
                new GroupDef(null, null, null, null, null, false, null, null),
                new EvaluationResult())
        );
    }

    @ParameterizedTest
    @MethodSource("validateGroupBasisTypeParams")
    void validateGroupBasisType(GroupDef groupDef, EvaluationResult evaluationResult) {
        testSubject.validateGroupPopulations(MEASURE_DEF, groupDef, evaluationResult);

    }

    private static Stream<Arguments> validateStratifierBasisTypeParams() {
        return Stream.of(
            Arguments.of()
        );
    }

    @ParameterizedTest
    @MethodSource("validateStratifierBasisTypeParams")
    void validateStratifierBasisType(GroupDef groupDef, EvaluationResult evaluationResult) {
        testSubject.validateStratifiers(MEASURE_DEF, groupDef, evaluationResult);
    }
}
