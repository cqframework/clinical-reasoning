package org.opencds.cqf.fhir.cr.measure.r4;

import org.cqframework.cql.cql2elm.CqlCompilerOptions;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.cql.engine.execution.CqlEngine;
import org.opencds.cqf.fhir.cql.CqlOptions;
import org.opencds.cqf.fhir.cql.EvaluationSettings;
import org.opencds.cqf.fhir.cr.measure.MeasureEvaluationOptions;
import org.opencds.cqf.fhir.cr.measure.common.MeasurePopulationType;
import org.opencds.cqf.fhir.cr.measure.r4.MultiMeasure.Given;

@SuppressWarnings({"java:S2699"})
class MultiLibEvalComplexCqlTest {
    private static final Given GIVEN_REPO =
            MultiMeasure.given().repositoryFor("MultiLibEvalComplexCql").evaluationOptions(getEvaluationOptions());

    @Test
    void singleLibraryTest_1A() {

        GIVEN_REPO
                .when()
                .measureId("Level1A")
                .reportType("subject")
                .evaluate()
                .then()
                .hasMeasureReportCount(1)
                .getFirstMeasureReport()
                .firstGroup()
                .population(MeasurePopulationType.INITIALPOPULATION)
                .hasCount(10)
                .up()
                .population(MeasurePopulationType.DENOMINATOR)
                .hasCount(7)
                .up()
                .population(MeasurePopulationType.NUMERATOR)
                .hasCount(3);
    }

    @Test
    void singleLibraryTest_1B() {

        GIVEN_REPO
                .when()
                .measureId("Level1B")
                .reportType("subject")
                .evaluate()
                .then()
                .hasMeasureReportCount(1)
                .getFirstMeasureReport()
                .firstGroup()
                .population(MeasurePopulationType.INITIALPOPULATION)
                .hasCount(10)
                .up()
                .population(MeasurePopulationType.DENOMINATOR)
                .hasCount(3)
                .up()
                .population(MeasurePopulationType.NUMERATOR)
                .hasCount(1);
    }

    @Test
    void multipleLibraryTest_1A_and_1B() {

        GIVEN_REPO
                .when()
                .measureId("Level1A")
                .measureId("Level1B")
                .reportType("subject")
                .evaluate()
                .then()
                .hasMeasureReportCount(2)
                .getFirstMeasureReport()
                .firstGroup()
                .population(MeasurePopulationType.INITIALPOPULATION)
                .hasCount(10)
                .up()
                .population(MeasurePopulationType.DENOMINATOR)
                .hasCount(7)
                .up()
                .population(MeasurePopulationType.NUMERATOR)
                .hasCount(3)
                .up()
                .up()
                .up()
                .getSecondMeasureReport()
                .firstGroup()
                .population(MeasurePopulationType.INITIALPOPULATION)
                .hasCount(10)
                .up()
                .population(MeasurePopulationType.DENOMINATOR)
                .hasCount(3)
                .up()
                .population(MeasurePopulationType.NUMERATOR)
                .hasCount(1);
    }

    private static MeasureEvaluationOptions getEvaluationOptions() {
        return MeasureEvaluationOptions.defaultOptions()
                .setApplyScoringSetMembership(false)
                .setEvaluationSettings(getEvaluationSettings());
    }

    private static EvaluationSettings getEvaluationSettings() {
        return EvaluationSettings.getDefault().withCqlOptions(getCqlOptions());
    }

    private static CqlOptions getCqlOptions() {
        final CqlOptions cqlOptions = CqlOptions.defaultOptions();
        cqlOptions.getCqlEngineOptions().getOptions().add(CqlEngine.Options.EnableHedisCompatibilityMode);
        cqlOptions.setCqlCompilerOptions(getCqlCompilerOptions());
        return cqlOptions;
    }

    private static CqlCompilerOptions getCqlCompilerOptions() {
        final CqlCompilerOptions cqlCompilerOptions = new CqlCompilerOptions();
        cqlCompilerOptions.setEnableCqlOnly(true);
        return cqlCompilerOptions;
    }
}
