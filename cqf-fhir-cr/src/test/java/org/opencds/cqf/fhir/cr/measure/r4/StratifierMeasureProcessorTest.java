package org.opencds.cqf.fhir.cr.measure.r4;

import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.Given;

class StratifierMeasureProcessorTest {

    protected static Given given =
            Measure.given().repositoryFor("PrimaryCariesPreventionasOfferedbyPCPsincludingDentistsFHIR");

    @Test
    void exm74_singlePatient_denominator() {

        given.when()
                .measureId("PrimaryCariesPreventionasOfferedbyPCPsincludingDentistsFHIR")
                .periodStart(LocalDate.of(2019, Month.JANUARY, 1).atStartOfDay(ZoneId.systemDefault()))
                .periodEnd(LocalDate.of(2020, Month.JANUARY, 1).atStartOfDay(ZoneId.systemDefault()))
                .subject("Patient/denom-EXM74-strat1-case1")
                .reportType("subject")
                .evaluate()
                .then()
                .firstGroup()
                .hasScore("0.0")
                .hasStratifierCount(3)
                .population("numerator")
                .hasCount(0)
                .up()
                .population("denominator")
                .hasCount(1)
                .up()
                .population("initial-population")
                .hasCount(1)
                .up()
                .firstStratifier()
                .firstStratum()
                .hasScore("0.0")
                .population("initial-population")
                .hasCount(1)
                .up()
                .population("denominator")
                .hasCount(1)
                .up()
                .population("numerator")
                .hasCount(0);
    }

    @Test
    void exm74_singlePatient_numerator() {
        given.when()
                .measureId("PrimaryCariesPreventionasOfferedbyPCPsincludingDentistsFHIR")
                .periodStart(LocalDate.of(2019, Month.JANUARY, 1).atStartOfDay(ZoneId.systemDefault()))
                .periodEnd(LocalDate.of(2020, Month.JANUARY, 1).atStartOfDay(ZoneId.systemDefault()))
                .subject("Patient/numer-EXM74-strat1-case7")
                .reportType("subject")
                .evaluate()
                .then()
                .firstGroup()
                .hasScore("1.0")
                .hasStratifierCount(3)
                .population("numerator")
                .hasCount(1)
                .up()
                .population("denominator")
                .hasCount(1)
                .up()
                .population("initial-population")
                .hasCount(1)
                .up()
                .firstStratifier()
                .firstStratum()
                .hasScore("1.0")
                .population("initial-population")
                .hasCount(1)
                .up()
                .population("numerator")
                .hasCount(1);
    }

    @Test
    void exm74_subject_list() {

        given.when()
                .measureId("PrimaryCariesPreventionasOfferedbyPCPsincludingDentistsFHIR")
                .periodStart(LocalDate.of(2019, Month.JANUARY, 1).atStartOfDay(ZoneId.systemDefault()))
                .periodEnd(LocalDate.of(2020, Month.JANUARY, 1).atStartOfDay(ZoneId.systemDefault()))
                .reportType("subject-list")
                .evaluate()
                .then()
                .firstGroup()
                .hasScore("0.5")
                .hasStratifierCount(3)
                .population("numerator")
                .hasCount(6)
                .up()
                .population("denominator")
                .hasCount(12)
                .up()
                .population("denominator-exclusion")
                .hasCount(3)
                .up()
                .population("initial-population")
                .hasCount(15)
                .up()
                .firstStratifier()
                .stratum("true")
                .hasScore("0.5")
                .population("initial-population")
                .hasCount(5)
                .up()
                .population("numerator")
                .hasCount(2)
                .up()
                .population("denominator")
                .hasCount(4)
                .up()
                .up()
                .stratum("false")
                .hasScore("0.5")
                .population("initial-population")
                .hasCount(10)
                .up()
                .population("numerator")
                .hasCount(4)
                .up()
                .population("denominator")
                .hasCount(8);
    }

    @Test
    void exm74_population() {
        given.when()
                .measureId("PrimaryCariesPreventionasOfferedbyPCPsincludingDentistsFHIR")
                .periodStart(LocalDate.of(2019, Month.JANUARY, 1).atStartOfDay(ZoneId.systemDefault()))
                .periodEnd(LocalDate.of(2020, Month.JANUARY, 1).atStartOfDay(ZoneId.systemDefault()))
                .reportType("population")
                .evaluate()
                .then()
                .firstGroup()
                .hasScore("0.5")
                .hasStratifierCount(3)
                .population("numerator")
                .hasCount(6)
                .up()
                .population("denominator")
                .hasCount(12)
                .up()
                .population("denominator-exclusion")
                .hasCount(3)
                .up()
                .population("initial-population")
                .hasCount(15)
                .up()
                .firstStratifier()
                .stratum("true")
                .hasScore("0.5")
                .population("initial-population")
                .hasCount(5)
                .up()
                .population("numerator")
                .hasCount(2)
                .up()
                .population("denominator")
                .hasCount(4)
                .up()
                .up()
                .stratum("false")
                .hasScore("0.5")
                .population("initial-population")
                .hasCount(10)
                .up()
                .population("numerator")
                .hasCount(4)
                .up()
                .population("denominator")
                .hasCount(8);

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
