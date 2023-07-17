package org.opencds.cqf.cql.evaluator.measure.r4;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.text.DecimalFormat;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import org.opencds.cqf.cql.evaluator.measure.r4.Measure.When;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.testng.annotations.Test;

@State(Scope.Benchmark)
public class MeasureBenchmarkIT {

  private static DecimalFormat df = new DecimalFormat("0.000");
  private static final double REFERENCE_SCORE = 15; // ops/second
  private static final double SCORE_DEVIATION = .5; // +/- 50% ops/second allowed

  private When when;

  @Setup(Level.Iteration)
  public void setupIteration() throws Exception {
    this.when = Measure.given().repositoryFor("CaseRepresentation101")
        .when()
        .measureId("GlycemicControlHypoglycemicInitialPopulation")
        .subject("Patient/eNeMVHWfNoTsMTbrwWQQ30A3")
        .periodStart("2022-01-01")
        .periodEnd("2022-06-29")
        .reportType("subject")
        .evaluate();
  }

  @Benchmark
  @Fork(warmups = 1, value = 1)
  @Measurement(batchSize = 10, iterations = 5, timeUnit = TimeUnit.SECONDS)
  @OutputTimeUnit(TimeUnit.SECONDS)
  public void test(Blackhole bh) throws Exception {
    // The Blackhole ensures that the compiler doesn't optimize
    // away this call, which does nothing with the result of the evaluation
    bh.consume(this.when.then().report());
  }


  @Test
  public void benchmark() throws Exception {
    Options opt = new OptionsBuilder()
        .include(MeasureBenchmarkIT.class.getSimpleName())
        .build();
    Collection<RunResult> runResults = new Runner(opt).run();
    assertFalse(runResults.isEmpty());
    for (RunResult runResult : runResults) {
      assertDeviationWithin(runResult, REFERENCE_SCORE, SCORE_DEVIATION);
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
