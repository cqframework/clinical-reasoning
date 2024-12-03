package org.opencds.cqf.fhir.cr.measure.r4;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opencds.cqf.fhir.cr.measure.r4.R4PopulationBasisValidator.PopulationBasis;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class R4PopulationBasisValidatorTest {

    private static Stream<Arguments> validateGroupBasisTypeParams() {
        return null;
    }

//    @ParameterizedTest
//    @MethodSource("validateGroupBasisTypeParams")
//    void validateGroupBasisType(PopulationBasis measureBasis, PopulationBasis groupBasis, PopulationBasis criteriaBasis) {
//
//    }
//
//    private static Stream<Arguments> validateStratifierBasisTypeParams() {
//        return null;
//    }
//
//    @ParameterizedTest
//    @MethodSource("validateStratifierBasisTypeParams")
//    void validateStratifierBasisType(PopulationBasis measureBasis, PopulationBasis stratifierBasis, PopulationBasis criteriaBasis) {
//
//    }
}