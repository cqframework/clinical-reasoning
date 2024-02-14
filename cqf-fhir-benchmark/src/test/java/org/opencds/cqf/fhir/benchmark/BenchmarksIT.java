package org.opencds.cqf.fhir.benchmark;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.text.DecimalFormat;
import java.util.Collection;
import java.util.Map;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

public class BenchmarksIT {

    private static final DecimalFormat df = new DecimalFormat("0.000");

    // These reference scores were produced on an i9-9980HK @ 2.4 Ghz with 32 GB of ram
    // Your mileage may vary, it's best to run the benchmarks on your local machine
    // and see what your specific hardware produces and then make modifications.
    // Check your results against your own personal reference scores.
    private static final Map<String, Double> REFERENCE_SCORES = Map.of(
            "testApply", 300.0, // ops/second
            "testApplyWithGenerate", 300.0, // ops/second
            "testEvaluate", 800.0, // ops/second
            "testEvaluateAdditionalData", .35, // ops/second
            "testPopulate", 530.0, // ops/second
            "testLarge", 4_000_000.0, // ops/second
            "testSmall", 7_000_000.0); // ops/second

    // private static final Map<String, Double> BAR_REFERENCE_SCORES = Map.of(
    //         "testApply", 200.0, // ops/second
    //         "testApplyWithGenerate", 350.0, // ops/second
    //         "testEvaluate", 675.0, // ops/second
    //         "testEvaluateAdditionalData", .11, // ops/second
    //         "testPopulate", 80.0, // ops/second
    //         "testLarge", 4_900_000.0, // ops/second
    //         "testSmall", 9_000_000.0); // ops/second

    private static final double SCORE_DEVIATION = .5; // +/- 50% ops/unit allowed

    @Test
    @Disabled("Test fails during package due to test-jar not working as expected ")
    public void benchmark() throws Exception {
        Options opt = new OptionsBuilder()
                .include(Questionnaires.class.getSimpleName())
                .include(Measures.class.getSimpleName())
                .include(MeasuresAdditionalData.class.getSimpleName())
                .include(PlanDefinitions.class.getSimpleName())
                .include(PlanDefinitionsWithGenerate.class.getSimpleName())
                .include(TerminologyProviders.class.getSimpleName())
                .build();
        Collection<RunResult> runResults = new Runner(opt).run();
        assertFalse(runResults.isEmpty());
        for (RunResult runResult : runResults) {
            var referenceScore =
                    REFERENCE_SCORES.get(runResult.getPrimaryResult().getLabel());
            assertNotNull(referenceScore);
            assertDeviationWithin(runResult, referenceScore, SCORE_DEVIATION);
        }
    }

    private static void assertDeviationWithin(RunResult result, double referenceScore, double maxDeviation) {
        double score = result.getPrimaryResult().getScore();
        double deviation = Math.abs(score / referenceScore - 1);
        String deviationString = df.format(deviation * 100) + "%";
        String maxDeviationString = df.format(maxDeviation * 100) + "%";
        String errorMessage =
                "Deviation " + deviationString + " exceeds maximum allowed deviation " + maxDeviationString;
        assertTrue(deviation < maxDeviation, errorMessage);
    }
}
