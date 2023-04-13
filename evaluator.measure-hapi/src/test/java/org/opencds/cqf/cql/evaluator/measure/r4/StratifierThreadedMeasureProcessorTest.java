package org.opencds.cqf.cql.evaluator.measure.r4;

import org.opencds.cqf.cql.evaluator.measure.r4.Measure.Given;
import org.testng.annotations.Test;


@Test(enabled = false,
    description = "The R4ReportAggregator is order dependent, meaning it doesn't work correctly in multi-threaded environments")
public class StratifierThreadedMeasureProcessorTest {

  protected static Given given =
      Measure.given()
          .repositoryFor("r4/PrimaryCariesPreventionasOfferedbyPCPsincludingDentistsFHIR");

  @Test(enabled = false)
  public void exm74_subject_list_threaded() {
    given.when()
        .measureId("PrimaryCariesPreventionasOfferedbyPCPsincludingDentistsFHIR")
        .periodStart("2019-01-01")
        .periodEnd("2020-01-01")
        .reportType("subject-list")
        .evaluate() // TODO: Enable threaded evaluation...
        .then()
        .firstGroup()
        .hasScore("0.5").hasStratifierCount(3)
        .population("numerator").hasCount(6).up()
        .population("denominator").hasCount(12).up()
        .population("denominator-exclusion").hasCount(3).up()
        .population("initial-population").hasCount(15).up()
        .firstStratifier(); // TODO: Stratifier validation

    // Stratifiers should cover the total population, so we expect
    // initial-population true (5) + initial-population false (10) = initial-population total
    // (15)
    // validateStratifier(mrgc.getStratifierFirstRep(), "true", "initial-population", 5);
    // validateStratifier(mrgc.getStratifierFirstRep(), "true", "numerator", 2);
    // validateStratumScore(mrgc.getStratifierFirstRep(), "true", new BigDecimal("0.5"));

    // validateStratifier(mrgc.getStratifierFirstRep(), "false", "initial-population", 10);
    // validateStratifier(mrgc.getStratifierFirstRep(), "false", "numerator", 4);
    // validateStratumScore(mrgc.getStratifierFirstRep(), "false", new BigDecimal("0.5"));
  }

}
