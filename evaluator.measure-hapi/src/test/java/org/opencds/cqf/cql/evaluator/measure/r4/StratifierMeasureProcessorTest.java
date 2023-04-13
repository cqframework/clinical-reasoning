package org.opencds.cqf.cql.evaluator.measure.r4;

import org.opencds.cqf.cql.evaluator.measure.r4.Measure.Given;
import org.testng.annotations.Test;

public class StratifierMeasureProcessorTest {


  protected static Given given =
      Measure.given()
          .repositoryFor("r4/PrimaryCariesPreventionasOfferedbyPCPsincludingDentistsFHIR");

  @Test
  public void exm74_singlePatient_denominator() {

    given.when()
        .measureId("PrimaryCariesPreventionasOfferedbyPCPsincludingDentistsFHIR")
        .periodStart("2019-01-01")
        .periodEnd("2020-01-01")
        .subject("Patient/denom-EXM74-strat1-case1")
        .reportType("subject")
        .evaluate()
        .then()
        .firstGroup()
        .hasScore("0.0").hasStratifierCount(3)
        .population("numerator").hasCount(0).up()
        .population("denominator").hasCount(1).up()
        .population("initial-population").hasCount(1).up()
        .firstStratifier(); // TODO: Stratifier validation


    // validateStratifier(mrgc.getStratifierFirstRep(), "true", "initial-population", 1);
    // validateStratumScore(mrgc.getStratifierFirstRep(), "true", new BigDecimal("0.0"));
  }

  @Test
  public void exm74_singlePatient_numerator() {
    given.when()
        .measureId("PrimaryCariesPreventionasOfferedbyPCPsincludingDentistsFHIR")
        .periodStart("2019-01-01")
        .periodEnd("2020-01-01")
        .subject("Patient/numer-EXM74-strat1-case7")
        .reportType("subject")
        .evaluate()
        .then()
        .firstGroup()
        .hasScore("1.0").hasStratifierCount(3)
        .population("numerator").hasCount(1).up()
        .population("denominator").hasCount(1).up()
        .population("initial-population").hasCount(1).up()
        .firstStratifier(); // TODO: Stratifier validation

    // validateStratifier(mrgc.getStratifierFirstRep(), "true", "initial-population", 1);
    // validateStratifier(mrgc.getStratifierFirstRep(), "true", "numerator", 1);
    // validateStratumScore(mrgc.getStratifierFirstRep(), "true", new BigDecimal("1.0"));
  }

  @Test
  public void exm74_subject_list() {

    given.when()
        .measureId("PrimaryCariesPreventionasOfferedbyPCPsincludingDentistsFHIR")
        .periodStart("2019-01-01")
        .periodEnd("2020-01-01")
        .reportType("subject-list")
        .evaluate()
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

  @Test
  public void exm74_population() {
    given.when()
        .measureId("PrimaryCariesPreventionasOfferedbyPCPsincludingDentistsFHIR")
        .periodStart("2019-01-01")
        .periodEnd("2020-01-01")
        .reportType("population")
        .evaluate()
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
