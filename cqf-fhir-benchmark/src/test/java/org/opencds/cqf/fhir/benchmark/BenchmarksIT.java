package org.opencds.cqf.fhir.benchmark;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.text.DecimalFormat;
import java.util.Collection;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

public class BenchmarksIT {

  private static final DecimalFormat df = new DecimalFormat("0.000");

  // ops/second
  private static final Map<String, Integer> REFERENCE_SCORES = Map.of(
      "org.opencds.cqf.fhir.benchmark.PlanDefinitions.test", 300,
      "org.opencds.cqf.fhir.benchmark.Measures.test", 190,
      "org.opencds.cqf.fhir.benchmark.Questionnaires.test", 530);

  private static final double SCORE_DEVIATION = .5; // +/- 50% ops/second allowed

  @Test
  public void benchmark() throws Exception {
    Options opt = new OptionsBuilder()
        .include(Questionnaires.class.getSimpleName())
        .include(Measures.class.getSimpleName())
        .include(PlanDefinitions.class.getSimpleName())
        .build();
    Collection<RunResult> runResults = new Runner(opt).run();
    assertFalse(runResults.isEmpty());
    for (RunResult runResult : runResults) {
      var referenceScore = REFERENCE_SCORES.get(runResult.getPrimaryResult().getLabel());
      assertNotNull(referenceScore);
      assertDeviationWithin(runResult, referenceScore, SCORE_DEVIATION);
    }
  }

  private static void assertDeviationWithin(RunResult result, double referenceScore,
      double maxDeviation) {
    double score = result.getPrimaryResult().getScore();
    double deviation = Math.abs(score / referenceScore - 1);
    String deviationString = df.format(deviation * 100) + "%";
    String maxDeviationString = df.format(maxDeviation * 100) + "%";
    String errorMessage =
        "Deviation " + deviationString + " exceeds maximum allowed deviation " + maxDeviationString;
    assertTrue(deviation < maxDeviation, errorMessage);
  }
}
