package org.opencds.cqf.fhir.cr.measure.dstu3;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.hl7.fhir.dstu3.model.MeasureReport;
import org.junit.jupiter.api.Test;

class MeasureScorerTest {

    @Test
    void scorerThrowsIfNoScoringSupplied() {
        var measureUrl = "http://some.measure.with.no.scoring";
        var mr = new MeasureReport();
        mr.addGroup();
        Dstu3MeasureReportScorer scorer = new Dstu3MeasureReportScorer();
        try {
            scorer.score(measureUrl, null, mr);
            fail();
        } catch (IllegalArgumentException e) {
            //            MeasureDef is required in order to score a Measure for Measure:
            // http://some.measure.with.no.scoring
            assertTrue(
                    e.getMessage()
                            .contains(
                                    "MeasureDef is required in order to score a Measure for Measure: http://some.measure.with.no.scoring"));
        }
    }
}
