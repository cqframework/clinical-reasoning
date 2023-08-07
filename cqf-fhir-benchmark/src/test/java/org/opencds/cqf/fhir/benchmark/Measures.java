package org.opencds.cqf.fhir.benchmark;

import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import org.opencds.cqf.cql.evaluator.measure.MeasureEvaluationOptions;
import org.opencds.cqf.cql.evaluator.measure.r4.Measure;
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
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

@State(Scope.Benchmark)
public class Measures {
  private When when;

  @Setup(Level.Iteration)
  public void setupIteration() throws Exception {
    var evaluationOptions = MeasureEvaluationOptions.defaultOptions();
    evaluationOptions.getEvaluationSettings().setLibraryCache(new HashMap<>());
    this.when = Measure.given().repositoryFor("CaseRepresentation101")
        .evaluationOptions(evaluationOptions)
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
  @Measurement(iterations = 2, timeUnit = TimeUnit.SECONDS)
  @OutputTimeUnit(TimeUnit.SECONDS)
  public void test(Blackhole bh) throws Exception {
    // The Blackhole ensures that the compiler doesn't optimize
    // away this call, which does nothing with the result of the evaluation
    bh.consume(this.when.then().report());
  }

  @SuppressWarnings("unused")
  public static void main(String[] args) throws RunnerException {
    Options opt = new OptionsBuilder()
        .include(Measures.class.getSimpleName())
        .build();
    Collection<RunResult> runResults = new Runner(opt).run();
  }
}
