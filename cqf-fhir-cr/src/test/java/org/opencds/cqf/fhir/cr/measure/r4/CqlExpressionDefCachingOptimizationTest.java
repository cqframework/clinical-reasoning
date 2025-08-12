package org.opencds.cqf.fhir.cr.measure.r4;

import org.cqframework.cql.cql2elm.CqlCompilerOptions;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cql.CqlOptions;
import org.opencds.cqf.fhir.cql.EvaluationSettings;
import org.opencds.cqf.fhir.cr.measure.MeasureEvaluationOptions;
import org.opencds.cqf.fhir.cr.measure.r4.MultiMeasure.Given;

public class CqlExpressionDefCachingOptimizationTest {
    private static final Given GIVEN_REPO =
            MultiMeasure.given().repositoryFor("CqlOptimize").evaluationOptions(getEvaluationOptions());

    @Test
    void MultiMeasure_AllSubjects_MeasureIdentifier() {
        var when = GIVEN_REPO
                .when()
                .measureId("MeasureA")
                .measureId("MeasureB")
                .reportType("population")
                .evaluate();

        when.then()
                .hasMeasureReportCount(2)
                .getFirstMeasureReport()
                .hasMeasure("http://example.com/Measure/MeasureA")
                .firstGroup()
                .population("initial-population")
                .hasCount(1)
                .up()
                .up()
                .up()
                .getSecondMeasureReport()
                .hasMeasure("http://example.com/Measure/MeasureB")
                .firstGroup()
                .population("initial-population")
                .hasCount(1);
    }

    // The point of this chain of methods is to set HEDIS options and to disable ELM library compilation.
    private static MeasureEvaluationOptions getEvaluationOptions() {
        return MeasureEvaluationOptions.defaultOptions().setEvaluationSettings(getEvaluationSettings());
    }

    private static EvaluationSettings getEvaluationSettings() {
        return EvaluationSettings.getDefault().withCqlOptions(getCqlOptions());
    }

    private static CqlOptions getCqlOptions() {
        final CqlOptions cqlOptions = CqlOptions.defaultOptions();
        cqlOptions.setCqlCompilerOptions(getCqlCompilerOptions());
        return cqlOptions;
    }

    private static CqlCompilerOptions getCqlCompilerOptions() {
        final CqlCompilerOptions cqlCompilerOptions = new CqlCompilerOptions();
        cqlCompilerOptions.setEnableCqlOnly(true);
        return cqlCompilerOptions;
    }
}
